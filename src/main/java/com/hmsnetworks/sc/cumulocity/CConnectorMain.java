package com.hmsnetworks.sc.cumulocity;

import com.ewon.ewonitf.DefaultEventHandler;
import com.ewon.ewonitf.EWException;
import com.ewon.ewonitf.EventHandlerThread;
import com.ewon.ewonitf.RuntimeControl;
import com.ewon.ewonitf.SysControlBlock;
import com.ewon.ewonitf.TagControl;
import com.hms_networks.americas.sc.extensions.config.exceptions.ConfigFileException;
import com.hms_networks.americas.sc.extensions.config.exceptions.ConfigFileWriteException;
import com.hms_networks.americas.sc.extensions.historicaldata.HistoricalDataQueueManager;
import com.hms_networks.americas.sc.extensions.json.JSONException;
import com.hms_networks.americas.sc.extensions.logging.Logger;
import com.hms_networks.americas.sc.extensions.retry.AutomaticRetryCodeExponential;
import com.hms_networks.americas.sc.extensions.system.application.SCAppManagement;
import com.hms_networks.americas.sc.extensions.system.http.SCHttpUtility;
import com.hms_networks.americas.sc.extensions.system.tags.SCTagUtils;
import com.hms_networks.americas.sc.extensions.system.threading.SCCountdownLatch;
import com.hms_networks.americas.sc.extensions.system.time.SCTimeUnit;
import com.hms_networks.americas.sc.extensions.system.time.SCTimeUtils;
import com.hmsnetworks.sc.cumulocity.api.CConnectorApiCertificateMgr;
import com.hmsnetworks.sc.cumulocity.api.CConnectorMqttMgr;
import com.hmsnetworks.sc.cumulocity.api.CConnectorProvisionMqttMgr;
import com.hmsnetworks.sc.cumulocity.api.CConnectorWebApiListener;
import com.hmsnetworks.sc.cumulocity.config.CConnectorConfigFile;
import com.hmsnetworks.sc.cumulocity.data.CConnectorAlarmMgr;
import com.hmsnetworks.sc.cumulocity.data.CConnectorDataMgr;
import java.util.Date;

/**
 * Main class for the Ewon Flexy Cumulocity Connector.
 *
 * @author HMS Networks, MU Americas Solution Center
 * @since 1.0.0
 */
public class CConnectorMain {

  /** A friendly name for the connector application in the format: %Title%, version %Version%. */
  private static final String CONNECTOR_FRIENDLY_NAME =
      CConnectorMain.class.getPackage().getImplementationTitle()
          + ", version "
          + CConnectorMain.class.getPackage().getImplementationVersion();

  /** The name of the tag that is used to halt the connector execution. */
  public static final String CONNECTOR_HALT_TAG_NAME = "CumulocityConnectorHalt";

  /** The name of the IO server for the tag that is used to halt the connector execution. */
  public static final String CONNECTOR_HALT_TAG_IO_SERVER_NAME = "MEM";

  /** The type of the tag (boolean) that is used to halt the connector execution. */
  public static final int CONNECTOR_HALT_TAG_TYPE = 0;

  /** The description of the tag that is used to halt the connector execution. */
  public static final String CONNECTOR_HALT_TAG_DESCRIPTION =
      "Tag which is used to halt the execution of the Cumulocity connector application.";

  /** The value of the connector halt tag that permits the application to execute. */
  public static final int CONNECTOR_CONTROL_TAG_RUN_VALUE = 0;

  /** The name of the tag that is used to enable/disable connector measurements. */
  public static final String CONNECTOR_MEASUREMENT_CONTROL_TAG_NAME = "CumulocityMeasurementEnable";

  /** The enable value for the tag that is used to enable/disable connector measurements. */
  public static final int CONNECTOR_MEASUREMENT_CONTROL_TAG_ENABLE_VALUE = 1;

  /** The type of the tag that is used to enable/disable connector measurements. */
  public static final int CONNECTOR_MEASUREMENT_CONTROL_TAG_TYPE = 0;

  /** The description of the tag that is used to enable/disable connector measurements. */
  public static final String CONNECTOR_MEASUREMENT_CONTROL_TAG_DESCRIPTION =
      "Tag which is used to enable/disable measurements in the Cumulocity connector application.";

  /** The interval (in milliseconds) at which the main loop is executed. */
  private static final int MAIN_LOOP_CYCLE_TIME_MILLIS = 1000;

