package com.hmsnetworks.sc.cumulocity.api;

import com.ewon.ewonitf.MqttMessage;
import com.hms_networks.americas.sc.extensions.json.JSONException;
import com.hms_networks.americas.sc.extensions.logging.Logger;
import com.hms_networks.americas.sc.extensions.mqtt.ConstrainedMqttManager;
import com.hms_networks.americas.sc.extensions.string.StringUtils;
import com.hmsnetworks.sc.cumulocity.CConnectorMain;
import java.util.List;

/**
 * The MQTT management class for the Cumulocity Connector.
 *
 * @author HMS Networks, MU Americas Solution Center
 * @since 1.0.0
 */
public class CConnectorProvisionMqttMgr extends ConstrainedMqttManager {

  /** QOS Level for MQTT connections. Azure IoT Hub requires QOS Level 1. */
  public static final int MQTT_QOS_LEVEL = 1;

  /** TLS version for MQTT connections. Azure IoT Hub connections use TLS v1.2. */
  public static final String MQTT_TLS_VERSION = "tlsv1.2";

  /** The MQTT topic for publishing messages to Cumulocity via MQTT. */
  private static final String CUMULOCITY_MQTT_TOPIC_UCR = "s/ucr";

  /** The MQTT topic for receiving messages from Cumulocity via MQTT. */
  private static final String CUMULOCITY_MQTT_TOPIC_DCR = "s/dcr";

  /** The message ID used for MQTT device provisioning messages with credentials. */
  private static final String CUMULOCITY_MQTT_PROVISION_CREDENTIALS_MESSAGE_ID = "70";

  /** The index of the message ID in the MQTT device provisioning message with credentials. */
  private static final int CUMULOCITY_MQTT_PROVISION_CREDENTIALS_MESSAGE_ID_INDEX = 0;

  /** The index of the tenant in the MQTT device provisioning message with credentials. */
  private static final int CUMULOCITY_MQTT_PROVISION_CREDENTIALS_MESSAGE_TENANT_INDEX = 1;

  /** The index of the username in the MQTT device provisioning message with credentials. */
  private static final int CUMULOCITY_MQTT_PROVISION_CREDENTIALS_MESSAGE_USERNAME_INDEX = 2;

  /** The index of the password in the MQTT device provisioning message with credentials. */
  private static final int CUMULOCITY_MQTT_PROVISION_CREDENTIALS_MESSAGE_PASSWORD_INDEX = 3;

  /** The number of fields in the MQTT device provisioning message with credentials. */
  private static final int CUMULOCITY_MQTT_PROVISION_CREDENTIALS_MESSAGE_NUM_INDEXES = 4;

  /** The maximum number of seconds between retries for MQTT connections. */
  public static final long MAX_SECONDS_BETWEEN_RETRIES = 180;

  /**
   * The time (in milliseconds) to wait between each MQTT loop. Changing this will modify the
   * interval at which payloads are checked for sending.
   */
  public static final long MQTT_LOOP_WAIT_MILLIS = 5000;

  /**
   * Boolean indicating of the MQTT manager should wait for a WAN IP address to be available before
   * initializing.
   */
  private static final boolean MQTT_WAIT_FOR_WAN_IP = true;

  /** The boolean value indicating whether the application is provisioned to Cumulocity or not. */
  private boolean isProvisioned = false;

  /**
   * Constructor for the MQTT provisioning manager with the given MQTT ID, host, and boolean
   * indicating if UTF-8 support is enabled.
   *
   * @param mqttId The ID of the MQTT client.
   * @param mqttHost The host of the MQTT broker.
   * @param enableUtf8 Boolean indicating if UTF-8 support is enabled.
   * @param port The port of the MQTT broker.
   * @param rootCaFilePath The path to the root CA file for the MQTT broker.
   * @param mqttUsername The username for the MQTT broker.
   * @param mqttPassword The password for the MQTT broker.
   * @throws Exception if unable to create the MQTT client.
   */
  public CConnectorProvisionMqttMgr(
      String mqttId,
      String mqttHost,
      boolean enableUtf8,
      String port,
      String rootCaFilePath,
      String mqttUsername,
      String mqttPassword)
      throws Exception {
    super(
        mqttId,
        mqttHost,
        enableUtf8,
        port,
        rootCaFilePath,
        MQTT_TLS_VERSION,
        mqttUsername,
        mqttPassword,
        MQTT_QOS_LEVEL,
        MQTT_LOOP_WAIT_MILLIS,
        MQTT_WAIT_FOR_WAN_IP);

    // Subscribe to the MQTT topics
    addSubscription(CUMULOCITY_MQTT_TOPIC_DCR);
  }

