package com.hmsnetworks.sc.cumulocity.config;

import com.hms_networks.americas.sc.extensions.config.ConfigFile;
import com.hms_networks.americas.sc.extensions.historicaldata.HistoricalDataQueueManager;
import com.hms_networks.americas.sc.extensions.json.JSONException;
import com.hms_networks.americas.sc.extensions.json.JSONObject;
import com.hms_networks.americas.sc.extensions.logging.Logger;
import com.hms_networks.americas.sc.extensions.string.StringUtils;
import com.hmsnetworks.sc.cumulocity.data.CConnectorAggregationMethod;
import com.hmsnetworks.sc.cumulocity.data.CConnectorDataProcessingMode;
import java.util.List;

/**
 * Configuration file management class for the Ewon Flexy Cumulocity Connector.
 *
 * @author HMS Networks, MU Americas Solution Center
 * @since 1.0.0
 */
public class CConnectorConfigFile extends ConfigFile {

  /** Indent factor for the configuration file JSON contents. */
  private static final int CONFIG_FILE_JSON_INDENT_FACTOR = 3;

  /** File path for the configuration file on the Ewon Flexy's file system. */
  private static final String CONFIG_FILE_PATH = "/usr/CumulocityConnectorConfig.json";

  /** Key for accessing the 'Connector' object in the configuration file. */
  private static final String CONFIG_FILE_CONNECTOR_KEY = "Connector";

  /** Key for accessing the 'Cumulocity' object in the configuration file. */
  private static final String CONFIG_FILE_CUMULOCITY_KEY = "Cumulocity";

  /** Key for accessing the 'Tenant' object in the configuration file. */
  private static final String CONFIG_FILE_BOOTSTRAP_TENANT_KEY = "BootstrapTenant";

  /** Key for accessing the 'Tenant' object in the configuration file. */
  private static final String CONFIG_FILE_DEVICE_TENANT_KEY = "DeviceTenant";

  /** Key for accessing the 'Username' object in the configuration file. */
  private static final String CONFIG_FILE_DEVICE_USERNAME_KEY = "DeviceUsername";

  /** Key for accessing the 'Password' object in the configuration file. */
  private static final String CONFIG_FILE_DEVICE_PASSWORD_KEY = "DevicePassword";

  /** Key for accessing the 'BootstrapUsername' object in the configuration file. */
  private static final String CONFIG_FILE_BOOTSTRAP_USERNAME_KEY = "BootstrapUsername";

  /** Key for accessing the 'BootstrapPassword' object in the configuration file. */
  private static final String CONFIG_FILE_BOOTSTRAP_PASSWORD_KEY = "BootstrapPassword";

  /** Key for accessing the 'Host' object in the configuration file. */
  private static final String CONFIG_FILE_HOST_KEY = "Host";

  /**
   * Key for accessing the prior (unsupported as of v1.3.3) 'CertificateUrl' object in the
   * configuration file.
   */
  private static final String CONFIG_FILE_CERTIFICATE_URL_KEY_UNSUPPORTED = "CertificateUrl";

  /** Key for accessing the 'CustomCertificateUrl' object in the configuration file. */
  private static final String CONFIG_FILE_CUSTOM_CERTIFICATE_URL_KEY = "CustomCertificateUrl";

  /** Key for accessing the 'CustomCertificateUrlEnabled' object in the configuration file. */
  private static final String CONFIG_FILE_CUSTOM_CERTIFICATE_URL_ENABLED_KEY =
      "CustomCertificateUrlEnabled";

  /** Key for accessing the 'Port' object in the configuration file. */
  private static final String CONFIG_FILE_PORT_KEY = "Port";

  /** Key for accessing the 'SubscribeToErrors' object in the configuration file. */
  private static final String CONFIG_FILE_SUBSCRIBE_TO_ERRORS_KEY = "SubscribeToErrors";

  /** Key for accessing the 'DataProcessingMode' object in the configuration file. */
  private static final String CONFIG_FILE_DATA_PROCESSING_MODE_KEY = "DataProcessingMode";

  /** Key for accessing the 'ParentDeviceAggregatedPayloadType' object in the configuration file. */
  private static final String CONFIG_FILE_PARENT_DEVICE_AGGREGATED_PAYLOAD_TYPE_KEY =
      "ParentDeviceAggregatedPayloadType";

  /** Key for accessing the 'LogLevel' object in the configuration file. */
  private static final String CONFIG_FILE_LOG_LEVEL_KEY = "LogLevel";

  /** Key for accessing the 'UTF8StringSupport' object in the configuration file. */
  private static final String CONFIG_FILE_UTF8_STRING_SUPPORT_KEY = "UTF8StringSupport";

  /** Key for accessing the 'QueueEnableStringHistory' object in the configuration file. */
  private static final String CONFIG_FILE_QUEUE_STRING_HISTORY_KEY = "QueueEnableStringHistory";

  /** Key for accessing the 'QueueDataPollSizeMins' object in the configuration file. */
  private static final String CONFIG_FILE_QUEUE_DATA_POLL_SIZE_MINS_KEY = "QueueDataPollSizeMins";

  /** Key for accessing the 'QueueDataPollMaxBehindTimeMins' object in the configuration file. */
  private static final String CONFIG_FILE_QUEUE_DATA_POLL_MAX_BEHIND_TIME_MINS_KEY =
      "QueueDataPollMaxBehindTimeMins";

  /** Key for accessing the 'QueueDataAggregationPeriodSecs' object in the configuration file. */
  public static final String CONFIG_FILE_QUEUE_DATA_AGGREGATION_PERIOD_SECS_KEY =
      "QueueDataAggregationPeriodSecs";

  /** Key for accessing the 'QueueDataAggregationMethod' object in the configuration file. */
  public static final String CONFIG_FILE_QUEUE_DATA_AGGREGATION_METHOD_KEY =
      "QueueDataAggregationMethod";

  /** Key for accessing the 'QueueDataPollIntervalMillis' object in the configuration file. */
  private static final String CONFIG_FILE_QUEUE_DATA_POLL_INTERVAL_MILLIS_KEY =
      "QueueDataPollIntervalMillis";

  /** The configuration file JSON key for the enable queue diagnostic tags setting. */
  public static final String CONFIG_FILE_ENABLE_QUEUE_DIAGNOSTIC_TAGS_KEY =
      "QueueEnableDiagnosticTags";

  /**
   * The default size (in mins) of each data queue poll. Changing this will modify the amount of
   * data checked during each poll interval.
   */
  private static final long QUEUE_DATA_POLL_SIZE_MINS_DEFAULT = 1;

  /**
   * The default maximum time (in mins) which data polling may run behind. Changing this will modify
   * the amount of time which data polling may run behind by. By default, this functionality is
   * disabled. The value {@link HistoricalDataQueueManager#DISABLED_MAX_HIST_FIFO_GET_BEHIND_MINS}
   * indicates that the functionality is disabled.
   */
  private static final long QUEUE_DATA_POLL_MAX_BEHIND_TIME_MINS_DEFAULT =
      HistoricalDataQueueManager.DISABLED_MAX_HIST_FIFO_GET_BEHIND_MINS;

  /** The default interval (in milliseconds) to poll the historical data queue. */
  private static final long QUEUE_DATA_POLL_INTERVAL_MILLIS_DEFAULT = 10000;

  /** The data aggregation period value which indicates that data aggregation is disabled. */
  public static final long QUEUE_DATA_AGGREGATION_PERIOD_SECS_DISABLED = -1;

  /**
   * The default aggregation period (in seconds) for data points processed from the historical data
   * queue. The value in {@link #QUEUE_DATA_AGGREGATION_PERIOD_SECS_DISABLED} indicates that data
   * aggregation is disabled.
   */
  public static final long CONFIG_FILE_QUEUE_DATA_AGGREGATION_PERIOD_SECS_DEFAULT =
      QUEUE_DATA_AGGREGATION_PERIOD_SECS_DISABLED;

  /** The default aggregation method for data points processed from the historical data queue. */
  public static final CConnectorAggregationMethod
      CONFIG_FILE_QUEUE_DATA_AGGREGATION_METHOD_DEFAULT =
          CConnectorAggregationMethod.LAST_RECORDED_DATA;

  /** The default value for the subscribe to errors setting. */
  private static final boolean SUBSCRIBE_TO_ERRORS_DEFAULT = false;

  /** The default value for the data processing mode setting. */
  private static final CConnectorDataProcessingMode DATA_PROCESSING_MODE_DEFAULT =
      CConnectorDataProcessingMode.PERSISTENT;

  /** The default value for the custom certificate URL setting. */
  private static final String CUSTOM_CERTIFICATE_URL_DEFAULT =
      "https://certs.godaddy.com/repository/gdroot-g2.crt";

  /** The default value for the parent device's aggregated data 'type' setting. */
  private static final String PARENT_DEVICE_AGGREGATED_PAYLOAD_TYPE_DEFAULT = "None";

  /** The default value for the custom certificate URL enabled setting. */
  private static final boolean CUSTOM_CERTIFICATE_URL_ENABLED_DEFAULT = false;

  /** The default value for the port setting. */
  private static final String PORT_DEFAULT = "8883";

  /**
   * Default value of boolean flag indicating if string history data should be retrieved from the
   * queue. String history requires an additional EBD call in the underlying queue library, and will
   * take extra processing time, especially in installations with large string tag counts.
   */
  private static final boolean QUEUE_DATA_STRING_HISTORY_ENABLED_DEFAULT = false;

  /** The default value for the queue diagnostic tags enabled setting. */
  public static final boolean ENABLE_QUEUE_DIAGNOSTIC_TAGS_DEFAULT = false;

  /** Text string used to indicate a field which is automatically populated by the connector. */
  private static final String CONFIG_FILE_AUTOMATICALLY_FILLED_TEXT = "<automatically filled>";

  /**
   * Text string used to represent a password value which has been masked (typically for security).
   */
  private static final String CONFIG_FILE_PASSWORD_MASKED_VALUE = "********";

  /**
   * The configuration field name used for the connector log level setting when communicating with
   * Cumulocity.
   */
  private static final String CONNECTOR_LOG_LEVEL_CONFIG_NAME =
      CONFIG_FILE_CONNECTOR_KEY + "/" + CONFIG_FILE_LOG_LEVEL_KEY;