  /**
   * The HTTP connection timeout time in seconds. This value affects the Ewon's global HTTP
   * timeouts.
   */
  public static final String HTTP_TIMEOUT_SECONDS_STRING = "10";

  /** Application watchdog timeout */
  public static final int APP_WATCHDOG_TIMEOUT_MIN = 5;

  /**
   * The prefix appended to the front of the Ewon Flexy's serial number to form the MQTT client ID.
   */
  private static final String MQTT_CLIENT_ID_SERIAL_PREFIX = "HMS-Flexy-";

  /** MQTT manager */
  private static CConnectorMqttMgr mqttMgr;

  /**
   * Provisioning MQTT manager. Note: this variable is always {@code null} unless provisioning is
   * currently in progress.
   */
  private static CConnectorProvisionMqttMgr provisioningMqttMgr = null;

  /** Alarm manager */
  private static CConnectorAlarmMgr alarmMgr;

  /** Connector configuration object */
  private static CConnectorConfigFile connectorConfig;

  /** Connector control tag object for accessing its value. */
  private static TagControl connectorControlTag = null;

  /** Connector measurement enable control tag object for accessing its value. */
  private static TagControl connectorMeasurementEnableControlTag = null;

  /** Boolean flag to indicate if the connector is running. */
  private static boolean isRunning = true;

  /** Boolean flag to indicate if the connector auto restart functionality is enabled. */
  private static boolean isAppAutoRestartEnabled = false;

  /**
   * Serial number of the host Ewon device, populated during the initialization of the connector.
   */
  private static String ewonSerialNumber = "";

  /** Boolean indicating if the device should be restarted after the connector shuts down. */
  private static boolean restartDeviceAfterShutdown = false;

  /** Boolean indicating if the connector application should be restarted after it shuts down. */
  private static boolean restartAppAfterShutdown = false;

  /** Count down latch used to wait for the connector to be bootstrapped. */
  private static SCCountdownLatch bootstrapConfigurationLatch = null;

  /**
   * Gets the friendly name for the connector application in the format: %Title%, version %Version%.
   *
   * @return friendly name for the connector application
   */
  public static String getConnectorFriendlyName() {
    return CONNECTOR_FRIENDLY_NAME;
  }

  /**
   * Gets the serial number of the host Ewon device which is populated during the initialization of
   * the connector.
   *
   * @return the serial number of the host Ewon device
   * @see #ewonSerialNumber
   */
  public static String getEwonSerialNumber() {
    return ewonSerialNumber;
  }

  /**
   * Gets the tag control object for the measurement enable control tag or null if the tag control
   * object cannot be created.
   *
   * @return tag control object for the measurement enable control tag
   */
  public static TagControl getConnectorMeasurementEnableControlTag() {
    return connectorMeasurementEnableControlTag;
  }

  /**
   * Gets a boolean indicating if measurements are enabled for the connector based on the value of
   * the measurement enable control tag. If the measurement enable tag is not found, then
   * measurements will be enabled by default.
   *
   * @return boolean indicating if measurements are enabled for the connector
   */
  public static boolean getConnectorMeasurementsEnabled() {
    boolean measurementsEnabled = true;
    if (connectorMeasurementEnableControlTag != null) {
      measurementsEnabled = connectorMeasurementEnableControlTag.getTagValueAsInt() == 1;
    }
    return measurementsEnabled;
  }

  /**
   * Gets the connector MQTT manager.
   *
   * @return connector MQTT manager
   */
  public static CConnectorMqttMgr getMqttMgr() {
    return mqttMgr;
  }

  /**
   * Gets the connector configuration object.
   *
   * @return connector configuration
   */
  public static CConnectorConfigFile getConnectorConfig() {
    return connectorConfig;
  }

  /**
   * Gets a boolean indicating if the connector auto restart functionality is enabled.
   *
   * @return true if the connector auto restart functionality is enabled, false otherwise
   */
  public static boolean isAppAutoRestartEnabled() {
    return isAppAutoRestartEnabled;
  }

  /**
   * Gets a boolean indicating if the connector bootstrap process has been completed.
   *
   * @return true if the connector bootstrap process has been completed, false otherwise
   */
  public static boolean isBootstrapComplete() {
    return bootstrapConfigurationLatch.getCount() == 0;
  }