  /**
   * Method for managing errors (exceptions) that occur in the MQTT manager.
   *
   * @param throwable exception to be handled
   */
  public void onError(Throwable throwable) {
    Logger.LOG_SERIOUS(
        "The MQTT device provisioning client has encountered an error (See exception details)!");
    Logger.LOG_EXCEPTION((Exception) throwable);
  }

  /**
   * Method for managing received MQTT messages.
   *
   * @param mqttMessage received MQTT message
   */
  public void onMessage(MqttMessage mqttMessage) {
    // Check if message is in proper format
    String mqttMessagePayload = new String(mqttMessage.getPayload());
    List mqttMessagePayloadSplit = StringUtils.split(mqttMessagePayload, ",");
    if (mqttMessagePayloadSplit.size()
        == CUMULOCITY_MQTT_PROVISION_CREDENTIALS_MESSAGE_NUM_INDEXES) {
      String mqttMessageId =
          (String)
              mqttMessagePayloadSplit.get(CUMULOCITY_MQTT_PROVISION_CREDENTIALS_MESSAGE_ID_INDEX);
      String mqttMessageDeviceTenant =
          (String)
              mqttMessagePayloadSplit.get(
                  CUMULOCITY_MQTT_PROVISION_CREDENTIALS_MESSAGE_TENANT_INDEX);
      String mqttMessageDeviceUsername =
          (String)
              mqttMessagePayloadSplit.get(
                  CUMULOCITY_MQTT_PROVISION_CREDENTIALS_MESSAGE_USERNAME_INDEX);
      String mqttMessageDevicePassword =
          (String)
              mqttMessagePayloadSplit.get(
                  CUMULOCITY_MQTT_PROVISION_CREDENTIALS_MESSAGE_PASSWORD_INDEX);

      // Check if message is a device provisioning credentials message
      if (mqttMessageId.equals(CUMULOCITY_MQTT_PROVISION_CREDENTIALS_MESSAGE_ID)) {
        // Store new device credentials
        try {
          CConnectorMain.getConnectorConfig().setCumulocityDeviceTenant(mqttMessageDeviceTenant);
          CConnectorMain.getConnectorConfig()
              .setCumulocityDeviceUsername(mqttMessageDeviceUsername);
          CConnectorMain.getConnectorConfig()
              .setCumulocityDevicePassword(mqttMessageDevicePassword);
          synchronized (this) {
            isProvisioned = true;
            notifyAll();
          }
        } catch (JSONException e) {
          Logger.LOG_CRITICAL("Unable to save new device credentials during provisioning!");
          Logger.LOG_EXCEPTION(e);
        }
      }
    }
  }

  /**
   * Method for processing or performing tasks on the looping MQTT thread.
   *
   * @param currentMqttStatus the current MQTT status integer
   */
  public void runOnMqttLoop(int currentMqttStatus) {
    // Send empty message to Cumulocity (if subscribed to receive response)
    if (getMqttSubscribed()) {
      try {
        mqttPublish(CUMULOCITY_MQTT_TOPIC_UCR, "", MQTT_QOS_LEVEL, true);
      } catch (Exception e) {
        Logger.LOG_CRITICAL("Unable to send device provisioning empty message to Cumulocity!");
        Logger.LOG_EXCEPTION(e);
      }
    }
  }

  /**
   * Method for managing MQTT status changes.
   *
   * @param status MQTT status code
   */
  public void onStatus(int status) {
    Logger.LOG_CRITICAL("MQTT device provisioning client status changed to " + status);
  }

  /** Method for processing a successful MQTT connection. */
  public void onConnect() {
    Logger.LOG_CRITICAL("MQTT device provisioning client is connected!");
  }

  /**
   * Method for cancelling the device provisioning process. This method is called when the device
   * provisioning process is to be cancelled. This method will stop the MQTT client and release any
   * threads waiting for provisioning.
   */
  public synchronized void cancelProvisioning() {
    // Stop MQTT client
    stop();

    // Release any threads waiting for provisioning
    isProvisioned = true;
    notifyAll();
  }

  /**
   * Method for waiting until device provisioning is complete. The calling thread is blocked until
   * released on completion of the device provisioning process.
   *
   * @throws InterruptedException if the MQTT provisioning manager is interrupted
   */
  public synchronized void awaitProvisioning() throws InterruptedException {
    if (!isProvisioned) {
      wait();
    }
  }
}