  /**
   * The configuration field name used for the connector UTF-8 string support setting when
   * communicating with Cumulocity.
   */
  private static final String CONNECTOR_UTF8_STRING_SUPPORT_CONFIG_NAME =
      CONFIG_FILE_CONNECTOR_KEY + "/" + CONFIG_FILE_UTF8_STRING_SUPPORT_KEY;

  /**
   * The configuration field name used for the connector queue data poll interval (in milliseconds)
   * setting when communicating with Cumulocity.
   */
  private static final String CONNECTOR_QUEUE_DATA_POLL_INTERVAL_MILLIS_CONFIG_NAME =
      CONFIG_FILE_CONNECTOR_KEY + "/" + CONFIG_FILE_QUEUE_DATA_POLL_INTERVAL_MILLIS_KEY;

  /**
   * The configuration field name used for the connector queue data poll size (in minutes) setting
   * when communicating with Cumulocity.
   */
  private static final String CONNECTOR_QUEUE_DATA_POLL_SIZE_MINS_CONFIG_NAME =
      CONFIG_FILE_CONNECTOR_KEY + "/" + CONFIG_FILE_QUEUE_DATA_POLL_SIZE_MINS_KEY;

  /**
   * The configuration field name used for the connector maximum data polling run behind time (in
   * minutes) setting when communicating with Cumulocity.
   */
  private static final String CONNECTOR_QUEUE_DATA_POLL_MAX_BEHIND_TIME_MINS_CONFIG_NAME =
      CONFIG_FILE_CONNECTOR_KEY + "/" + CONFIG_FILE_QUEUE_DATA_POLL_MAX_BEHIND_TIME_MINS_KEY;

  /**
   * The configuration field name used for the connector queue data aggregation period (in seconds)
   * setting when communicating with Cumulocity.
   */
  private static final String CONNECTOR_QUEUE_DATA_AGGREGATION_PERIOD_SECS_CONFIG_NAME =
      CONFIG_FILE_CONNECTOR_KEY + "/" + CONFIG_FILE_QUEUE_DATA_AGGREGATION_PERIOD_SECS_KEY;

  /**
   * The configuration field name used for the connector queue data aggregation method setting when
   * communicating with Cumulocity.
   */
  private static final String CONNECTOR_QUEUE_DATA_AGGREGATION_METHOD_CONFIG_NAME =
      CONFIG_FILE_CONNECTOR_KEY + "/" + CONFIG_FILE_QUEUE_DATA_AGGREGATION_METHOD_KEY;

  /**
   * The configuration field name used for the connector queue diagnostic tags enable setting when
   * communicating with Cumulocity.
   */
  private static final String CONNECTOR_QUEUE_ENABLE_DIAGNOSTIC_TAGS_CONFIG_NAME =
      CONFIG_FILE_CONNECTOR_KEY + "/" + CONFIG_FILE_ENABLE_QUEUE_DIAGNOSTIC_TAGS_KEY;

  /**
   * The configuration field name used for the connector queue string history data enable setting
   * when communicating with Cumulocity.
   */
  private static final String CONNECTOR_QUEUE_ENABLE_STRING_HISTORY_CONFIG_NAME =
      CONFIG_FILE_CONNECTOR_KEY + "/" + CONFIG_FILE_QUEUE_STRING_HISTORY_KEY;

  /**
   * The configuration field name used for the Cumulocity bootstrap username setting when
   * communicating with Cumulocity.
   */
  private static final String CUMULOCITY_BOOTSTRAP_USERNAME_CONFIG_NAME =
      CONFIG_FILE_CUMULOCITY_KEY + "/" + CONFIG_FILE_BOOTSTRAP_USERNAME_KEY;

  /**
   * The configuration field name used for the Cumulocity bootstrap password setting when
   * communicating with Cumulocity.
   */
  private static final String CUMULOCITY_BOOTSTRAP_PASSWORD_CONFIG_NAME =
      CONFIG_FILE_CUMULOCITY_KEY + "/" + CONFIG_FILE_BOOTSTRAP_PASSWORD_KEY;

  /**
   * The configuration field name used for the Cumulocity bootstrap tenant setting when
   * communicating with Cumulocity.
   */
  private static final String CUMULOCITY_BOOTSTRAP_TENANT_CONFIG_NAME =
      CONFIG_FILE_CUMULOCITY_KEY + "/" + CONFIG_FILE_BOOTSTRAP_TENANT_KEY;

  /**
   * The configuration field name used for the Cumulocity device username setting when communicating
   * with Cumulocity.
   */
  private static final String CUMULOCITY_DEVICE_USERNAME_CONFIG_NAME =
      CONFIG_FILE_CUMULOCITY_KEY + "/" + CONFIG_FILE_DEVICE_USERNAME_KEY;

  /**
   * The configuration field name used for the Cumulocity device password setting when communicating
   * with Cumulocity.
   */
  private static final String CUMULOCITY_DEVICE_PASSWORD_CONFIG_NAME =
      CONFIG_FILE_CUMULOCITY_KEY + "/" + CONFIG_FILE_DEVICE_PASSWORD_KEY;

  /**
   * The configuration field name used for the Cumulocity device tenant setting when communicating
   * with Cumulocity.
   */
  private static final String CUMULOCITY_DEVICE_TENANT_CONFIG_NAME =
      CONFIG_FILE_CUMULOCITY_KEY + "/" + CONFIG_FILE_DEVICE_TENANT_KEY;

  /**
   * The configuration field name used for the Cumulocity host setting when communicating with
   * Cumulocity.
   */
  private static final String CUMULOCITY_HOST_CONFIG_NAME =
      CONFIG_FILE_CUMULOCITY_KEY + "/" + CONFIG_FILE_HOST_KEY;

  /**
   * The configuration field name used for the Cumulocity custom certificate URL setting when
   * communicating with Cumulocity.
   */
  private static final String CUMULOCITY_CUSTOM_CERTIFICATE_URL_CONFIG_NAME =
      CONFIG_FILE_CUMULOCITY_KEY + "/" + CONFIG_FILE_CUSTOM_CERTIFICATE_URL_KEY;

  /**
   * The configuration field name used for the Cumulocity custom certificate URL enabled setting
   * when communicating with Cumulocity.
   */
  private static final String CUMULOCITY_CUSTOM_CERTIFICATE_URL_ENABLED_CONFIG_NAME =
      CONFIG_FILE_CUMULOCITY_KEY + "/" + CONFIG_FILE_CUSTOM_CERTIFICATE_URL_ENABLED_KEY;

  /**
   * The configuration field name used for the Cumulocity port setting when communicating with
   * Cumulocity.
   */
  private static final String CUMULOCITY_PORT_CONFIG_NAME =
      CONFIG_FILE_CUMULOCITY_KEY + "/" + CONFIG_FILE_PORT_KEY;

  /**
   * The configuration field name used for the Cumulocity subscribe to errors setting when
   * communicating with Cumulocity.
   */
  private static final String CUMULOCITY_SUBSCRIBE_TO_ERRORS_CONFIG_NAME =
      CONFIG_FILE_CUMULOCITY_KEY + "/" + CONFIG_FILE_SUBSCRIBE_TO_ERRORS_KEY;

  /**
   * The configuration field name used for the Cumulocity processing mode setting when communicating
   * with Cumulocity.
   */
  private static final String CUMULOCITY_DATA_PROCESSING_MODE_CONFIG_NAME =
      CONFIG_FILE_CUMULOCITY_KEY + "/" + CONFIG_FILE_DATA_PROCESSING_MODE_KEY;

  /**
   * The configuration field name used for the 'type' value in aggregated data payloads associated
   * with the parent device when communicating with Cumulocity.
   */
  private static final String CUMULOCITY_PARENT_DEVICE_AGGREGATED_PAYLOAD_TYPE_CONFIG_NAME =
      CONFIG_FILE_CUMULOCITY_KEY + "/" + CONFIG_FILE_PARENT_DEVICE_AGGREGATED_PAYLOAD_TYPE_KEY;

  /**
   * Returns a boolean indicating if device provisioning is required.
   *
   * @return true if device provisioning is required, false otherwise
   * @throws JSONException if unable to get device provisioning status from configuration
   */
  public boolean isProvisioned() throws JSONException {
    return !getCumulocityDeviceUsername().equals(CONFIG_FILE_AUTOMATICALLY_FILLED_TEXT)
        && !getCumulocityDevicePassword().equals(CONFIG_FILE_AUTOMATICALLY_FILLED_TEXT)
        && getCumulocityDeviceUsername().length() > 0
        && getCumulocityDevicePassword().length() > 0;
  }

  /**
   * Resets the device authentication configuration (tenant, username, and password) to the default
   * values.
   *
   * @throws JSONException if unable to reset the device authentication configuration
   */
  public void resetDeviceAuthenticationConfiguration() throws JSONException {
    configurationObject.put(CONFIG_FILE_DEVICE_USERNAME_KEY, CONFIG_FILE_AUTOMATICALLY_FILLED_TEXT);
    configurationObject.put(CONFIG_FILE_DEVICE_PASSWORD_KEY, CONFIG_FILE_AUTOMATICALLY_FILLED_TEXT);
    configurationObject.put(CONFIG_FILE_DEVICE_TENANT_KEY, CONFIG_FILE_AUTOMATICALLY_FILLED_TEXT);
    trySave();
  }

  /**
   * Returns a boolean indicating if the bootstrap credentials are configured.
   *
   * @return true if the bootstrap credentials are configured, false otherwise
   * @throws JSONException if unable to get bootstrap credentials from configuration
   */
  public boolean isBootstrapConfigured() throws JSONException {
    return getCumulocityBootstrapTenant().length() > 0
        && getCumulocityBootstrapUsername().length() > 0
        && getCumulocityBootstrapPassword().length() > 0;
  }

  /**
   * Get the configured Cumulocity bootstrap tenant.
   *
   * @return Cumulocity bootstrap tenant
   * @throws JSONException if unable to get Cumulocity bootstrap tenant from configuration
   */
  public String getCumulocityBootstrapTenant() throws JSONException {
    return configurationObject
        .getJSONObject(CONFIG_FILE_CUMULOCITY_KEY)
        .getString(CONFIG_FILE_BOOTSTRAP_TENANT_KEY);
  }

