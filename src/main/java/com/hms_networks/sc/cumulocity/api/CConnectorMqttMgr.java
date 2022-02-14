package com.hms_networks.sc.cumulocity.api;

import com.ewon.ewonitf.DefaultEventHandler;
import com.ewon.ewonitf.EWException;
import com.ewon.ewonitf.MqttMessage;
import com.ewon.ewonitf.SysControlBlock;
import com.hms_networks.americas.sc.extensions.logging.Logger;
import com.hms_networks.americas.sc.extensions.mqtt.MqttManager;
import com.hms_networks.americas.sc.extensions.mqtt.MqttStatusCode;
import com.hms_networks.americas.sc.extensions.string.StringUtils;
import com.hms_networks.sc.cumulocity.CConnectorMain;
import com.hms_networks.sc.cumulocity.api.CConnectorApiMessageBuilder.InstalledSoftware;
import com.hms_networks.sc.cumulocity.data.CConnectorAlarmMgr;
import java.io.UnsupportedEncodingException;
import java.util.List;

/**
 * The MQTT management class for the Cumulocity Connector.
 *
 * @author HMS Networks, MU Americas Solution Center
 * @since 1.0.0
 */
public class CConnectorMqttMgr extends MqttManager {

  /** The array of supported operations to be sent to Cumulocity. */
  public static final String[] CONNECTOR_SUPPORTED_OPERATIONS = {
    CConnectorApiMessageReader.CUMULOCITY_FIRMWARE_OPERATION_ID,
    CConnectorApiMessageReader.CUMULOCITY_SOFTWARE_OPERATION_ID,
    CConnectorApiMessageReader.CUMULOCITY_CONFIGURATION_OPERATION_ID,
    CConnectorApiMessageReader.CUMULOCITY_RUN_COMMAND_OPERATION_ID,
    CConnectorApiMessageReader.CUMULOCITY_RESTART_OPERATION_ID
  };

  /** The URL for downloading releases of the Ewon Flexy Cumulocity Connector. */
  public static final String CONNECTOR_DOWNLOAD_URL =
      "https://github.com/hms-networks/flexy-cumulocity-connector/releases";

  /** The URL for downloading releases of the Ewon Flexy firmware. */
  public static final String EWON_FIRMWARE_DOWNLOAD_WEBPAGE_URL =
      "https://hmsnetworks.blob.core.windows.net/www/docs/librariesprovider10/downloads-monitored/firmware/source/";

  /** QOS Level for MQTT connections. Azure IoT Hub requires QOS Level 1. */
  public static final int MQTT_QOS_LEVEL = 1;

  /** TLS version for MQTT connections. Azure IoT Hub connections use TLS v1.2. */
  public static final String MQTT_TLS_VERSION = "tlsv1.2";

  /** The MQTT topic for publishing messages to Cumulocity via MQTT. */
  private static final String CUMULOCITY_MQTT_TOPIC_SUS = "s/us";

  /** The MQTT topic for receiving messages from Cumulocity via MQTT. */
  private static final String CUMULOCITY_MQTT_TOPIC_SDS = "s/ds";

  /** The MQTT topic for publishing agent information messages to Cumulocity via MQTT. */
  private static final String CUMULOCITY_MQTT_TOPIC_AGENT_INFO_PREFIX =
      "inventory/managedObjects/update/";

  /** The maximum number of seconds between retries for MQTT connections. */
  public static final long MAX_SECONDS_BETWEEN_RETRIES = 180;

  /**
   * The time (in milliseconds) to wait between each MQTT loop. Changing this will modify the
   * interval at which payloads are checked for sending.
   */
  public static final long MQTT_LOOP_WAIT_MILLIS = 3000;

  /**
   * Boolean indicating if messages should be retained by the MQTT broken to send when a new client
   * subscribes to the applicable topic.
   */
  private static final boolean MQTT_RETAIN = false;

  /** MQTT loop attempt count. Used for retry backoff in {@link #runOnMqttLoop()} */
  private int mqttLoopAttemptCount = 0;

  /** MQTT loop limit, before triggering a reprovision event */
  private int mqttLoopAttemptLimit = 15;

  /** Boolean flag to track subscribing to the required MQTT topics. */
  private boolean subscribedToTopics = false;