  /**
   * Sets the connector bootstrap configuration and returns a boolean indicating if the operation
   * was successful.
   *
   * @param host the Cumulocity host to configure
   * @param port the Cumulocity port to configure
   * @param bootstrapTenant the Cumulocity bootstrap tenant to configure
   * @param bootstrapUsername the Cumulocity bootstrap username to configure
   * @param bootstrapPassword the Cumulocity bootstrap password to configure
   * @param resetDeviceAuthentication boolean indicating if the device authentication configuration
   *     should be reset. This is used when the bootstrap configuration is being overwritten.
   * @return true if the configuration was set successfully, false otherwise
   */
  public static boolean setBootstrapConfiguration(
      String host,
      String port,
      String bootstrapTenant,
      String bootstrapUsername,
      String bootstrapPassword,
      boolean resetDeviceAuthentication) {
    boolean success = false;
    if (connectorConfig != null) {
      try {
        // Reset device authentication configuration (if requested)
        if (resetDeviceAuthentication) {
          connectorConfig.resetDeviceAuthenticationConfiguration();
        }

        // Parse the port number to check if valid
        Integer.parseInt(port);

        // Set the bootstrap configuration
        connectorConfig.setCumulocityHost(host);
        connectorConfig.setCumulocityPort(port);
        connectorConfig.setCumulocityBootstrapTenant(bootstrapTenant);
        connectorConfig.setCumulocityBootstrapUsername(bootstrapUsername);
        connectorConfig.setCumulocityBootstrapPassword(bootstrapPassword);

        // Countdown the bootstrap latch to indicate that the bootstrap configuration has been set
        bootstrapConfigurationLatch.countDown();

        // Set the success flag to true
        success = true;
      } catch (Exception e) {
        Logger.LOG_CRITICAL("Failed to set the bootstrap configuration!");
        Logger.LOG_EXCEPTION(e);
      }
    }
    return success;
  }

  /**
   * Sets the {@link #restartAppAfterShutdown} flag to <code>true</code> to trigger a shutdown and
   * restart of the connector.
   */
  public static void shutdownAndRestartConnector() {
    Logger.LOG_CRITICAL("The connector has been requested to restart...");
    restartAppAfterShutdown = true;
    isRunning = false;

    // Release the bootstrap configuration latch in case main thread is waiting on it
    if (bootstrapConfigurationLatch != null) {
      bootstrapConfigurationLatch.countDown();
    }

    // Stop provisioning if it is in progress
    if (provisioningMqttMgr != null) {
      provisioningMqttMgr.cancelProvisioning();
    }
  }

  /**
   * Sets the {@link #restartDeviceAfterShutdown} flag to <code>true</code> to trigger a shutdown of
   * the connector and restart of the host device when requested.
   */
  public static void shutdownConnectorAndRestartDevice() {
    Logger.LOG_CRITICAL("The connector has been requested to shut down and restart the device...");
    restartDeviceAfterShutdown = true;
    isRunning = false;

    // Release the bootstrap configuration latch in case main thread is waiting on it
    if (bootstrapConfigurationLatch != null) {
      bootstrapConfigurationLatch.countDown();
    }

    // Stop provisioning if it is in progress
    if (provisioningMqttMgr != null) {
      provisioningMqttMgr.cancelProvisioning();
    }
  }

  /**
   * Sets the {@link #isRunning} flag to <code>false</code> to trigger a shutdown of the connector
   * when requested.
   */
  public static void shutdownConnector() {
    Logger.LOG_CRITICAL("The connector has been requested to shut down...");
    restartDeviceAfterShutdown = false;
    isRunning = false;

    // Release the bootstrap configuration latch in case main thread is waiting on it
    if (bootstrapConfigurationLatch != null) {
      bootstrapConfigurationLatch.countDown();
    }

    // Stop provisioning if it is in progress
    if (provisioningMqttMgr != null) {
      provisioningMqttMgr.cancelProvisioning();
    }
  }

