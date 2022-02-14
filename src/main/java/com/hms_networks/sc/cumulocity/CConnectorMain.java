package com.hms_networks.sc.cumulocity;

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
import com.hms_networks.americas.sc.extensions.retry.AutomaticRetryState;
import com.hms_networks.americas.sc.extensions.system.http.SCHttpUtility;
import com.hms_networks.americas.sc.extensions.system.tags.SCTagUtils;
import com.hms_networks.americas.sc.extensions.system.time.SCTimeUnit;
import com.hms_networks.americas.sc.extensions.system.time.SCTimeUtils;
import com.hms_networks.sc.cumulocity.api.CConnectorMqttMgr;
import com.hms_networks.sc.cumulocity.api.CConnectorProvisionMqttMgr;
import com.hms_networks.sc.cumulocity.config.CConnectorConfigFile;
import com.hms_networks.sc.cumulocity.data.CConnectorAlarmMgr;
import com.hms_networks.sc.cumulocity.data.CConnectorDataMgr;
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

  /** The interval (in milliseconds) at which the main loop is executed. */
  private static final int MAIN_LOOP_CYCLE_TIME_MILLIS = 1000;

  /**
   * The HTTP connection timeout time in seconds. This value affects the Ewon's global HTTP
   * timeouts.
   */
  public static final String HTTP_TIMEOUT_SECONDS_STRING = "10";

  /**
   * The prefix appended to the front of the Ewon Flexy's serial number to form the MQTT client ID.
   */
  private static final String MQTT_CLIENT_ID_SERIAL_PREFIX = "HMS-Flexy-";

  /** MQTT manager */
  private static CConnectorMqttMgr mqttMgr;

  /** Alarm manager */
  private static CConnectorAlarmMgr alarmMgr;

  /** Connector configuration object */
  private static CConnectorConfigFile connectorConfig;

  /** Connector control tag object for accessing its value. */
  private static TagControl connectorControlTag = null;

  /** Boolean flag to indicate if the connector is running. */
  private static boolean isRunning = true;

  /**
   * Serial number of the host Ewon device, populated during the initialization of the connector.
   */
  private static String ewonSerialNumber = "";

  /** Boolean indicating if the device should be restarted after the connector shuts down. */
  private static boolean restartDeviceAfterShutdown = false;

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

  /** Triggers a shutdown of the connector and restart of the host device when requested. */
  public static void shutdownConnectorAndRestartDevice() {
    Logger.LOG_CRITICAL("The connector has been requested to shut down and restart the device...");
    restartDeviceAfterShutdown = true;
    isRunning = false;
  }

  /** Method for performing connector application initialization steps. */
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

    // Reset connector control tag value
    if (connectorControlTag != null) {
      try {
        connectorControlTag.setTagValueAsInt(CONNECTOR_CONTROL_TAG_RUN_VALUE);
      } catch (EWException e) {
        Logger.LOG_CRITICAL("Unable to reset the connector control tag value!");
        Logger.LOG_EXCEPTION(e);
        initializeSuccess = false;
      }
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

    // Configure queue diagnostic tags option
    try {
      Logger.LOG_CRITICAL(
          "Configuring the queue diagnostic tags option..."
              + connectorConfig.getQueueDiagnosticTagsEnabled());
      CConnectorDataMgr.configureQueueDiagnosticTags(
          connectorConfig.getQueueDiagnosticTagsEnabled());
    } catch (Exception e) {
      Logger.LOG_CRITICAL("Failed to configure the queue diagnostic tags enabled option!");
      Logger.LOG_EXCEPTION(e);
      initializeSuccess = false;
    }

    if (initializeSuccess) {
      Logger.LOG_CRITICAL("Finished initializing " + CONNECTOR_FRIENDLY_NAME + ".");
    } else {
      Logger.LOG_CRITICAL("Failed to initialize " + CONNECTOR_FRIENDLY_NAME + "!");
    }
    return initializeSuccess;
  }

  /** Method for performing connector application start up steps. */
  private static boolean startUp() {
    Logger.LOG_CRITICAL("Starting " + CONNECTOR_FRIENDLY_NAME + "...");
    boolean startUpSuccess = true;

    setUpConnectorControlTag();

    // Run MQTT provisioning manager if provisioning required
    try {
      if (!connectorConfig.isProvisioned()) {
        startUpSuccess &= runProvisioningMqtt();
      }
    } catch (Exception e) {
      Logger.LOG_CRITICAL("Unable to check existing provisioning status of connector.");
      Logger.LOG_EXCEPTION(e);
    }

    // Start main MQTT manager
    startUpSuccess &= setUpMqtt();

    // Create tag alarm manager
    try {
      alarmMgr = new CConnectorAlarmMgr();
      Logger.LOG_CRITICAL("Created the tag alarm manager.");
    } catch (Exception e) {
      Logger.LOG_CRITICAL("Failed to create the tag alarm manager!");
      Logger.LOG_EXCEPTION(e);
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

    // Grab latest data and send via MQTT
    try {
      CConnectorDataMgr.checkForHistoricalDataAndSend(mqttMgr);
    } catch (Exception e) {
      Logger.LOG_CRITICAL("Unable to grab latest data and send via MQTT.");
      Logger.LOG_EXCEPTION(e);
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
        mqttMgr.disconnect();
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

    // Unregister MQTT from event handler
    if (mqttMgr != null) {
      try {
        DefaultEventHandler.delMqttListener(mqttMgr);
      } catch (Exception e) {
        Logger.LOG_CRITICAL("Unable to unregister MQTT manager from event handler.");
        cleanUpSuccess = false;
      }
    }

    if (cleanUpSuccess) {
      Logger.LOG_CRITICAL("Finished cleaning up " + CONNECTOR_FRIENDLY_NAME + ".");
    } else {
      Logger.LOG_CRITICAL("Failed to clean up " + CONNECTOR_FRIENDLY_NAME + " properly!");
    }
    Logger.LOG_CRITICAL(CONNECTOR_FRIENDLY_NAME + " has finished running.");

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
   * Method for rerunning the connector provisioning process.
   *
   * @return boolean indicating if provisioning and MQTT setup were successful
   */
  public static boolean rerunProvisioning() {
    // Shutdown existing MQTT manager
    if (mqttMgr != null) {
      DefaultEventHandler.delMqttListener(mqttMgr);
      mqttMgr.disconnect();
      mqttMgr.stopMqttThread();
      mqttMgr = null;
    }

    // Run provisioning
    boolean success = runProvisioningMqtt();

    // Setup new MQTT manager
    success &= setUpMqtt();

    return success;
  }

  /**
   * Performs device provisioning to Cumulocity via the configured bootstrap credentials and tenant.
   *
   * @return true if provisioning was successful, false otherwise
   */
  public static boolean runProvisioningMqtt() {
    // Create device provisioning MQTT setup retry-able task
    AutomaticRetryCodeExponential provisioningMqttRetryTask =
        new AutomaticRetryCodeExponential() {
          protected long getMaxDelayMillisBeforeRetry() {
            return SCTimeUnit.SECONDS.toMillis(
                CConnectorProvisionMqttMgr.MAX_SECONDS_BETWEEN_RETRIES);
          }

          protected int getMaxRetries() {
            return MAX_RETRIES_UNLIMITED_VALUE;
          }

          protected void codeToRetry() {
            try {
              Logger.LOG_DEBUG("Starting device provisioning MQTT...");
              final String mqttHost = connectorConfig.getCumulocityHost();

              Logger.LOG_DEBUG("Creating device provisioning MQTT manager and starting it...");
              final String mqttClientId = MQTT_CLIENT_ID_SERIAL_PREFIX + ewonSerialNumber;
              CConnectorProvisionMqttMgr provisioningMqttMgr =
                  new CConnectorProvisionMqttMgr(
                      mqttClientId, mqttHost, connectorConfig.getStringUtf8Support());
              provisioningMqttMgr.startMqttThread();
              provisioningMqttMgr.initConnection();
              Logger.LOG_DEBUG("Finished creating and starting device provisioning MQTT manager.");

              provisioningMqttMgr.awaitProvisioning();
              provisioningMqttMgr.disconnect();
              provisioningMqttMgr.stopMqttThread();
              setState(AutomaticRetryState.FINISHED);
              Logger.LOG_DEBUG("Finished device provisioning MQTT.");
            } catch (Exception e) {
              final int currentTryNumber = getCurrentTryNumber();
              final long delaySecsBeforeRetry =
                  SCTimeUnit.MILLISECONDS.toSeconds(getDelayMillisBeforeRetry(currentTryNumber));
              Logger.LOG_CRITICAL(
                  "MQTT device provisioning encountered an error and failed to complete! (Attempt #"
                      + currentTryNumber
                      + ") Retrying in "
                      + delaySecsBeforeRetry
                      + " seconds.");
              Logger.LOG_EXCEPTION(e);
              setState(AutomaticRetryState.ERROR_RETRY);
            }
          }
        };

    // Run MQTT device provisioning task
    boolean mqttProvisionSuccess = true;
    try {
      provisioningMqttRetryTask.run();
    } catch (InterruptedException e) {
      Logger.LOG_CRITICAL("Failed to run MQTT device provisioning due to an InterruptedException!");
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
    // Create MQTT setup retry-able task
    AutomaticRetryCodeExponential mqttSetUpRetryTask =
        new AutomaticRetryCodeExponential() {
          protected long getMaxDelayMillisBeforeRetry() {
            return SCTimeUnit.SECONDS.toMillis(CConnectorMqttMgr.MAX_SECONDS_BETWEEN_RETRIES);
          }

          protected int getMaxRetries() {
            return MAX_RETRIES_UNLIMITED_VALUE;
          }

          protected void codeToRetry() {
            try {
              Logger.LOG_DEBUG("Setting up MQTT...");
              final String mqttHost = connectorConfig.getCumulocityHost();

              Logger.LOG_DEBUG("Creating MQTT manager and starting it...");
              final String mqttClientId = MQTT_CLIENT_ID_SERIAL_PREFIX + ewonSerialNumber;
              mqttMgr =
                  new CConnectorMqttMgr(
                      mqttClientId, mqttHost, connectorConfig.getStringUtf8Support());
              mqttMgr.startMqttThread();
              mqttMgr.initConnection();

              Logger.LOG_DEBUG("Finished creating and starting MQTT manager.");
              setState(AutomaticRetryState.FINISHED);
              Logger.LOG_DEBUG("Finished setting up MQTT.");
            } catch (Exception e) {
              final int currentTryNumber = getCurrentTryNumber();
              final long delaySecsBeforeRetry =
                  SCTimeUnit.MILLISECONDS.toSeconds(getDelayMillisBeforeRetry(currentTryNumber));
              Logger.LOG_CRITICAL(
                  "MQTT setup encountered an error and failed to complete! (Attempt #"
                      + currentTryNumber
                      + ") Retrying in "
                      + delaySecsBeforeRetry
                      + " seconds.");
              Logger.LOG_EXCEPTION(e);
              mqttMgr = null;
              setState(AutomaticRetryState.ERROR_RETRY);
            }
          }
        };

    // Run MQTT setup task
    boolean mqttSetUpSuccess = true;
    try {
      mqttSetUpRetryTask.run();
    } catch (InterruptedException e) {
      Logger.LOG_CRITICAL("Failed to run MQTT setup due to an InterruptedException!");
      Logger.LOG_EXCEPTION(e);
      mqttSetUpSuccess = false;
    }
    return mqttSetUpSuccess;
  }

  /**
   * Shuts down the existing MQTT manager and creates a new one.
   *
   * @return true if the MQTT manager was successfully shut down and recreated, false otherwise
   */
  public static boolean restartMqtt() {
    // Shutdown and cleanup existing MQTT manager
    if (mqttMgr != null) {
      DefaultEventHandler.delMqttListener(mqttMgr);
      mqttMgr.disconnect();
      mqttMgr.stopMqttThread();
      mqttMgr = null;
    }

    // Set up new MQTT manager
    return setUpMqtt();
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
  }
}