  /** String containing the ID of the MQTT client. */
  private final String mqttId;

  /**
   * Constructor for the MQTT manager with the given MQTT ID, host, and boolean indicating if UTF-8
   * support is enabled.
   *
   * @param mqttId The ID of the MQTT client.
   * @param mqttHost The host of the MQTT broker.
   * @param enableUtf8 Boolean indicating if UTF-8 support is enabled.
   * @throws Exception if unable to create the MQTT client.
   */
  public CConnectorMqttMgr(String mqttId, String mqttHost, boolean enableUtf8) throws Exception {
    super(mqttId, mqttHost, enableUtf8);
    this.mqttId = mqttId;

    // Configure desired MQTT loop sleep interval
    setMqttThreadSleepIntervalMs(MQTT_LOOP_WAIT_MILLIS);

    // Add MQTT listener to default event handler
    DefaultEventHandler.addMqttListener(this);

    // Add alarm listener to default event handler
    DefaultEventHandler.setDefaultTagAlarmListener(new CConnectorAlarmMgr());
  }

  /**
   * Method for managing errors (exceptions) that occur in the MQTT manager.
   *
   * @param throwable exception to be handled
   */
  public void onError(Throwable throwable) {
    Logger.LOG_SERIOUS("The MQTT client has encountered an error (See exception details)!");
    Logger.LOG_EXCEPTION((Exception) throwable);
  }

  /**
   * Method for managing received MQTT messages.
   *
   * @param mqttMessage received MQTT message
   */
  public void onMessage(MqttMessage mqttMessage) {
    // Pass message to API message reader/parser
    String mqttMessagePayload = new String(mqttMessage.getPayload());
    CConnectorApiMessageReader.parseMessage(
        this, mqttMessage.getTopic(), mqttMessagePayload, mqttId);
  }

  /** Method for processing or performing tasks on the looping MQTT thread. */
  public void runOnMqttLoop() {
    try {
      if (getStatus() != MqttStatusCode.CONNECTED) {
        // Increment loop attempt count
        mqttLoopAttemptCount++;
        if (mqttLoopAttemptCount > mqttLoopAttemptLimit) {
          Logger.LOG_CRITICAL(
              "MQTT disconnected for "
                  + mqttLoopAttemptCount
                  + " cycles, triggering a reprovision.");
          CConnectorMain.rerunProvisioning();
        }
      }
    } catch (EWException ew) {
      Logger.LOG_SERIOUS("Exception getting MQTT status!");
      Logger.LOG_EXCEPTION(ew);
    }
  }

  /**
   * Method for managing MQTT status changes.
   *
   * @param status MQTT status code
   */
  public void onStatus(int status) {
    Logger.LOG_CRITICAL("MQTT client status changed to " + status);
  }

  /** Method for processing a successful MQTT connection. */
  public void onConnect() {
    Logger.LOG_CRITICAL("MQTT client is connected!");

    // Subscribe to Cumulocity MQTT topics
    if (!subscribedToTopics) {
      try {
        subscribe("s/e", MQTT_QOS_LEVEL);
        subscribe(CUMULOCITY_MQTT_TOPIC_SDS, MQTT_QOS_LEVEL);
        subscribe(CUMULOCITY_MQTT_TOPIC_SDS + "/*", MQTT_QOS_LEVEL);
        subscribedToTopics = true;
      } catch (Exception e) {
        Logger.LOG_CRITICAL(
            "Unable to subscribe to Cumulocity MQTT topics: "
                + CUMULOCITY_MQTT_TOPIC_SDS
                + " and "
                + CUMULOCITY_MQTT_TOPIC_SDS
                + "/*");
        Logger.LOG_EXCEPTION(e);
      }
    }

    // Finish operations which required reboot
    CConnectorApiMessageReader.finalizeRebootOperations(this);

    // Send connector, hardware, and software information to Cumulocity
    sendInformationToCumulocity();

    // Send configuration to Cumulocity
    sendConfigurationFileToCumulocity();

    // Request pending operations (500)
    try {
      mqttPublish(
          CUMULOCITY_MQTT_TOPIC_SUS,
          CConnectorApiMessageBuilder.getPendingOperations_500(),
          MQTT_QOS_LEVEL,
          MQTT_RETAIN);
    } catch (Exception e) {
      Logger.LOG_CRITICAL("Unable to request pending operations (Template 500) on MQTT connect!");
      Logger.LOG_EXCEPTION(e);
    }
  }