  /**
   * Method for performing connector application initialization steps.
   *
   * @return {@code true} if initialization was successful, {@code false} otherwise
   */
  private static boolean initialize() {
    Logger.LOG_CRITICAL("Initializing " + CONNECTOR_FRIENDLY_NAME + "...");
    boolean initializeSuccess = true;

    // Load Ewon serial number
    try {
      ewonSerialNumber = new SysControlBlock(SysControlBlock.INF).getItem("SERNUM");
    } catch (Exception e) {
      Logger.LOG_CRITICAL("Failed to load device serial number!");
      Logger.LOG_EXCEPTION(e);
      initializeSuccess = false;
    }

    // Load configuration file
    initializeSuccess &= loadConfiguration();

    // Check that bootstrap is configured, otherwise cannot continue
    int bootstrapConfigurationLatchCount = 0;
    try {
      if (!connectorConfig.isProvisioned() && !connectorConfig.isBootstrapConfigured()) {
        Logger.LOG_CRITICAL(
            "The bootstrap tenant and/or credentials are not configured in the "
                + "configuration file! Please configure them using the remote API or "
                + "modify the configuration file and restart the connector.");
        bootstrapConfigurationLatchCount = 1;
      }
    } catch (JSONException e) {
      Logger.LOG_CRITICAL(
          "Unable to check for bootstrap credentials in the connector configuration file!");
      Logger.LOG_EXCEPTION(e);
      initializeSuccess = false;
    }
    bootstrapConfigurationLatch = new SCCountdownLatch(bootstrapConfigurationLatchCount);

    // Enable application auto restart
    isAppAutoRestartEnabled = SCAppManagement.enableAppAutoRestart();

    // Start thread for default event manager
    boolean autorun = false;
    EventHandlerThread eventHandler = new EventHandlerThread(autorun);
    eventHandler.runEventManagerInThread();

    // Load connector log level (default of TRACE, but should never encounter this)
    int connectorLogLevel = Logger.LOG_LEVEL_TRACE;
    try {
      connectorLogLevel = connectorConfig.getConnectorLogLevel();
    } catch (JSONException e) {
      Logger.LOG_CRITICAL("Unable to load the connector configuration file!");
      Logger.LOG_EXCEPTION(e);
      initializeSuccess = false;
    }
    boolean logLevelSetSuccess = Logger.SET_LOG_LEVEL(connectorLogLevel);
    if (!logLevelSetSuccess) {
      Logger.LOG_CRITICAL(
          "The log level specified in the connector configuration file is invalid! Please "
              + "refer to the documentation for details on available log levels!");
      initializeSuccess = false;
    }

    // Configure Ewon's HTTP timeouts
    try {
      SCHttpUtility.setHttpTimeouts(HTTP_TIMEOUT_SECONDS_STRING);
    } catch (Exception e) {
      Logger.LOG_CRITICAL("Failed to set the Ewon system's HTTP timeout value!");
      Logger.LOG_EXCEPTION(e);
      initializeSuccess = false;
    }

    // Calculate local time offset and configure queue
    try {
      SCTimeUtils.injectJvmLocalTime();

      final Date currentTime = new Date();
      final String currentLocalTime = SCTimeUtils.getIso8601LocalTimeFormat().format(currentTime);
      final String currentUtcTime = SCTimeUtils.getIso8601UtcTimeFormat().format(currentTime);
      Logger.LOG_DEBUG(
          "The local time zone is "
              + SCTimeUtils.getTimeZoneName()
              + " with an identifier of "
              + SCTimeUtils.getLocalTimeZoneDesignator()
              + ". The current local time is "
              + currentLocalTime
              + ", and the current UTC time is "
              + currentUtcTime
              + ".");
    } catch (Exception e) {
      Logger.LOG_CRITICAL("Unable to inject local time into the JVM!");
      Logger.LOG_EXCEPTION(e);
      initializeSuccess = false;
    }

    // Configure queue string history data option
    try {
      HistoricalDataQueueManager.setStringHistoryEnabled(
          connectorConfig.getQueueDataStringEnabled());
    } catch (Exception e) {
      Logger.LOG_CRITICAL(
          "Failed to configure the queue option for enabling/disabling string history data!");
      Logger.LOG_EXCEPTION(e);
      initializeSuccess = false;
    }

    // Configure queue data poll size
    try {
      HistoricalDataQueueManager.setQueueFifoTimeSpanMins(
          connectorConfig.getQueueDataPollSizeMinutes());
    } catch (Exception e) {
      Logger.LOG_CRITICAL(
          "Failed to configure the queue data poll size interval (minutes) option!");
      Logger.LOG_EXCEPTION(e);
      initializeSuccess = false;
    }

    // Configure queue max fall behind time option
    try {
      long queueDataPollMaxBehindTimeMinutes =
          connectorConfig.getQueueDataPollMaxBehindTimeMinutes();
      if (queueDataPollMaxBehindTimeMinutes
          == HistoricalDataQueueManager.DISABLED_MAX_HIST_FIFO_GET_BEHIND_MINS) {
        Logger.LOG_WARN("Queue maximum fall behind time (minutes) option is not enabled!");
      } else {
        Logger.LOG_DEBUG(
            "Setting the queue maximum fall behind time (minutes) option to"
                + queueDataPollMaxBehindTimeMinutes
                + ".");
      }
      HistoricalDataQueueManager.setQueueMaxBehindMins(queueDataPollMaxBehindTimeMinutes);
    } catch (Exception e) {
      Logger.LOG_CRITICAL(
          "Failed to configure the queue data poll maximum fall behind time (minutes) option!");
      Logger.LOG_EXCEPTION(e);
      initializeSuccess = false;
    }

    // Configure queue diagnostic tags option
    try {
      Logger.LOG_CRITICAL(
          "Configuring the queue diagnostic tags option..."
              + connectorConfig.getQueueDiagnosticTagsEnabled());
      HistoricalDataQueueManager.setEnableDiagnosticTags(
          connectorConfig.getQueueDiagnosticTagsEnabled(),
          SCTimeUnit.MILLISECONDS.toSeconds(CConnectorDataMgr.QUEUE_DATA_POLL_BEHIND_MILLIS_WARN));
    } catch (Exception e) {
      Logger.LOG_CRITICAL("Failed to configure the queue diagnostic tags enabled option!");
      Logger.LOG_EXCEPTION(e);
      initializeSuccess = false;
    }

    try {
      Logger.LOG_CRITICAL("Configuring the connector control web API...");
      DefaultEventHandler.setDefaultWebFormtListener(new CConnectorWebApiListener());
      Logger.LOG_CRITICAL("Configured the connector control web API successfully!");
    } catch (Exception e) {
      Logger.LOG_CRITICAL("Failed to configure the connector control web API!");
      Logger.LOG_EXCEPTION(e);
    }

    if (initializeSuccess) {
      Logger.LOG_CRITICAL("Finished initializing " + CONNECTOR_FRIENDLY_NAME + ".");
    } else {
      Logger.LOG_CRITICAL("Failed to initialize " + CONNECTOR_FRIENDLY_NAME + "!");
    }
    return initializeSuccess;
  }