  /**
   * Set the configured Cumulocity bootstrap tenant.
   *
   * @param bootstrapTenant Cumulocity bootstrap tenant
   * @throws JSONException if unable to set Cumulocity bootstrap tenant in configuration
   */
  public void setCumulocityBootstrapTenant(String bootstrapTenant) throws JSONException {
    configurationObject
        .getJSONObject(CONFIG_FILE_CUMULOCITY_KEY)
        .put(CONFIG_FILE_BOOTSTRAP_TENANT_KEY, bootstrapTenant);
    trySave();
  }

  /**
   * Get the configured Cumulocity device tenant.
   *
   * @return Cumulocity device tenant
   * @throws JSONException if unable to get Cumulocity device tenant from configuration
   */
  public String getCumulocityDeviceTenant() throws JSONException {
    return configurationObject
        .getJSONObject(CONFIG_FILE_CUMULOCITY_KEY)
        .getString(CONFIG_FILE_DEVICE_TENANT_KEY);
  }

  /**
   * Set the configured Cumulocity device tenant.
   *
   * @param deviceTenant Cumulocity device tenant
   * @throws JSONException if unable to set Cumulocity device tenant in configuration
   */
  public void setCumulocityDeviceTenant(String deviceTenant) throws JSONException {
    configurationObject
        .getJSONObject(CONFIG_FILE_CUMULOCITY_KEY)
        .put(CONFIG_FILE_DEVICE_TENANT_KEY, deviceTenant);
    trySave();
  }

  /**
   * Get the configured Cumulocity bootstrap username.
   *
   * @return Cumulocity bootstrap username
   * @throws JSONException if unable to get Cumulocity username from configuration
   */
  public String getCumulocityBootstrapUsername() throws JSONException {
    return configurationObject
        .getJSONObject(CONFIG_FILE_CUMULOCITY_KEY)
        .getString(CONFIG_FILE_BOOTSTRAP_USERNAME_KEY);
  }

  /**
   * Set the configured Cumulocity bootstrap username.
   *
   * @param bootstrapUsername Cumulocity bootstrap username
   * @throws JSONException if unable to set Cumulocity bootstrap username in configuration
   */
  public void setCumulocityBootstrapUsername(String bootstrapUsername) throws JSONException {
    configurationObject
        .getJSONObject(CONFIG_FILE_CUMULOCITY_KEY)
        .put(CONFIG_FILE_BOOTSTRAP_USERNAME_KEY, bootstrapUsername);
    trySave();
  }

  /**
   * Get the configured Cumulocity device username.
   *
   * @return Cumulocity device username
   * @throws JSONException if unable to get Cumulocity username from configuration
   */
  public String getCumulocityDeviceUsername() throws JSONException {
    return configurationObject
        .getJSONObject(CONFIG_FILE_CUMULOCITY_KEY)
        .getString(CONFIG_FILE_DEVICE_USERNAME_KEY);
  }

  /**
   * Set the configured Cumulocity device username.
   *
   * @param deviceUsername Cumulocity device username
   * @throws JSONException if unable to set Cumulocity device username in configuration
   */
  public void setCumulocityDeviceUsername(String deviceUsername) throws JSONException {
    configurationObject
        .getJSONObject(CONFIG_FILE_CUMULOCITY_KEY)
        .put(CONFIG_FILE_DEVICE_USERNAME_KEY, deviceUsername);
    trySave();
  }

  /**
   * Get the configured Cumulocity bootstrap password.
   *
   * @return Cumulocity bootstrap password
   * @throws JSONException if unable to get Cumulocity password from configuration
   */
  public String getCumulocityBootstrapPassword() throws JSONException {
    return configurationObject
        .getJSONObject(CONFIG_FILE_CUMULOCITY_KEY)
        .getString(CONFIG_FILE_BOOTSTRAP_PASSWORD_KEY);
  }

  /**
   * Set the configured Cumulocity bootstrap password.
   *
   * @param bootstrapPassword Cumulocity bootstrap password
   * @throws JSONException if unable to set Cumulocity bootstrap password in configuration
   */
  public void setCumulocityBootstrapPassword(String bootstrapPassword) throws JSONException {
    configurationObject
        .getJSONObject(CONFIG_FILE_CUMULOCITY_KEY)
        .put(CONFIG_FILE_BOOTSTRAP_PASSWORD_KEY, bootstrapPassword);
    trySave();
  }

  /**
   * Get the configured Cumulocity device password.
   *
   * @return Cumulocity device password
   * @throws JSONException if unable to get Cumulocity password from configuration
   */
  public String getCumulocityDevicePassword() throws JSONException {
    return configurationObject
        .getJSONObject(CONFIG_FILE_CUMULOCITY_KEY)
        .getString(CONFIG_FILE_DEVICE_PASSWORD_KEY);
  }

  /**
   * Set the configured Cumulocity device password.
   *
   * @param devicePassword Cumulocity device password
   * @throws JSONException if unable to set Cumulocity device password in configuration
   */
  public void setCumulocityDevicePassword(String devicePassword) throws JSONException {
    configurationObject
        .getJSONObject(CONFIG_FILE_CUMULOCITY_KEY)
        .put(CONFIG_FILE_DEVICE_PASSWORD_KEY, devicePassword);
    trySave();
  }

  /**
   * Gets the configured Cumulocity host.
   *
   * @return Cumulocity host
   * @throws JSONException if unable to get Cumulocity host from configuration
   */
  public String getCumulocityHost() throws JSONException {
    return configurationObject
        .getJSONObject(CONFIG_FILE_CUMULOCITY_KEY)
        .getString(CONFIG_FILE_HOST_KEY);
  }

  /**
   * Sets the configured Cumulocity host.
   *
   * @param host Cumulocity host
   * @throws JSONException if unable to set Cumulocity host in configuration
   */
  public void setCumulocityHost(String host) throws JSONException {
    configurationObject.getJSONObject(CONFIG_FILE_CUMULOCITY_KEY).put(CONFIG_FILE_HOST_KEY, host);
    trySave();
  }

  /**
   * Checks for an old, unsupported certificate URL configuration field and migrates it to the new
   * custom certificate URL configuration field. If an old, unsupported certificate URL
   * configuration field is detected, then the custom certificate URL enabled setting is set to true
   * to make the transition seamless for the user.
   *
   * <p>This method should be called before getting the custom certificate URL or custom certificate
   * URL enabled fields from the configuration.
   *
   * @throws JSONException if unable to read the existing configuration or migrate a detected old,
   *     unsupported configuration
   */
  private void checkMigrateUnsupportedCertificateUrlConfiguration() throws JSONException {
    // Check for old, unsupported certificate URL configuration field
    String unsupportedCertificateUrl = null;
    if (configurationObject
        .getJSONObject(CONFIG_FILE_CUMULOCITY_KEY)
        .has(CONFIG_FILE_CERTIFICATE_URL_KEY_UNSUPPORTED)) {
      String unsupportedCertificateUrlRaw =
          configurationObject
              .getJSONObject(CONFIG_FILE_CUMULOCITY_KEY)
              .getString(CONFIG_FILE_CERTIFICATE_URL_KEY_UNSUPPORTED);
      if (unsupportedCertificateUrlRaw != null && unsupportedCertificateUrlRaw.length() > 0) {
        unsupportedCertificateUrl = unsupportedCertificateUrlRaw;
      }
    }

    // Migrate to new certificate URL configuration field
    if (unsupportedCertificateUrl != null) {
      final boolean cumulocityCustomCertificateUrlEnabled = true;
      configurationObject
          .getJSONObject(CONFIG_FILE_CUMULOCITY_KEY)
          .put(CONFIG_FILE_CUSTOM_CERTIFICATE_URL_KEY, unsupportedCertificateUrl);
      configurationObject
          .getJSONObject(CONFIG_FILE_CUMULOCITY_KEY)
          .put(
              CONFIG_FILE_CUSTOM_CERTIFICATE_URL_ENABLED_KEY,
              cumulocityCustomCertificateUrlEnabled);
      configurationObject
          .getJSONObject(CONFIG_FILE_CUMULOCITY_KEY)
          .remove(CONFIG_FILE_CERTIFICATE_URL_KEY_UNSUPPORTED);
      trySave();
    }
  }

  /**
   * Sets the configured Cumulocity certificate URL.
   *
   * @param cumulocityCertificateUrl Cumulocity certificate URL
   * @throws JSONException if unable to set Cumulocity certificate URL in configuration
   */
  public void setCumulocityCustomCertificateUrl(String cumulocityCertificateUrl)
      throws JSONException {
    configurationObject
        .getJSONObject(CONFIG_FILE_CUMULOCITY_KEY)
        .put(CONFIG_FILE_CUSTOM_CERTIFICATE_URL_KEY, cumulocityCertificateUrl);
    trySave();
  }

  /**
   * Gets the configured Cumulocity certificate URL.
   *
   * @return Cumulocity certificate URL
   */
  public String getCumulocityCustomCertificateUrl() {
    String cumulocityCustomCertificateUrl;
    try {
      // Check for old, unsupported certificate URL configuration field and migrate it if necessary
      checkMigrateUnsupportedCertificateUrlConfiguration();

      // Set to default if not present
      if (!configurationObject
          .getJSONObject(CONFIG_FILE_CUMULOCITY_KEY)
          .has(CONFIG_FILE_CUSTOM_CERTIFICATE_URL_KEY)) {
        Logger.LOG_DEBUG(
            "No value was specified for \""
                + CONFIG_FILE_CUSTOM_CERTIFICATE_URL_KEY
                + "\" in the configuration file. Defaulting to "
                + CUSTOM_CERTIFICATE_URL_DEFAULT
                + ".");
        setCumulocityCustomCertificateUrl(CUSTOM_CERTIFICATE_URL_DEFAULT);
      }

      // Get the value
      cumulocityCustomCertificateUrl =
          configurationObject
              .getJSONObject(CONFIG_FILE_CUMULOCITY_KEY)
              .getString(CONFIG_FILE_CUSTOM_CERTIFICATE_URL_KEY);
    } catch (Exception e) {
      Logger.LOG_SERIOUS(
          "An error occurred while attempting to get the value for \""
              + CONFIG_FILE_CUSTOM_CERTIFICATE_URL_KEY
              + "\" from the configuration file. Defaulting to "
              + CUSTOM_CERTIFICATE_URL_DEFAULT
              + ".");
      Logger.LOG_EXCEPTION(e);

      cumulocityCustomCertificateUrl = CUSTOM_CERTIFICATE_URL_DEFAULT;
    }

    return cumulocityCustomCertificateUrl;
  }