  /**
   * Method for initializing connection
   *
   * @throws Exception if unable to initialize connection
   */
  public void initConnection() throws Exception {
    Logger.LOG_DEBUG("Initializing MQTT connection...");

    // Configure MQTT
    setPortNonSecureDefault();
    setTLSVersion(MQTT_TLS_VERSION);

    // Get provisioner information
    String authPassword = CConnectorMain.getConnectorConfig().getCumulocityDevicePassword();
    String authUsername = CConnectorMain.getConnectorConfig().getCumulocityDeviceUsername();
    String authTenant = CConnectorMain.getConnectorConfig().getCumulocityDeviceTenant();

    // Apply provisioning information, if present/required
    setAuthUsername(authTenant + "/" + authUsername);
    setAuthPassword(authPassword);

    // Attempt connection to MQTT
    connect();
    Logger.LOG_DEBUG("Finished initializing MQTT connection.");
  }

  /** Sends basic information about the connector, hardware, and other software to Cumulocity. */
  public void sendInformationToCumulocity() {
    // Get connector information for adding software/agent information
    String connectorName =
        "\"" + CConnectorMqttMgr.class.getPackage().getImplementationTitle() + "\"";
    String connectorVersion =
        "\"" + CConnectorMqttMgr.class.getPackage().getImplementationVersion() + "\"";
    String connectorDownloadUrl = "\"" + CONNECTOR_DOWNLOAD_URL + "\"";

    // Send agent information
    try {
      String agentInfoPayload =
          CConnectorApiMessageBuilder.buildC8YAgentPayload(
              connectorName, connectorVersion, connectorDownloadUrl);
      String agentInfoTopic = CUMULOCITY_MQTT_TOPIC_AGENT_INFO_PREFIX + mqttId;
      mqttPublish(agentInfoTopic, agentInfoPayload, MQTT_QOS_LEVEL, MQTT_RETAIN);
    } catch (Exception e) {
      Logger.LOG_CRITICAL("Unable to send agent information to Cumulocity!");
      Logger.LOG_EXCEPTION(e);
    }

    // Send supported operation information
    try {
      mqttPublish(
          CUMULOCITY_MQTT_TOPIC_SUS,
          CConnectorApiMessageBuilder.setSupportedOperations_114(CONNECTOR_SUPPORTED_OPERATIONS),
          MQTT_QOS_LEVEL,
          MQTT_RETAIN);
    } catch (Exception e) {
      Logger.LOG_CRITICAL("Unable to send supported operation information to Cumulocity!");
      Logger.LOG_EXCEPTION(e);
    }

    // Send basic hardware information
    try {
      SysControlBlock sysControlBlockInf = new SysControlBlock(SysControlBlock.INF);
      String serialNumber = "\"" + sysControlBlockInf.getItem("MbSerNum") + "\"";
      String modelNumber = "\"" + sysControlBlockInf.getItem("MbPartNum") + "\"";
      String revision = "\"" + sysControlBlockInf.getItem("MbExtInfo") + "\"";
      mqttPublish(
          CUMULOCITY_MQTT_TOPIC_SUS,
          CConnectorApiMessageBuilder.configureHardware_110(serialNumber, modelNumber, revision),
          MQTT_QOS_LEVEL,
          MQTT_RETAIN);
    } catch (Exception e) {
      Logger.LOG_CRITICAL("Unable to send hardware information to Cumulocity!");
      Logger.LOG_EXCEPTION(e);
    }

    // Send basic firmware information
    try {
      SysControlBlock sysControlBlockInf = new SysControlBlock(SysControlBlock.INF);
      String firmwareVersion = "\"" + sysControlBlockInf.getItem("CodeName") + "\"";
      String firmwareDownloadUrlVersion = StringUtils.replace(firmwareVersion, ".", "_");
      List serialNumberSplit = StringUtils.split(sysControlBlockInf.getItem("MbSerNum"), "-");
      String firmwareDownloadPCode = serialNumberSplit.get(serialNumberSplit.size() - 1).toString();
      String firmwareDownloadUrl =
          "\""
              + EWON_FIRMWARE_DOWNLOAD_WEBPAGE_URL
              + "er"
              + firmwareDownloadUrlVersion
              + "p"
              + firmwareDownloadPCode
              + "_ma.edf"
              + "\"";
      mqttPublish(
          CUMULOCITY_MQTT_TOPIC_SUS,
          CConnectorApiMessageBuilder.setFirmware_115(
              "Ewon Firmware", firmwareVersion, firmwareDownloadUrl),
          MQTT_QOS_LEVEL,
          MQTT_RETAIN);
    } catch (Exception e) {
      Logger.LOG_CRITICAL("Unable to send firmware information to Cumulocity!");
      Logger.LOG_EXCEPTION(e);
    }

    // Send basic software information
    try {
      // Create installed software array and add connector information as entry
      InstalledSoftware[] installedSoftware = new InstalledSoftware[1];
      installedSoftware[0] =
          new InstalledSoftware(connectorName, connectorVersion, connectorDownloadUrl);
      mqttPublish(
          CUMULOCITY_MQTT_TOPIC_SUS,
          CConnectorApiMessageBuilder.setSoftwareList_116(installedSoftware),
          MQTT_QOS_LEVEL,
          MQTT_RETAIN);
    } catch (Exception e) {
      Logger.LOG_CRITICAL("Unable to send software information to Cumulocity!");
      Logger.LOG_EXCEPTION(e);
    }
  }