  /**
   * Method for performing connector application start up steps.
   *
   * @return {@code true} if start up was successful, {@code false} otherwise
   */
  private static boolean startUp() {
    Logger.LOG_CRITICAL("Starting " + CONNECTOR_FRIENDLY_NAME + "...");
    boolean startUpSuccess = true;

    setUpConnectorControlTag();
    setupConnectorMeasurementEnableControlTag();

    // Run MQTT provisioning manager if provisioning required
    try {
      if (!connectorConfig.isProvisioned()) {
        if (bootstrapConfigurationLatch.getCount() > 0) {
          Logger.LOG_CRITICAL(
              "Awaiting remote API configuration of bootstrap before continuing...");
          bootstrapConfigurationLatch.await();
        }

        // Start provisioning MQTT manager (if shutdown/restart not already requested)
        if (isRunning) {
          startUpSuccess &= runProvisioningMqtt();
        }
      }
    } catch (Exception e) {
      Logger.LOG_CRITICAL("Unable to check existing provisioning status of connector.");
      Logger.LOG_EXCEPTION(e);
    }

    // Start main MQTT manager (if shutdown/restart not already requested)
    if (isRunning) {
      startUpSuccess &= setUpMqtt();
    }

    // Create tag alarm manager (if shutdown/restart not already requested)
    if (isRunning) {
      try {
        alarmMgr = new CConnectorAlarmMgr();
        Logger.LOG_CRITICAL("Created the tag alarm manager.");
      } catch (Exception e) {
        Logger.LOG_CRITICAL("Failed to create the tag alarm manager!");
        Logger.LOG_EXCEPTION(e);
      }
    }

    // Configure the application watchdog (if shutdown/restart not already requested)
    if (isRunning) {
      RuntimeControl.configureAppWatchdog(APP_WATCHDOG_TIMEOUT_MIN);
    }

    if (startUpSuccess) {
      Logger.LOG_CRITICAL("Finished starting " + CONNECTOR_FRIENDLY_NAME + ".");
    } else {
      Logger.LOG_CRITICAL("Failed to start " + CONNECTOR_FRIENDLY_NAME + "!");
    }
    return startUpSuccess;
  }