  /**
   * Sets the configured Cumulocity certificate URL enabled setting.
   *
   * @param cumulocityCertificateUrlEnabled Cumulocity certificate URL enabled setting
   * @throws JSONException if unable to set Cumulocity certificate URL enabled setting in
   *     configuration
   */
  public void setCumulocityCustomCertificateUrlEnabled(boolean cumulocityCertificateUrlEnabled)
      throws JSONException {
    configurationObject
        .getJSONObject(CONFIG_FILE_CUMULOCITY_KEY)
        .put(CONFIG_FILE_CUSTOM_CERTIFICATE_URL_ENABLED_KEY, cumulocityCertificateUrlEnabled);
    trySave();
  }

  /**
   * Gets the configured Cumulocity certificate URL enabled setting.
   *
   * @return true if a Cumulocity certificate URL is enabled, false otherwise
   */
  public boolean getCumulocityCustomCertificateUrlEnabled() {
    boolean cumulocityCustomCertificateUrlEnabled;
    try {
      // Check for old, unsupported certificate URL configuration field and migrate it if necessary
      checkMigrateUnsupportedCertificateUrlConfiguration();

      // Set to default if not present
      if (!configurationObject
          .getJSONObject(CONFIG_FILE_CUMULOCITY_KEY)
          .has(CONFIG_FILE_CUSTOM_CERTIFICATE_URL_ENABLED_KEY)) {
        Logger.LOG_DEBUG(
            "No value was specified for \""
                + CONFIG_FILE_CUSTOM_CERTIFICATE_URL_ENABLED_KEY
                + "\" in the configuration file. Defaulting to "
                + CUSTOM_CERTIFICATE_URL_ENABLED_DEFAULT
                + ".");
        setCumulocityCustomCertificateUrlEnabled(CUSTOM_CERTIFICATE_URL_ENABLED_DEFAULT);
      }

      // Get the value
      cumulocityCustomCertificateUrlEnabled =
          configurationObject
              .getJSONObject(CONFIG_FILE_CUMULOCITY_KEY)
              .getBoolean(CONFIG_FILE_CUSTOM_CERTIFICATE_URL_ENABLED_KEY);
    } catch (Exception e) {
      Logger.LOG_SERIOUS(
          "An error occurred while attempting to get the value for \""
              + CONFIG_FILE_CUSTOM_CERTIFICATE_URL_ENABLED_KEY
              + "\" from the configuration file. Defaulting to "
              + CUSTOM_CERTIFICATE_URL_ENABLED_DEFAULT
              + ".");
      Logger.LOG_EXCEPTION(e);

      cumulocityCustomCertificateUrlEnabled = CUSTOM_CERTIFICATE_URL_ENABLED_DEFAULT;
    }

    return cumulocityCustomCertificateUrlEnabled;
  }

  /**
   * Gets the Cumulocity subscribe to errors setting.
   *
   * @return Cumulocity subscribe to errors setting
   */
  public boolean getCumulocitySubscribeToErrors() {
    boolean subscribeToErrors = SUBSCRIBE_TO_ERRORS_DEFAULT;
    try {
      subscribeToErrors =
          configurationObject
              .getJSONObject(CONFIG_FILE_CUMULOCITY_KEY)
              .getBoolean(CONFIG_FILE_SUBSCRIBE_TO_ERRORS_KEY);
    } catch (JSONException e) {
      Logger.LOG_DEBUG(
          CONFIG_FILE_SUBSCRIBE_TO_ERRORS_KEY
              + " not found in configuration file. Using default: "
              + subscribeToErrors);
    }
    return subscribeToErrors;
  }

  /**
   * Gets the Cumulocity data processing mode setting.
   *
   * @return Cumulocity data processing mode setting
   */
  public CConnectorDataProcessingMode getCumulocityDataProcessingMode() {
    CConnectorDataProcessingMode dataProcessingMode = DATA_PROCESSING_MODE_DEFAULT;
    try {
      if (configurationObject
          .getJSONObject(CONFIG_FILE_CUMULOCITY_KEY)
          .has(CONFIG_FILE_DATA_PROCESSING_MODE_KEY)) {
        dataProcessingMode =
            CConnectorDataProcessingMode.fromValue(
                configurationObject
                    .getJSONObject(CONFIG_FILE_CUMULOCITY_KEY)
                    .getString(CONFIG_FILE_DATA_PROCESSING_MODE_KEY));
      } else {
        final String logKey = CUMULOCITY_DATA_PROCESSING_MODE_CONFIG_NAME;
        final String logMessage =
            CONFIG_FILE_DATA_PROCESSING_MODE_KEY
                + " not found in configuration file. Using default: "
                + dataProcessingMode;
        Logger.LOG_DEBUG_ONCE(logKey, logMessage);
      }
    } catch (JSONException e) {
      Logger.LOG_DEBUG(
          "An error occurred while accessing the "
              + CONFIG_FILE_DATA_PROCESSING_MODE_KEY
              + " field in the configuration file. Using default: "
              + dataProcessingMode);
    }
    return dataProcessingMode;
  }

  /**
   * Gets the Cumulocity parent device aggregated payload type setting.
   *
   * @return Cumulocity parent device aggregated payload type setting
   */
  public String getCumulocityParentDeviceAggregatedPayloadType() {
    String parentDeviceAggregatedPayloadType = PARENT_DEVICE_AGGREGATED_PAYLOAD_TYPE_DEFAULT;
    try {
      if (configurationObject
          .getJSONObject(CONFIG_FILE_CUMULOCITY_KEY)
          .has(CONFIG_FILE_PARENT_DEVICE_AGGREGATED_PAYLOAD_TYPE_KEY)) {
        parentDeviceAggregatedPayloadType =
            configurationObject
                .getJSONObject(CONFIG_FILE_CUMULOCITY_KEY)
                .getString(CONFIG_FILE_PARENT_DEVICE_AGGREGATED_PAYLOAD_TYPE_KEY);
      } else {
        final String logKey = CUMULOCITY_PARENT_DEVICE_AGGREGATED_PAYLOAD_TYPE_CONFIG_NAME;
        final String logMessage =
            CONFIG_FILE_PARENT_DEVICE_AGGREGATED_PAYLOAD_TYPE_KEY
                + " not found in configuration file. Using default: "
                + parentDeviceAggregatedPayloadType;
        Logger.LOG_DEBUG_ONCE(logKey, logMessage);
      }
    } catch (JSONException e) {
      Logger.LOG_DEBUG(
          "An error occurred while accessing the "
              + CONFIG_FILE_PARENT_DEVICE_AGGREGATED_PAYLOAD_TYPE_KEY
              + " field in the configuration file. Using default: "
              + parentDeviceAggregatedPayloadType);
    }
    return parentDeviceAggregatedPayloadType;
  }

  /**
   * Gets the Cumulocity port setting.
   *
   * @return Cumulocity port setting
   */
  public String getCumulocityPort() {
    String port = PORT_DEFAULT;
    try {
      port =
          configurationObject
              .getJSONObject(CONFIG_FILE_CUMULOCITY_KEY)
              .getString(CONFIG_FILE_PORT_KEY);

      // Test that port can be parsed as integer
      int tryParsePort = Integer.parseInt(port);
    } catch (JSONException e) {
      Logger.LOG_DEBUG(
          CONFIG_FILE_PORT_KEY + " not found in configuration file. Using default: " + port);
    } catch (NumberFormatException e) {
      port = PORT_DEFAULT;
      Logger.LOG_DEBUG(
          CONFIG_FILE_PORT_KEY
              + " does not contain a valid port number. The port must be a valid number between 0 "
              + "and 65535, enclosed in a string. Using default: "
              + port);
    }
    return port;
  }

  /**
   * Sets the configured Cumulocity port.
   *
   * @param port Cumulocity port
   * @throws JSONException if unable to set Cumulocity port in configuration
   */
  public void setCumulocityPort(String port) throws JSONException {
    configurationObject.getJSONObject(CONFIG_FILE_CUMULOCITY_KEY).put(CONFIG_FILE_PORT_KEY, port);
    trySave();
  }

  /**
   * Get the configured connector log level from the configuration.
   *
   * @return connector log level
   * @throws JSONException if unable to get connector log level from configuration
   */
  public int getConnectorLogLevel() throws JSONException {
    return configurationObject
        .getJSONObject(CONFIG_FILE_CONNECTOR_KEY)
        .getInt(CONFIG_FILE_LOG_LEVEL_KEY);
  }

  /**
   * Get the configuration value for UTF-8 String support, this is required for supporting non-ASCII
   * characters. When users leave out the JSON key, this function will return false.
   *
   * @return boolean for support for UTF-8 Strings
   */
  public boolean getStringUtf8Support() {
    boolean isUtf8Supported = false;
    try {
      isUtf8Supported =
          configurationObject
              .getJSONObject(CONFIG_FILE_CONNECTOR_KEY)
              .getBoolean(CONFIG_FILE_UTF8_STRING_SUPPORT_KEY);
    } catch (JSONException e) {
      Logger.LOG_DEBUG(CONFIG_FILE_UTF8_STRING_SUPPORT_KEY + " not found in configuration file.");
    }
    return isUtf8Supported;
  }

  /**
   * Get the queue data string enabled setting from the configuration.
   *
   * @return queue data string enabled setting
   * @throws JSONException if unable to get queue data string enabled setting from configuration
   */
  public boolean getQueueDataStringEnabled() throws JSONException {
    boolean queueDataStringEnabled;
    if (configurationObject
        .getJSONObject(CONFIG_FILE_CONNECTOR_KEY)
        .has(CONFIG_FILE_QUEUE_STRING_HISTORY_KEY)) {
      queueDataStringEnabled =
          configurationObject
              .getJSONObject(CONFIG_FILE_CONNECTOR_KEY)
              .getBoolean(CONFIG_FILE_QUEUE_STRING_HISTORY_KEY);
    } else {
      String defaultStringEnabledStr = String.valueOf(QUEUE_DATA_STRING_HISTORY_ENABLED_DEFAULT);
      Logger.LOG_WARN(
          "The queue data string enabled setting was not set. Using default value of "
              + defaultStringEnabledStr
              + ".");
      queueDataStringEnabled = QUEUE_DATA_STRING_HISTORY_ENABLED_DEFAULT;
    }

    return queueDataStringEnabled;
  }