  /**
   * Sends the specified operation response to Cumulocity on the correct topic based on the original
   * request topic.
   *
   * @param originalMessageTopic the topic where the original request was received
   * @param operationResponsePayload the operation response payload to send
   */
  public void sendOperationResponse(String originalMessageTopic, String operationResponsePayload) {
    // Build topic name to publish operation response to (switch downstream to upstream)
    String operationResponseTopic =
        StringUtils.replace(
            originalMessageTopic, CUMULOCITY_MQTT_TOPIC_SDS, CUMULOCITY_MQTT_TOPIC_SUS);

    // Send operation response to Cumulocity
    try {
      mqttPublish(operationResponseTopic, operationResponsePayload, MQTT_QOS_LEVEL, MQTT_RETAIN);
    } catch (Exception e) {
      Logger.LOG_CRITICAL("Unable to send a response for a received message or operation!");
      Logger.LOG_EXCEPTION(e);
    }
  }

  /** Sends the current connector configuration to Cumulocity. */
  public void sendConfigurationFileToCumulocity() {
    // Send configuration file to Cumulocity
    try {
      final boolean maskPasswordsInConfig = true;
      String s =
          CConnectorApiMessageBuilder.setConfiguration_113(
              CConnectorMain.getConnectorConfig()
                  .getConfigFileEscapedString(maskPasswordsInConfig));
      mqttPublish(CUMULOCITY_MQTT_TOPIC_SUS, s, MQTT_QOS_LEVEL, MQTT_RETAIN);
    } catch (Exception e) {
      Logger.LOG_CRITICAL("Unable to send the current configuration to Cumulocity!");
      Logger.LOG_EXCEPTION(e);
    }
  }

  /**
   * Sends the specified message to Cumulocity with the proper topic for routing to a child device,
   * if not null.
   *
   * @param messagePayload the message payload to send
   * @param childDevice the child device to route the message to (if not null)
   * @throws EWException if an Ewon exception occurs, check the Ewon event log for more details
   * @throws UnsupportedEncodingException if the character encoding is not supported
   */
  public void sendMessageWithChildDeviceRouting(String messagePayload, String childDevice)
      throws EWException, UnsupportedEncodingException {
    // Build topic name to publish method (append child device, if not null)
    String messageTopic = CUMULOCITY_MQTT_TOPIC_SUS;
    if (childDevice != null) {
      messageTopic += "/" + childDevice;
    }

    // Send message to Cumulocity
    mqttPublish(messageTopic, messagePayload, MQTT_QOS_LEVEL, MQTT_RETAIN);
    Logger.LOG_DEBUG(
        "Sent message to Cumulocity on topic [" + messageTopic + "]: " + messagePayload);
  }
}