  /** Method for performing connector main loop tasks. */
  private static void runMainLoop() {
    Logger.LOG_DEBUG("Running main loop...");

    // Grab latest data and send via MQTT (if measurements enabled)
    if (getConnectorMeasurementsEnabled()) {
      try {
        CConnectorDataMgr.checkForHistoricalDataAndSend(mqttMgr);
      } catch (Exception e) {
        Logger.LOG_CRITICAL("Unable to grab latest data and send via MQTT.");
        Logger.LOG_EXCEPTION(e);
      }
    } else {
      Logger.LOG_DEBUG("Measurements disabled, skipping data processing.");
    }

    // Update connector control tag value
    if (connectorControlTag != null && isRunning) {
      isRunning = (connectorControlTag.getTagValueAsInt() == CONNECTOR_CONTROL_TAG_RUN_VALUE);
    }
  }

  /** Method for performing connector application shut down steps. */
  private static void shutDown() {
    Logger.LOG_CRITICAL("Shutting down " + CONNECTOR_FRIENDLY_NAME + "...");
    boolean shutDownClean = true;

    // Disconnect from MQTT
    if (mqttMgr != null) {
      try {
        mqttMgr.stop();
      } catch (Exception e) {
        Logger.LOG_CRITICAL("Unable to disconnect from MQTT.");
        shutDownClean = false;
      }
    }

    if (shutDownClean) {
      Logger.LOG_CRITICAL("Finished shutting down " + CONNECTOR_FRIENDLY_NAME + ".");
    } else {
      Logger.LOG_CRITICAL("Failed to shut down " + CONNECTOR_FRIENDLY_NAME + " properly!");
    }
  }

  /** Method for performing connector application clean up steps. */
  private static void cleanUp() {
    Logger.LOG_CRITICAL("Cleaning up " + CONNECTOR_FRIENDLY_NAME + "...");
    boolean cleanUpSuccess = true;

    // Unregister alarm listener from event handler
    if (alarmMgr != null) {
      try {
        DefaultEventHandler.delTagAlarmListener(alarmMgr);
      } catch (Exception e) {
        Logger.LOG_CRITICAL("Unable to unregister tag alarm manager from event handler.");
        cleanUpSuccess = false;
      }
    }

    // Disable app watchdog
    final int watchDogTimeoutDisabled = 0;
    RuntimeControl.configureAppWatchdog(watchDogTimeoutDisabled);

    if (cleanUpSuccess) {
      Logger.LOG_CRITICAL("Finished cleaning up " + CONNECTOR_FRIENDLY_NAME + ".");
    } else {
      Logger.LOG_CRITICAL("Failed to clean up " + CONNECTOR_FRIENDLY_NAME + " properly!");
    }
    Logger.LOG_CRITICAL(CONNECTOR_FRIENDLY_NAME + " has finished running.");

    // Restart connector if requested, otherwise disable automatic application restart
    if (restartAppAfterShutdown) {
      // Exit with non-zero exit code otherwise app auto restart doesn't work
      final int nonNormalExitCode = -1;
      Logger.LOG_CRITICAL("Restarting connector!");
      System.exit(nonNormalExitCode);
    } else {
      SCAppManagement.disableAppAutoRestart();
    }

    // Trigger a restart of the device (if flag is set)
    if (restartDeviceAfterShutdown) {
      Logger.LOG_CRITICAL("Restarting device!");
      RuntimeControl.reboot();
    }
  }

  /**
   * Main method for Ewon Flexy Cumulocity Connector.
   *
   * @param args project arguments
   */
  public static void main(String[] args) {
    // Initialize connector
    boolean initialized = initialize();

    // Start connector if initialization was successful
    boolean startedUp = false;
    if (initialized) {
      startedUp = startUp();
    }

    // Run connector main loop if initialization and startup were successful
    if (initialized && startedUp) {
      // Cyclically run main loop and sleep while connector is running
      while (isRunning) {
        // Service the watchdog
        RuntimeControl.refreshWatchdog();

        runMainLoop();
        try {
          Thread.sleep(MAIN_LOOP_CYCLE_TIME_MILLIS);
        } catch (InterruptedException e) {
          Logger.LOG_SERIOUS("Unable to sleep main loop for specified cycle time.");
          Logger.LOG_EXCEPTION(e);
        }
      }
    }

    // Shutdown and cleanup connector
    shutDown();
    cleanUp();
  }