  /**
   * Get the queue data poll size in minutes from the configuration.
   *
   * @return queue data poll size in minutes
   * @throws JSONException if unable to get queue data string enabled setting from configuration
   */
  public long getQueueDataPollSizeMinutes() throws JSONException {
    long queueDataPollSizeMinutes;
    if (configurationObject
        .getJSONObject(CONFIG_FILE_CONNECTOR_KEY)
        .has(CONFIG_FILE_QUEUE_DATA_POLL_SIZE_MINS_KEY)) {
      queueDataPollSizeMinutes =
          configurationObject
              .getJSONObject(CONFIG_FILE_CONNECTOR_KEY)
              .getLong(CONFIG_FILE_QUEUE_DATA_POLL_SIZE_MINS_KEY);
    } else {
      String defaultPollSizeStr = String.valueOf(QUEUE_DATA_POLL_SIZE_MINS_DEFAULT);
      Logger.LOG_WARN(
          "The queue data poll size setting was not set. Using default value of "
              + defaultPollSizeStr
              + " minutes.");
      queueDataPollSizeMinutes = QUEUE_DATA_POLL_SIZE_MINS_DEFAULT;
    }

    return queueDataPollSizeMinutes;
  }

  /**
   * Get the queue maximum data polling run behind time in minutes setting from the configuration.
   * The value of {@link
   * com.hms_networks.americas.sc.extensions.historicaldata.HistoricalDataQueueManager#DISABLED_MAX_HIST_FIFO_GET_BEHIND_MINS}
   * indicates that the functionality is disabled.
   *
   * @return queue maximum data polling run behind time in minutes
   * @throws JSONException if unable to get queue maximum data polling run behind time in minutes
   *     setting from configuration
   */
  public long getQueueDataPollMaxBehindTimeMinutes() throws JSONException {
    long queueDataPollMaxBehindTimeMinutes;
    if (configurationObject
        .getJSONObject(CONFIG_FILE_CONNECTOR_KEY)
        .has(CONFIG_FILE_QUEUE_DATA_POLL_MAX_BEHIND_TIME_MINS_KEY)) {
      queueDataPollMaxBehindTimeMinutes =
          configurationObject
              .getJSONObject(CONFIG_FILE_CONNECTOR_KEY)
              .getLong(CONFIG_FILE_QUEUE_DATA_POLL_MAX_BEHIND_TIME_MINS_KEY);
    } else {
      String defaultQueueDataPollMaxBehindTimeMinsStr =
          String.valueOf(QUEUE_DATA_POLL_MAX_BEHIND_TIME_MINS_DEFAULT);
      Logger.LOG_WARN(
          "The queue maximum data polling run behind time setting was not set. "
              + "Using default value of "
              + defaultQueueDataPollMaxBehindTimeMinsStr
              + " minutes.");
      queueDataPollMaxBehindTimeMinutes = QUEUE_DATA_POLL_MAX_BEHIND_TIME_MINS_DEFAULT;
    }

    return queueDataPollMaxBehindTimeMinutes;
  }

  /**
   * Get the queue data poll interval in milliseconds from the configuration.
   *
   * @return queue data poll interval in milliseconds
   * @throws JSONException if unable to get queue data poll interval from the configuration file
   */
  public long getQueueDataPollIntervalMillis() throws JSONException {
    long queueDataPollIntervalMillis;
    if (configurationObject
        .getJSONObject(CONFIG_FILE_CONNECTOR_KEY)
        .has(CONFIG_FILE_QUEUE_DATA_POLL_INTERVAL_MILLIS_KEY)) {
      queueDataPollIntervalMillis =
          configurationObject
              .getJSONObject(CONFIG_FILE_CONNECTOR_KEY)
              .getLong(CONFIG_FILE_QUEUE_DATA_POLL_INTERVAL_MILLIS_KEY);
    } else {
      String defaultPollInterval = String.valueOf(QUEUE_DATA_POLL_INTERVAL_MILLIS_DEFAULT);
      Logger.LOG_WARN(
          "The queue data poll interval setting was not set. Using default value of "
              + defaultPollInterval
              + " milliseconds.");
      queueDataPollIntervalMillis = QUEUE_DATA_POLL_INTERVAL_MILLIS_DEFAULT;
    }

    return queueDataPollIntervalMillis;
  }

  /**
   * Get the queue diagnostic tags enabled setting from the configuration.
   *
   * @return queue diagnostic tags enabled setting
   */
  public boolean getQueueDiagnosticTagsEnabled() {
    boolean queueDiagnosticTagsEnabled;
    try {
      if (configurationObject
          .getJSONObject(CONFIG_FILE_CONNECTOR_KEY)
          .has(CONFIG_FILE_ENABLE_QUEUE_DIAGNOSTIC_TAGS_KEY)) {
        queueDiagnosticTagsEnabled =
            configurationObject
                .getJSONObject(CONFIG_FILE_CONNECTOR_KEY)
                .getBoolean(CONFIG_FILE_ENABLE_QUEUE_DIAGNOSTIC_TAGS_KEY);
      } else {
        queueDiagnosticTagsEnabled = ENABLE_QUEUE_DIAGNOSTIC_TAGS_DEFAULT;
      }
    } catch (JSONException e) {
      queueDiagnosticTagsEnabled = ENABLE_QUEUE_DIAGNOSTIC_TAGS_DEFAULT;
      Logger.LOG_WARN(
          "The queue diagnostic tags enabled setting could not be read from the configuration"
              + " file. Using default value of "
              + ENABLE_QUEUE_DIAGNOSTIC_TAGS_DEFAULT
              + ".");
      Logger.LOG_EXCEPTION(e);
    }

    return queueDiagnosticTagsEnabled;
  }

  /**
   * Get the historical data queue aggregation period (in seconds) from the configuration.
   *
   * @return queue data queue aggregation period (in seconds)
   */
  public long getQueueDataAggregationPeriodSecs() {
    long queueDataAggregationPeriodSecs;
    try {
      if (configurationObject
          .getJSONObject(CONFIG_FILE_CONNECTOR_KEY)
          .has(CONFIG_FILE_QUEUE_DATA_AGGREGATION_PERIOD_SECS_KEY)) {
        queueDataAggregationPeriodSecs =
            configurationObject
                .getJSONObject(CONFIG_FILE_CONNECTOR_KEY)
                .getLong(CONFIG_FILE_QUEUE_DATA_AGGREGATION_PERIOD_SECS_KEY);
      } else {
        // Use default and add to configuration file
        queueDataAggregationPeriodSecs = CONFIG_FILE_QUEUE_DATA_AGGREGATION_PERIOD_SECS_DEFAULT;
        configurationObject
            .getJSONObject(CONFIG_FILE_CONNECTOR_KEY)
            .put(
                CONFIG_FILE_QUEUE_DATA_AGGREGATION_PERIOD_SECS_KEY,
                CONFIG_FILE_QUEUE_DATA_AGGREGATION_PERIOD_SECS_DEFAULT);
        trySave();
      }
    } catch (JSONException e) {
      queueDataAggregationPeriodSecs = CONFIG_FILE_QUEUE_DATA_AGGREGATION_PERIOD_SECS_DEFAULT;
      Logger.LOG_WARN(
          "The queue data aggregation period setting could not be read from the configuration"
              + " file. Using default value of "
              + CONFIG_FILE_QUEUE_DATA_AGGREGATION_PERIOD_SECS_DEFAULT
              + ".",
          e);
    }

    return queueDataAggregationPeriodSecs;
  }

  /**
   * Get the historical data queue aggregation method from the configuration.
   *
   * @return queue data queue aggregation method
   */
  public CConnectorAggregationMethod getQueueDataAggregationMethod() {
    CConnectorAggregationMethod queueDataAggregationMethod;
    try {
      if (configurationObject
          .getJSONObject(CONFIG_FILE_CONNECTOR_KEY)
          .has(CONFIG_FILE_QUEUE_DATA_AGGREGATION_METHOD_KEY)) {
        queueDataAggregationMethod =
            CConnectorAggregationMethod.fromValue(
                configurationObject
                    .getJSONObject(CONFIG_FILE_CONNECTOR_KEY)
                    .getInt(CONFIG_FILE_QUEUE_DATA_AGGREGATION_METHOD_KEY));
      } else {
        // Use default and add to configuration file
        queueDataAggregationMethod = CONFIG_FILE_QUEUE_DATA_AGGREGATION_METHOD_DEFAULT;
        configurationObject
            .getJSONObject(CONFIG_FILE_CONNECTOR_KEY)
            .put(
                CONFIG_FILE_QUEUE_DATA_AGGREGATION_METHOD_KEY,
                CONFIG_FILE_QUEUE_DATA_AGGREGATION_METHOD_DEFAULT.getValue());
        trySave();
      }
    } catch (JSONException e) {
      queueDataAggregationMethod = CONFIG_FILE_QUEUE_DATA_AGGREGATION_METHOD_DEFAULT;
      Logger.LOG_WARN(
          "The queue data aggregation method setting could not be read from the configuration"
              + " file. Using default value of "
              + CONFIG_FILE_QUEUE_DATA_AGGREGATION_METHOD_DEFAULT.getValue()
              + ".",
          e);
    }

    return queueDataAggregationMethod;
  }

  /**
   * Saves the configuration to the file system and catches any exceptions generated while saving.
   */
  void trySave() {
    try {
      save();
      Logger.LOG_DEBUG("Saved application configuration changes to file.");
    } catch (Exception e) {
      Logger.LOG_SERIOUS("Unable to save application configuration to file.");
      Logger.LOG_EXCEPTION(e);
    }
  }