  /**
   * Performs device provisioning to Cumulocity via the configured bootstrap credentials and tenant.
   *
   * @return true if provisioning was successful, false otherwise
   */
  public static boolean runProvisioningMqtt() {
    boolean mqttProvisionSuccess = true;
    try {
      Logger.LOG_DEBUG("Starting device provisioning MQTT...");
      final String mqttHost = connectorConfig.getCumulocityHost();

      Logger.LOG_DEBUG("Creating device provisioning MQTT manager and starting it...");
      final String mqttClientId = MQTT_CLIENT_ID_SERIAL_PREFIX + ewonSerialNumber;
      final String mqttPort = connectorConfig.getCumulocityPort();
      final String mqttUsername =
          connectorConfig.getCumulocityBootstrapTenant()
              + "/"
              + connectorConfig.getCumulocityBootstrapUsername();
      final String mqttPassword = connectorConfig.getCumulocityBootstrapPassword();
      final String mqttRootCaFilePath = CConnectorApiCertificateMgr.getRootCaFilePath();
      provisioningMqttMgr =
          new CConnectorProvisionMqttMgr(
              mqttClientId,
              mqttHost,
              connectorConfig.getStringUtf8Support(),
              mqttPort,
              mqttRootCaFilePath,
              mqttUsername,
              mqttPassword);
      provisioningMqttMgr.startWithExponentialRetry(
          AutomaticRetryCodeExponential.MAX_RETRIES_UNLIMITED_VALUE,
          SCTimeUnit.SECONDS.toMillis(CConnectorProvisionMqttMgr.MAX_SECONDS_BETWEEN_RETRIES));
      Logger.LOG_DEBUG("Finished creating and starting device provisioning MQTT manager.");

      provisioningMqttMgr.awaitProvisioning();
      provisioningMqttMgr.stop();
      Logger.LOG_DEBUG("Finished device provisioning MQTT.");
      provisioningMqttMgr = null;
    } catch (Exception e) {
      Logger.LOG_CRITICAL("Exception occurred, failed to run MQTT device provisioning!");
      Logger.LOG_EXCEPTION(e);
      mqttProvisionSuccess = false;
    }
    return mqttProvisionSuccess;
  }

  /**
   * Performs setup and initialization of the connector's MQTT manager.
   *
   * @return true if setup and initialization was successful, false otherwise
   */
  public static boolean setUpMqtt() {
    boolean mqttSetUpSuccess = true;
    try {
      final String mqttHost = connectorConfig.getCumulocityHost();

      Logger.LOG_DEBUG("Creating MQTT manager and starting it...");
      final String mqttClientId = MQTT_CLIENT_ID_SERIAL_PREFIX + ewonSerialNumber;
      final String mqttPort = connectorConfig.getCumulocityPort();
      final String mqttUsername =
          connectorConfig.getCumulocityDeviceTenant()
              + "/"
              + connectorConfig.getCumulocityDeviceUsername();
      final String mqttPassword = connectorConfig.getCumulocityDevicePassword();
      final String mqttRootCaFilePath = CConnectorApiCertificateMgr.getRootCaFilePath();
      mqttMgr =
          new CConnectorMqttMgr(
              mqttClientId,
              mqttHost,
              connectorConfig.getStringUtf8Support(),
              mqttPort,
              mqttRootCaFilePath,
              mqttUsername,
              mqttPassword);
      mqttMgr.startWithExponentialRetry(
          AutomaticRetryCodeExponential.MAX_RETRIES_UNLIMITED_VALUE,
          SCTimeUnit.SECONDS.toMillis(CConnectorMqttMgr.MAX_SECONDS_BETWEEN_RETRIES));
    } catch (Exception e) {
      Logger.LOG_CRITICAL("Exception occurred, failed to run MQTT setup!");
      Logger.LOG_EXCEPTION(e);
      mqttSetUpSuccess = false;
    }
    return mqttSetUpSuccess;
  }

  /**
   * Reads the connector configuration from file, and creates a new one if it cannot be found..
   *
   * @return true if the configuration was read successfully, false otherwise.
   */
  private static boolean loadConfiguration() {
    // Load connector configuration
    connectorConfig = new CConnectorConfigFile();

    // If configuration exists on disk, read from disk, otherwise write new default configuration
    boolean configLoadSuccess = true;
    if (connectorConfig.fileExists()) {
      try {
        connectorConfig.read();
      } catch (ConfigFileException e) {
        Logger.LOG_CRITICAL(
            "Unable to read configuration file at "
                + connectorConfig.getConfigFilePath()
                + ". Check that it is properly formatted.");
        Logger.LOG_EXCEPTION(e);
        configLoadSuccess = false;
      }
    } else {
      try {
        connectorConfig.loadAndSaveDefaultConfiguration();
      } catch (ConfigFileWriteException e) {
        Logger.LOG_CRITICAL("Unable to write default configuration file.");
        Logger.LOG_EXCEPTION(e);
        configLoadSuccess = false;
      }
    }
    return configLoadSuccess;
  }

  /** Sets up the connector control tag and its change listener. */
  private static void setUpConnectorControlTag() {
    // Load connector control tag or create new one if it doesn't exist
    try {
      connectorControlTag = new TagControl(CONNECTOR_HALT_TAG_NAME);
    } catch (Exception e1) {
      Logger.LOG_INFO(
          "Unable to create tag object to track connector control tag! Attempting to create `"
              + CONNECTOR_HALT_TAG_NAME
              + "` tag.");
      Logger.LOG_EXCEPTION(e1);
      try {
        SCTagUtils.createTag(
            CONNECTOR_HALT_TAG_NAME,
            CONNECTOR_HALT_TAG_DESCRIPTION,
            CONNECTOR_HALT_TAG_IO_SERVER_NAME,
            CONNECTOR_HALT_TAG_TYPE);
        connectorControlTag = new TagControl(CONNECTOR_HALT_TAG_NAME);
        Logger.LOG_INFO("Created `" + CONNECTOR_HALT_TAG_NAME + "` tag.");
      } catch (Exception e2) {
        Logger.LOG_WARN(
            "Unable to create tag `"
                + CONNECTOR_HALT_TAG_NAME
                + "`! To control this connector, create a boolean tag with the name `"
                + CONNECTOR_HALT_TAG_NAME
                + "`.");
        Logger.LOG_EXCEPTION(e2);
      }
    }

    // Reset value
    if (connectorControlTag != null) {
      try {
        connectorControlTag.setTagValueAsInt(CONNECTOR_CONTROL_TAG_RUN_VALUE);
      } catch (EWException e) {
        Logger.LOG_WARN(
            "Unable to reset tag `"
                + CONNECTOR_HALT_TAG_NAME
                + "` to "
                + CONNECTOR_CONTROL_TAG_RUN_VALUE
                + "! The connector may shut down if the halt tag value indicates.");
        Logger.LOG_EXCEPTION(e);
      }
    }
  }

  /** Sets up the connector control tag and its change listener. */
  private static void setupConnectorMeasurementEnableControlTag() {
    // Load connector control tag or create new one if it doesn't exist
    try {
      connectorMeasurementEnableControlTag = new TagControl(CONNECTOR_MEASUREMENT_CONTROL_TAG_NAME);
    } catch (Exception e1) {
      Logger.LOG_INFO(
          "Unable to create tag object to track connector measurement enable control tag! "
              + "Attempting to create `"
              + CONNECTOR_MEASUREMENT_CONTROL_TAG_NAME
              + "` tag.");
      Logger.LOG_EXCEPTION(e1);
      try {
        SCTagUtils.createPersistentMemTag(
            CONNECTOR_MEASUREMENT_CONTROL_TAG_NAME,
            CONNECTOR_MEASUREMENT_CONTROL_TAG_DESCRIPTION,
            CONNECTOR_MEASUREMENT_CONTROL_TAG_TYPE);
        connectorMeasurementEnableControlTag =
            new TagControl(CONNECTOR_MEASUREMENT_CONTROL_TAG_NAME);
        connectorMeasurementEnableControlTag.setTagValueAsInt(
            CONNECTOR_MEASUREMENT_CONTROL_TAG_ENABLE_VALUE);
        Logger.LOG_INFO("Created `" + CONNECTOR_MEASUREMENT_CONTROL_TAG_NAME + "` tag.");
      } catch (Exception e2) {
        Logger.LOG_WARN(
            "Unable to create tag `"
                + CONNECTOR_MEASUREMENT_CONTROL_TAG_NAME
                + "`! To enable/disable measurements, create a boolean tag with the name `"
                + CONNECTOR_MEASUREMENT_CONTROL_TAG_NAME
                + "`.");
        Logger.LOG_EXCEPTION(e2);
      }
    }
  }
}