  /**
   * Parses an escaped configuration file string that is received from Cumulocity, and apply any
   * changes to matching fields.
   *
   * @param configFileEscapedString escaped configuration file string contents from Cumulocity
   * @throws JSONException if unable to update the configuration file
   */
  public void parseConfigFileEscapedString(String configFileEscapedString) throws JSONException {
    String configFileEscapedStringNoQuotes =
        configFileEscapedString.substring(1, configFileEscapedString.length() - 1);
    List configFileEscapedStringLines = StringUtils.split(configFileEscapedStringNoQuotes, "\n");

    // Loop through each line of the config file string (skip last blank line)
    for (int i = 0; i < configFileEscapedStringLines.size() - 1; i++) {
      Logger.LOG_INFO(
          "Updating configuration field from Cumulocity: ["
              + configFileEscapedStringLines.get(i)
              + "]");
      List configFileEscapedStringLineSplit =
          StringUtils.split((String) configFileEscapedStringLines.get(i), "=");
      String configFileEscapedStringLineKey = (String) configFileEscapedStringLineSplit.get(0);
      String configFileEscapedStringLineValue = (String) configFileEscapedStringLineSplit.get(1);
      if (configFileEscapedStringLineKey.equals(CONNECTOR_LOG_LEVEL_CONFIG_NAME)) {
        configurationObject
            .getJSONObject(CONFIG_FILE_CONNECTOR_KEY)
            .put(CONFIG_FILE_LOG_LEVEL_KEY, Integer.parseInt(configFileEscapedStringLineValue));
      } else if (configFileEscapedStringLineKey.equals(CONNECTOR_UTF8_STRING_SUPPORT_CONFIG_NAME)) {
        configurationObject
            .getJSONObject(CONFIG_FILE_CONNECTOR_KEY)
            .put(
                CONFIG_FILE_UTF8_STRING_SUPPORT_KEY,
                configFileEscapedStringLineValue.equals("true"));
      } else if (configFileEscapedStringLineKey.equals(
          CONNECTOR_QUEUE_DATA_POLL_INTERVAL_MILLIS_CONFIG_NAME)) {
        configurationObject
            .getJSONObject(CONFIG_FILE_CONNECTOR_KEY)
            .put(
                CONFIG_FILE_QUEUE_DATA_POLL_INTERVAL_MILLIS_KEY,
                Long.parseLong(configFileEscapedStringLineValue));
      } else if (configFileEscapedStringLineKey.equals(
          CONNECTOR_QUEUE_DATA_POLL_SIZE_MINS_CONFIG_NAME)) {
        configurationObject
            .getJSONObject(CONFIG_FILE_CONNECTOR_KEY)
            .put(
                CONFIG_FILE_QUEUE_DATA_POLL_SIZE_MINS_KEY,
                Long.parseLong(configFileEscapedStringLineValue));
      } else if (configFileEscapedStringLineKey.equals(
          CONNECTOR_QUEUE_DATA_POLL_MAX_BEHIND_TIME_MINS_CONFIG_NAME)) {
        configurationObject
            .getJSONObject(CONFIG_FILE_CONNECTOR_KEY)
            .put(
                CONFIG_FILE_QUEUE_DATA_POLL_MAX_BEHIND_TIME_MINS_KEY,
                Long.parseLong(configFileEscapedStringLineValue));
      } else if (configFileEscapedStringLineKey.equals(
          CONNECTOR_QUEUE_ENABLE_DIAGNOSTIC_TAGS_CONFIG_NAME)) {
        configurationObject
            .getJSONObject(CONFIG_FILE_CONNECTOR_KEY)
            .put(
                CONFIG_FILE_ENABLE_QUEUE_DIAGNOSTIC_TAGS_KEY,
                configFileEscapedStringLineValue.equals("true"));
      } else if (configFileEscapedStringLineKey.equals(
          CONNECTOR_QUEUE_ENABLE_STRING_HISTORY_CONFIG_NAME)) {
        configurationObject
            .getJSONObject(CONFIG_FILE_CONNECTOR_KEY)
            .put(
                CONFIG_FILE_QUEUE_STRING_HISTORY_KEY,
                configFileEscapedStringLineValue.equals("true"));
      } else if (configFileEscapedStringLineKey.equals(CUMULOCITY_BOOTSTRAP_USERNAME_CONFIG_NAME)) {
        configurationObject
            .getJSONObject(CONFIG_FILE_CUMULOCITY_KEY)
            .put(CONFIG_FILE_BOOTSTRAP_USERNAME_KEY, configFileEscapedStringLineValue);
      } else if (configFileEscapedStringLineKey.equals(CUMULOCITY_BOOTSTRAP_PASSWORD_CONFIG_NAME)
          && !configFileEscapedStringLineValue.equals(CONFIG_FILE_PASSWORD_MASKED_VALUE)) {
        configurationObject
            .getJSONObject(CONFIG_FILE_CUMULOCITY_KEY)
            .put(CONFIG_FILE_BOOTSTRAP_PASSWORD_KEY, configFileEscapedStringLineValue);
      } else if (configFileEscapedStringLineKey.equals(CUMULOCITY_BOOTSTRAP_TENANT_CONFIG_NAME)) {
        configurationObject
            .getJSONObject(CONFIG_FILE_CUMULOCITY_KEY)
            .put(CONFIG_FILE_BOOTSTRAP_TENANT_KEY, configFileEscapedStringLineValue);
      } else if (configFileEscapedStringLineKey.equals(CUMULOCITY_DEVICE_USERNAME_CONFIG_NAME)) {
        configurationObject
            .getJSONObject(CONFIG_FILE_CUMULOCITY_KEY)
            .put(CONFIG_FILE_DEVICE_USERNAME_KEY, configFileEscapedStringLineValue);
      } else if (configFileEscapedStringLineKey.equals(CUMULOCITY_DEVICE_PASSWORD_CONFIG_NAME)
          && !configFileEscapedStringLineValue.equals(CONFIG_FILE_PASSWORD_MASKED_VALUE)) {
        configurationObject
            .getJSONObject(CONFIG_FILE_CUMULOCITY_KEY)
            .put(CONFIG_FILE_DEVICE_PASSWORD_KEY, configFileEscapedStringLineValue);
      } else if (configFileEscapedStringLineKey.equals(CUMULOCITY_DEVICE_TENANT_CONFIG_NAME)) {
        configurationObject
            .getJSONObject(CONFIG_FILE_CUMULOCITY_KEY)
            .put(CONFIG_FILE_DEVICE_TENANT_KEY, configFileEscapedStringLineValue);
      } else if (configFileEscapedStringLineKey.equals(CUMULOCITY_HOST_CONFIG_NAME)) {
        configurationObject
            .getJSONObject(CONFIG_FILE_CUMULOCITY_KEY)
            .put(CONFIG_FILE_HOST_KEY, configFileEscapedStringLineValue);
      } else if (configFileEscapedStringLineKey.equals(
          CUMULOCITY_CUSTOM_CERTIFICATE_URL_CONFIG_NAME)) {
        configurationObject
            .getJSONObject(CONFIG_FILE_CUMULOCITY_KEY)
            .put(CONFIG_FILE_CUSTOM_CERTIFICATE_URL_KEY, configFileEscapedStringLineValue);
      } else if (configFileEscapedStringLineKey.equals(
          CUMULOCITY_CUSTOM_CERTIFICATE_URL_ENABLED_CONFIG_NAME)) {
        configurationObject
            .getJSONObject(CONFIG_FILE_CUMULOCITY_KEY)
            .put(
                CONFIG_FILE_CUSTOM_CERTIFICATE_URL_ENABLED_KEY,
                configFileEscapedStringLineValue.equals("true"));
      } else if (configFileEscapedStringLineKey.equals(CUMULOCITY_PORT_CONFIG_NAME)) {
        configurationObject
            .getJSONObject(CONFIG_FILE_CUMULOCITY_KEY)
            .put(CONFIG_FILE_PORT_KEY, configFileEscapedStringLineValue);
      } else if (configFileEscapedStringLineKey.equals(
          CUMULOCITY_SUBSCRIBE_TO_ERRORS_CONFIG_NAME)) {
        configurationObject
            .getJSONObject(CONFIG_FILE_CUMULOCITY_KEY)
            .put(
                CONFIG_FILE_SUBSCRIBE_TO_ERRORS_KEY,
                configFileEscapedStringLineValue.equals("true"));
      } else if (configFileEscapedStringLineKey.equals(
          CUMULOCITY_DATA_PROCESSING_MODE_CONFIG_NAME)) {
        configurationObject
            .getJSONObject(CONFIG_FILE_CUMULOCITY_KEY)
            .put(CONFIG_FILE_DATA_PROCESSING_MODE_KEY, configFileEscapedStringLineValue);
      } else if (configFileEscapedStringLineKey.equals(
          CUMULOCITY_PARENT_DEVICE_AGGREGATED_PAYLOAD_TYPE_CONFIG_NAME)) {
        configurationObject
            .getJSONObject(CONFIG_FILE_CUMULOCITY_KEY)
            .put(
                CONFIG_FILE_PARENT_DEVICE_AGGREGATED_PAYLOAD_TYPE_KEY,
                configFileEscapedStringLineValue);
      } else if (configFileEscapedStringLineKey.equals(
          CONNECTOR_QUEUE_DATA_AGGREGATION_PERIOD_SECS_CONFIG_NAME)) {
        configurationObject
            .getJSONObject(CONFIG_FILE_CONNECTOR_KEY)
            .put(
                CONFIG_FILE_QUEUE_DATA_AGGREGATION_PERIOD_SECS_KEY,
                Long.parseLong(configFileEscapedStringLineValue));
      } else if (configFileEscapedStringLineKey.equals(
          CONNECTOR_QUEUE_DATA_AGGREGATION_METHOD_CONFIG_NAME)) {
        configurationObject
            .getJSONObject(CONFIG_FILE_CONNECTOR_KEY)
            .put(
                CONFIG_FILE_QUEUE_DATA_AGGREGATION_METHOD_KEY,
                Integer.parseInt(configFileEscapedStringLineValue));
      }
    }
    trySave();
  }

  /**
   * Gets the configuration file as a string, with password values masked if the <code>maskPasswords
   * </code> parameter is set to true.
   *
   * @param maskPasswords boolean indicating whether to mask passwords in the configuration file
   *     string
   * @return configuration file as a string, with password values masked if the <code>maskPasswords
   *     </code> parameter is set to true
   * @throws JSONException if unable to parse the configuration file or output the configuration
   *     file as a string
   */
  public String getConfigFileEscapedString(boolean maskPasswords) throws JSONException {
    // Create buffer for building the configuration file string
    StringBuffer configFileEscapedString = new StringBuffer();

    // Add Connector/LogLevel
    if (configurationObject
        .getJSONObject(CONFIG_FILE_CONNECTOR_KEY)
        .has(CONFIG_FILE_LOG_LEVEL_KEY)) {
      configFileEscapedString.append(CONNECTOR_LOG_LEVEL_CONFIG_NAME);
      configFileEscapedString.append("=");
      configFileEscapedString.append(
          configurationObject
              .getJSONObject(CONFIG_FILE_CONNECTOR_KEY)
              .getInt(CONFIG_FILE_LOG_LEVEL_KEY));
      configFileEscapedString.append("\n");
    }

    // Add Connector/UTF8StringSupport
    if (configurationObject
        .getJSONObject(CONFIG_FILE_CONNECTOR_KEY)
        .has(CONFIG_FILE_UTF8_STRING_SUPPORT_KEY)) {
      configFileEscapedString.append(CONNECTOR_UTF8_STRING_SUPPORT_CONFIG_NAME);
      configFileEscapedString.append("=");
      configFileEscapedString.append(
          configurationObject
              .getJSONObject(CONFIG_FILE_CONNECTOR_KEY)
              .getBoolean(CONFIG_FILE_UTF8_STRING_SUPPORT_KEY));
      configFileEscapedString.append("\n");
    }

    // Add Connector/QueueDataPollIntervalMillis
    if (configurationObject
        .getJSONObject(CONFIG_FILE_CONNECTOR_KEY)
        .has(CONFIG_FILE_QUEUE_DATA_POLL_INTERVAL_MILLIS_KEY)) {
      configFileEscapedString.append(CONNECTOR_QUEUE_DATA_POLL_INTERVAL_MILLIS_CONFIG_NAME);
      configFileEscapedString.append("=");
      configFileEscapedString.append(
          configurationObject
              .getJSONObject(CONFIG_FILE_CONNECTOR_KEY)
              .getInt(CONFIG_FILE_QUEUE_DATA_POLL_INTERVAL_MILLIS_KEY));
      configFileEscapedString.append("\n");
    }

    // Add Connector/QueueDataPollSizeMins
    if (configurationObject
        .getJSONObject(CONFIG_FILE_CONNECTOR_KEY)
        .has(CONFIG_FILE_QUEUE_DATA_POLL_SIZE_MINS_KEY)) {
      configFileEscapedString.append(CONNECTOR_QUEUE_DATA_POLL_SIZE_MINS_CONFIG_NAME);
      configFileEscapedString.append("=");
      configFileEscapedString.append(
          configurationObject
              .getJSONObject(CONFIG_FILE_CONNECTOR_KEY)
              .getLong(CONFIG_FILE_QUEUE_DATA_POLL_SIZE_MINS_KEY));
      configFileEscapedString.append("\n");
    }

    // Add Connector/QueueDataPollMaxBehindTimeMins
    if (configurationObject
        .getJSONObject(CONFIG_FILE_CONNECTOR_KEY)
        .has(CONFIG_FILE_QUEUE_DATA_POLL_MAX_BEHIND_TIME_MINS_KEY)) {
      configFileEscapedString.append(CONNECTOR_QUEUE_DATA_POLL_MAX_BEHIND_TIME_MINS_CONFIG_NAME);
      configFileEscapedString.append("=");
      configFileEscapedString.append(
          configurationObject
              .getJSONObject(CONFIG_FILE_CONNECTOR_KEY)
              .getLong(CONFIG_FILE_QUEUE_DATA_POLL_MAX_BEHIND_TIME_MINS_KEY));
      configFileEscapedString.append("\n");
    }

    // Add Connector/QueueEnableDiagnosticTags
    if (configurationObject
        .getJSONObject(CONFIG_FILE_CONNECTOR_KEY)
        .has(CONFIG_FILE_ENABLE_QUEUE_DIAGNOSTIC_TAGS_KEY)) {
      configFileEscapedString.append(CONNECTOR_QUEUE_ENABLE_DIAGNOSTIC_TAGS_CONFIG_NAME);
      configFileEscapedString.append("=");
      configFileEscapedString.append(
          configurationObject
              .getJSONObject(CONFIG_FILE_CONNECTOR_KEY)
              .getBoolean(CONFIG_FILE_ENABLE_QUEUE_DIAGNOSTIC_TAGS_KEY));
      configFileEscapedString.append("\n");
    }

    // Add Connector/QueueDataAggregationPeriodSecs
    if (configurationObject
        .getJSONObject(CONFIG_FILE_CONNECTOR_KEY)
        .has(CONFIG_FILE_QUEUE_DATA_AGGREGATION_PERIOD_SECS_KEY)) {
      configFileEscapedString.append(CONNECTOR_QUEUE_DATA_AGGREGATION_PERIOD_SECS_CONFIG_NAME);
      configFileEscapedString.append("=");
      configFileEscapedString.append(
          configurationObject
              .getJSONObject(CONFIG_FILE_CONNECTOR_KEY)
              .getLong(CONFIG_FILE_QUEUE_DATA_AGGREGATION_PERIOD_SECS_KEY));
      configFileEscapedString.append("\n");
    }

    // Add Connector/QueueDataAggregationMethod
    if (configurationObject
        .getJSONObject(CONFIG_FILE_CONNECTOR_KEY)
        .has(CONFIG_FILE_QUEUE_DATA_AGGREGATION_METHOD_KEY)) {
      configFileEscapedString.append(CONNECTOR_QUEUE_DATA_AGGREGATION_METHOD_CONFIG_NAME);
      configFileEscapedString.append("=");
      configFileEscapedString.append(
          configurationObject
              .getJSONObject(CONFIG_FILE_CONNECTOR_KEY)
              .getInt(CONFIG_FILE_QUEUE_DATA_AGGREGATION_METHOD_KEY));
      configFileEscapedString.append("\n");
    }

    // Add Connector/QueueEnableStringHistory
    if (configurationObject
        .getJSONObject(CONFIG_FILE_CONNECTOR_KEY)
        .has(CONFIG_FILE_QUEUE_STRING_HISTORY_KEY)) {
      configFileEscapedString.append(CONNECTOR_QUEUE_ENABLE_STRING_HISTORY_CONFIG_NAME);
      configFileEscapedString.append("=");
      configFileEscapedString.append(
          configurationObject
              .getJSONObject(CONFIG_FILE_CONNECTOR_KEY)
              .getBoolean(CONFIG_FILE_QUEUE_STRING_HISTORY_KEY));
      configFileEscapedString.append("\n");
    }

    // Add Cumulocity/BootstrapUsername
    if (configurationObject
        .getJSONObject(CONFIG_FILE_CUMULOCITY_KEY)
        .has(CONFIG_FILE_BOOTSTRAP_USERNAME_KEY)) {
      configFileEscapedString.append(CUMULOCITY_BOOTSTRAP_USERNAME_CONFIG_NAME);
      configFileEscapedString.append("=");
      configFileEscapedString.append(
          configurationObject
              .getJSONObject(CONFIG_FILE_CUMULOCITY_KEY)
              .getString(CONFIG_FILE_BOOTSTRAP_USERNAME_KEY));
      configFileEscapedString.append("\n");
    }

    // Add Cumulocity/BootstrapPassword
    if (configurationObject
        .getJSONObject(CONFIG_FILE_CUMULOCITY_KEY)
        .has(CONFIG_FILE_BOOTSTRAP_PASSWORD_KEY)) {
      configFileEscapedString.append(CUMULOCITY_BOOTSTRAP_PASSWORD_CONFIG_NAME);
      configFileEscapedString.append("=");
      if (maskPasswords) {
        configFileEscapedString.append(CONFIG_FILE_PASSWORD_MASKED_VALUE);
      } else {
        configFileEscapedString.append(
            configurationObject
                .getJSONObject(CONFIG_FILE_CUMULOCITY_KEY)
                .getString(CONFIG_FILE_BOOTSTRAP_PASSWORD_KEY));
      }
      configFileEscapedString.append("\n");
    }

    // Add Cumulocity/BootstrapTenant
    if (configurationObject
        .getJSONObject(CONFIG_FILE_CUMULOCITY_KEY)
        .has(CONFIG_FILE_BOOTSTRAP_TENANT_KEY)) {
      configFileEscapedString.append(CUMULOCITY_BOOTSTRAP_TENANT_CONFIG_NAME);
      configFileEscapedString.append("=");
      configFileEscapedString.append(
          configurationObject
              .getJSONObject(CONFIG_FILE_CUMULOCITY_KEY)
              .getString(CONFIG_FILE_BOOTSTRAP_TENANT_KEY));
      configFileEscapedString.append("\n");
    }

    // Add Cumulocity/DeviceUsername
    if (configurationObject
        .getJSONObject(CONFIG_FILE_CUMULOCITY_KEY)
        .has(CONFIG_FILE_DEVICE_USERNAME_KEY)) {
      configFileEscapedString.append(CUMULOCITY_DEVICE_USERNAME_CONFIG_NAME);
      configFileEscapedString.append("=");
      configFileEscapedString.append(
          configurationObject
              .getJSONObject(CONFIG_FILE_CUMULOCITY_KEY)
              .getString(CONFIG_FILE_DEVICE_USERNAME_KEY));
      configFileEscapedString.append("\n");
    }

    // Add Cumulocity/DevicePassword
    if (configurationObject
        .getJSONObject(CONFIG_FILE_CUMULOCITY_KEY)
        .has(CONFIG_FILE_DEVICE_PASSWORD_KEY)) {
      configFileEscapedString.append(CUMULOCITY_DEVICE_PASSWORD_CONFIG_NAME);
      configFileEscapedString.append("=");
      if (maskPasswords) {
        configFileEscapedString.append(CONFIG_FILE_PASSWORD_MASKED_VALUE);
      } else {
        configFileEscapedString.append(
            configurationObject
                .getJSONObject(CONFIG_FILE_CUMULOCITY_KEY)
                .getString(CONFIG_FILE_DEVICE_PASSWORD_KEY));
      }
      configFileEscapedString.append("\n");
    }

    // Add Cumulocity/DeviceTenant
    if (configurationObject
        .getJSONObject(CONFIG_FILE_CUMULOCITY_KEY)
        .has(CONFIG_FILE_DEVICE_TENANT_KEY)) {
      configFileEscapedString.append(CUMULOCITY_DEVICE_TENANT_CONFIG_NAME);
      configFileEscapedString.append("=");
      configFileEscapedString.append(
          configurationObject
              .getJSONObject(CONFIG_FILE_CUMULOCITY_KEY)
              .getString(CONFIG_FILE_DEVICE_TENANT_KEY));
      configFileEscapedString.append("\n");
    }

    // Add Cumulocity/Host
    if (configurationObject.getJSONObject(CONFIG_FILE_CUMULOCITY_KEY).has(CONFIG_FILE_HOST_KEY)) {
      configFileEscapedString.append(CUMULOCITY_HOST_CONFIG_NAME);
      configFileEscapedString.append("=");
      configFileEscapedString.append(
          configurationObject
              .getJSONObject(CONFIG_FILE_CUMULOCITY_KEY)
              .getString(CONFIG_FILE_HOST_KEY));
      configFileEscapedString.append("\n");
    }

    // Add Cumulocity/CustomCertificateUrl
    if (configurationObject
        .getJSONObject(CONFIG_FILE_CUMULOCITY_KEY)
        .has(CONFIG_FILE_CUSTOM_CERTIFICATE_URL_KEY)) {
      configFileEscapedString.append(CUMULOCITY_CUSTOM_CERTIFICATE_URL_CONFIG_NAME);
      configFileEscapedString.append("=");
      configFileEscapedString.append(
          configurationObject
              .getJSONObject(CONFIG_FILE_CUMULOCITY_KEY)
              .getString(CONFIG_FILE_CUSTOM_CERTIFICATE_URL_KEY));
      configFileEscapedString.append("\n");
    }

    // Add Cumulocity/CustomCertificateUrlEnabled
    if (configurationObject
        .getJSONObject(CONFIG_FILE_CUMULOCITY_KEY)
        .has(CONFIG_FILE_CUSTOM_CERTIFICATE_URL_ENABLED_KEY)) {
      configFileEscapedString.append(CUMULOCITY_CUSTOM_CERTIFICATE_URL_ENABLED_CONFIG_NAME);
      configFileEscapedString.append("=");
      configFileEscapedString.append(
          configurationObject
              .getJSONObject(CONFIG_FILE_CUMULOCITY_KEY)
              .getString(CONFIG_FILE_CUSTOM_CERTIFICATE_URL_ENABLED_KEY));
      configFileEscapedString.append("\n");
    }

    // Add Cumulocity/Port
    if (configurationObject.getJSONObject(CONFIG_FILE_CUMULOCITY_KEY).has(CONFIG_FILE_PORT_KEY)) {
      configFileEscapedString.append(CUMULOCITY_PORT_CONFIG_NAME);
      configFileEscapedString.append("=");
      configFileEscapedString.append(
          configurationObject
              .getJSONObject(CONFIG_FILE_CUMULOCITY_KEY)
              .getString(CONFIG_FILE_PORT_KEY));
      configFileEscapedString.append("\n");
    }

    // Add Cumulocity/SubscribeToErrors
    if (configurationObject
        .getJSONObject(CONFIG_FILE_CUMULOCITY_KEY)
        .has(CONFIG_FILE_SUBSCRIBE_TO_ERRORS_KEY)) {
      configFileEscapedString.append(CUMULOCITY_SUBSCRIBE_TO_ERRORS_CONFIG_NAME);
      configFileEscapedString.append("=");
      configFileEscapedString.append(
          configurationObject
              .getJSONObject(CONFIG_FILE_CUMULOCITY_KEY)
              .getString(CONFIG_FILE_SUBSCRIBE_TO_ERRORS_KEY));
      configFileEscapedString.append("\n");
    }

    // Add Cumulocity/DataProcessingMode
    if (configurationObject
        .getJSONObject(CONFIG_FILE_CUMULOCITY_KEY)
        .has(CONFIG_FILE_DATA_PROCESSING_MODE_KEY)) {
      configFileEscapedString.append(CUMULOCITY_DATA_PROCESSING_MODE_CONFIG_NAME);
      configFileEscapedString.append("=");
      configFileEscapedString.append(
          configurationObject
              .getJSONObject(CONFIG_FILE_CUMULOCITY_KEY)
              .getString(CONFIG_FILE_DATA_PROCESSING_MODE_KEY));
      configFileEscapedString.append("\n");
    }

    // Add Cumulocity/parentDeviceAggregatedPayloadType
    if (configurationObject
        .getJSONObject(CONFIG_FILE_CUMULOCITY_KEY)
        .has(CONFIG_FILE_PARENT_DEVICE_AGGREGATED_PAYLOAD_TYPE_KEY)) {
      configFileEscapedString.append(CUMULOCITY_PARENT_DEVICE_AGGREGATED_PAYLOAD_TYPE_CONFIG_NAME);
      configFileEscapedString.append("=");
      configFileEscapedString.append(
          configurationObject
              .getJSONObject(CONFIG_FILE_CUMULOCITY_KEY)
              .getString(CONFIG_FILE_PARENT_DEVICE_AGGREGATED_PAYLOAD_TYPE_KEY));
      configFileEscapedString.append("\n");
    }

    return configFileEscapedString.toString();
  }

  /**
   * Gets the file path for the configuration file on the Ewon Flexy's file system.
   *
   * @return file path for the configuration file
   */
  public String getConfigFilePath() {
    return CONFIG_FILE_PATH;
  }

  /**
   * Gets the indent factor for the configuration file JSON contents.
   *
   * @return configuration file JSON indent factor
   */
  public int getJSONIndentFactor() {
    return CONFIG_FILE_JSON_INDENT_FACTOR;
  }

  /**
   * Gets the default configuration object for use when an existing configuration file is not found.
   *
   * @return default configuration object
   * @throws JSONException if unable to build JSON configuration object
   */
  public JSONObject getDefaultConfigurationObject() throws JSONException {
    // Build default connector configuration object
    JSONObject defaultConnectorConfigurationObject = new JSONObject();
    defaultConnectorConfigurationObject.put(CONFIG_FILE_LOG_LEVEL_KEY, Logger.LOG_LEVEL_INFO);
    defaultConnectorConfigurationObject.put(CONFIG_FILE_UTF8_STRING_SUPPORT_KEY, true);
    defaultConnectorConfigurationObject.put(
        CONFIG_FILE_QUEUE_DATA_POLL_SIZE_MINS_KEY, QUEUE_DATA_POLL_SIZE_MINS_DEFAULT);
    defaultConnectorConfigurationObject.put(
        CONFIG_FILE_QUEUE_DATA_POLL_MAX_BEHIND_TIME_MINS_KEY,
        QUEUE_DATA_POLL_MAX_BEHIND_TIME_MINS_DEFAULT);
    defaultConnectorConfigurationObject.put(
        CONFIG_FILE_QUEUE_STRING_HISTORY_KEY, QUEUE_DATA_STRING_HISTORY_ENABLED_DEFAULT);
    defaultConnectorConfigurationObject.put(
        CONFIG_FILE_QUEUE_DATA_POLL_INTERVAL_MILLIS_KEY, QUEUE_DATA_POLL_INTERVAL_MILLIS_DEFAULT);
    defaultConnectorConfigurationObject.put(
        CONFIG_FILE_QUEUE_DATA_AGGREGATION_PERIOD_SECS_KEY,
        CONFIG_FILE_QUEUE_DATA_AGGREGATION_PERIOD_SECS_DEFAULT);
    defaultConnectorConfigurationObject.put(
        CONFIG_FILE_QUEUE_DATA_AGGREGATION_METHOD_KEY,
        CONFIG_FILE_QUEUE_DATA_AGGREGATION_METHOD_DEFAULT.getValue());
    defaultConnectorConfigurationObject.put(
        CONFIG_FILE_ENABLE_QUEUE_DIAGNOSTIC_TAGS_KEY, ENABLE_QUEUE_DIAGNOSTIC_TAGS_DEFAULT);

    // Build default Cumulocity configuration object
    JSONObject defaultCumulocityConfigurationObject = new JSONObject();
    defaultCumulocityConfigurationObject.put(CONFIG_FILE_BOOTSTRAP_TENANT_KEY, "");
    defaultCumulocityConfigurationObject.put(CONFIG_FILE_BOOTSTRAP_USERNAME_KEY, "");
    defaultCumulocityConfigurationObject.put(CONFIG_FILE_BOOTSTRAP_PASSWORD_KEY, "");
    defaultCumulocityConfigurationObject.put(
        CONFIG_FILE_DEVICE_USERNAME_KEY, CONFIG_FILE_AUTOMATICALLY_FILLED_TEXT);
    defaultCumulocityConfigurationObject.put(
        CONFIG_FILE_DEVICE_PASSWORD_KEY, CONFIG_FILE_AUTOMATICALLY_FILLED_TEXT);
    defaultCumulocityConfigurationObject.put(
        CONFIG_FILE_DEVICE_TENANT_KEY, CONFIG_FILE_AUTOMATICALLY_FILLED_TEXT);
    defaultCumulocityConfigurationObject.put(CONFIG_FILE_HOST_KEY, "");
    defaultCumulocityConfigurationObject.put(
        CONFIG_FILE_CUSTOM_CERTIFICATE_URL_KEY, CUSTOM_CERTIFICATE_URL_DEFAULT);
    defaultCumulocityConfigurationObject.put(
        CONFIG_FILE_CUSTOM_CERTIFICATE_URL_ENABLED_KEY, CUSTOM_CERTIFICATE_URL_ENABLED_DEFAULT);
    defaultCumulocityConfigurationObject.put(CONFIG_FILE_PORT_KEY, PORT_DEFAULT);
    defaultCumulocityConfigurationObject.put(
        CONFIG_FILE_SUBSCRIBE_TO_ERRORS_KEY, SUBSCRIBE_TO_ERRORS_DEFAULT);
    defaultCumulocityConfigurationObject.put(
        CONFIG_FILE_DATA_PROCESSING_MODE_KEY, DATA_PROCESSING_MODE_DEFAULT.getValue());
    defaultCumulocityConfigurationObject.put(
        CONFIG_FILE_PARENT_DEVICE_AGGREGATED_PAYLOAD_TYPE_KEY,
        PARENT_DEVICE_AGGREGATED_PAYLOAD_TYPE_DEFAULT);

    // Add default Cumulocity object and Connector object to a root object and return
    JSONObject defaultConfigObject = new JSONObject();
    defaultConfigObject.putOpt(CONFIG_FILE_CONNECTOR_KEY, defaultConnectorConfigurationObject);
    defaultConfigObject.putOpt(CONFIG_FILE_CUMULOCITY_KEY, defaultCumulocityConfigurationObject);
    return defaultConfigObject;
  }
}
