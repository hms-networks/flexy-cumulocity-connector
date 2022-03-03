package com.hms_networks.sc.cumulocity.api;

import com.ewon.ewonitf.EWException;
import com.ewon.ewonitf.RuntimeControl;
import com.ewon.ewonitf.ScheduledActionManager;
import com.ewon.ewonitf.TagControl;
import com.hms_networks.americas.sc.extensions.fileutils.FileAccessManager;
import com.hms_networks.americas.sc.extensions.json.JSONException;
import com.hms_networks.americas.sc.extensions.logging.Logger;
import com.hms_networks.americas.sc.extensions.string.StringUtils;
import com.hms_networks.americas.sc.extensions.system.application.SCAppManagement;
import com.hms_networks.americas.sc.extensions.system.http.SCHttpUtility;
import com.hms_networks.americas.sc.extensions.taginfo.TagInfo;
import com.hms_networks.americas.sc.extensions.taginfo.TagInfoManager;
import com.hms_networks.americas.sc.extensions.taginfo.TagType;
import com.hms_networks.americas.sc.extensions.util.Base64;
import com.hms_networks.sc.cumulocity.CConnectorMain;
import com.hms_networks.sc.cumulocity.data.CConnectorDataMgr;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;

/**
 * Helper class for parsing and handling MQTT payloads corresponding to the available subscribe
 * templates on Cumulocity.
 *
 * @author HMS Networks, MU Americas Solution Center
 * @since 1.0.0
 */
public class CConnectorApiMessageReader {

  /** The response string used to indicate a successful operation. */
  public static final String RESPONSE_SUCCESS = "Operation successful.";

  /** The response string used to indicate a failed operation due to device ID mismatch. */
  public static final String RESPONSE_DEVICE_ID_MISMATCH =
      "The device ID in the payload does not match the expected device ID.";

  /**
   * The response string used to indicate a failed operation due to an unknown or unsupported
   * command.
   */
  public static final String RESPONSE_COMMAND_UNKNOWN_NOT_SUPPORTED =
      "The requested command is not known or is not supported.";

  /**
   * The response string used to indicate an in-progress operation which requires a reboot to apply
   * changes or finish.
   */
  public static final String RESPONSE_TEMPORARY_FILE_CREATE_ERROR =
      "Unable to create the temporary file used to indicate an in-progress operation during device"
          + " reboot.";

  /**
   * The response string used to indicate a failed operation due to an unknown message or operation.
   */
  public static final String RESPONSE_UNKNOWN_MESSAGE_OPERATION_TYPE =
      "Unknown message or operation.";

  /**
   * The response string used to indicate a failed operation due to a configuration file parse
   * error.
   */
  public static final String RESPONSE_CONFIG_FILE_PARSE_ERROR =
      "Unable to parse the configuration file contents.";

  /**
   * The response string used to indicate a failed operation due to an error creating a tag control
   * object
   */
  public static final String RESPONSE_UNABLE_SET_COMMAND_NO_TAG_CTRL =
      "Unable to process set/setf command from Cumulocity because a TagControl object cannot be "
          + "created! Check that the tag exists on the Ewon system and that there are no spelling "
          + "errors.";

  /**
   * The response string used to indicate a failed operation due to an exception while setting the
   * tag value.
   */
  public static final String RESPONSE_UNABLE_SET_COMMAND_EXCEPTION =
      "Unable to process set/setf command from Cumulocity because an exception occurred while "
          + "setting the tag value! Check that the value specified is of the correct type.";

  /**
   * The response string used to indicate a failed operation due to an error in the expected format
   * or parameters.
   */
  public static final String RESPONSE_UNABLE_SET_COMMAND_FORMAT =
      "Unable to process set/setf command from Cumulocity because the required parameters were "
          + "either missing or incorrectly formatted. Check the format of the command.";

  /**
   * The response string used to indicate a failed operation due to an exception while downloading
   * the firmware file.
   */
  public static final String RESPONSE_DOWNLOAD_FIRMWARE_EXCEPTION =
      "Unable to download and update the device firmware due to an exception.";

  /**
   * The response string used to indicate a failed operation due to an error creating a tag control
   * object for the measurements enable/disable control tag.
   */
  public static final String RESPONSE_UNABLE_SET_MEASUREMENTS_COMMAND_NO_TAG_CTRL =
      "Unable to process measurements enable/disable command from Cumulocity because a TagControl "
          + "object cannot be created! Check that the tag creation is successful and no errors "
          + "occur during connector startup that may prevent this tag from being created.";

  /**
   * The response string used to indicate a failed operation due to an exception while setting the
   * measurements enable/disable control tag value.
   */
  public static final String RESPONSE_UNABLE_SET_MEASUREMENTS_COMMAND_EXCEPTION =
      "Unable to process measurements enable/disable command from Cumulocity because an exception "
          + "occurred while setting the corresponding tag value! Check that the value specified is "
          + "of the correct type.";

  /**
   * The response string used to indicate a failed operation due to a formatting error while setting
   * the measurements enable/disable control tag value.
   */
  public static final String RESPONSE_UNABLE_SET_MEASUREMENTS_COMMAND_FORMAT =
      "Unable to process measurements enable/disable command from Cumulocity because the value was "
          + "not in the expected format (enable/disable).";

  /**
   * The name of the temporary file used to indicate that the connector is restarting due to a
   * c8y_Restart operation.
   */
  public static final String TEMPORARY_RESTART_FILE_PATH = "/usr/cumulocityRestarting.tmp";

  /**
   * The name of the temporary file used to indicate that the connector is rebooting due to a
   * c8y_Configuration operation.
   */
  public static final String TEMPORARY_CONFIG_RESTART_FILE_PATH =
      "/usr/cumulocityConfigRestarting.tmp";

  /**
   * The name of the temporary file used to indicate that the connector is rebooting due to a
   * c8y_Firmware operation.
   */
  public static final String TEMPORARY_FIRMWARE_RESTART_FILE_PATH =
      "/usr/cumulocityFwRestarting.tmp";

  /** The Cumulocity operation ID for a restart operation. */
  public static final String CUMULOCITY_RESTART_OPERATION_ID = "c8y_Restart";

  /** The Cumulocity operation ID for a configuration operation. */
  public static final String CUMULOCITY_CONFIGURATION_OPERATION_ID = "c8y_Configuration";

  /** The Cumulocity operation ID for a firmware operation. */
  public static final String CUMULOCITY_FIRMWARE_OPERATION_ID = "c8y_Firmware";

  /** The Cumulocity operation ID for a command operation. */
  public static final String CUMULOCITY_RUN_COMMAND_OPERATION_ID = "c8y_Command";

  /** The Cumulocity operation ID for a software operation. */
  public static final String CUMULOCITY_SOFTWARE_OPERATION_ID = "c8y_Software";

  /**
   * Method to finalize any operations which required a reboot to apply changes or complete, and
   * report their status to Cumulocity.
   *
   * @param mqttMgr The MQTT manager to use to send the status message.
   */
  public static void finalizeRebootOperations(CConnectorMqttMgr mqttMgr) {
    // Report restart operation as successful if it was previously in progress
    File tempRestartFile = new File(TEMPORARY_RESTART_FILE_PATH);
    if (tempRestartFile.exists()) {
      try {
        String[] successfulOperationParameters = {};
        String originalOperationTopic =
            FileAccessManager.readFileToString(TEMPORARY_RESTART_FILE_PATH);
        String operationResponsePayload =
            CConnectorApiMessageBuilder.setOperationToSuccessful_503(
                CUMULOCITY_RESTART_OPERATION_ID, successfulOperationParameters);
        mqttMgr.sendOperationResponse(originalOperationTopic, operationResponsePayload);
        Logger.LOG_DEBUG("Successfully reported restart operation as successful.");
        boolean deleted = tempRestartFile.delete();
        if (!deleted) {
          Logger.LOG_CRITICAL(
              "Unable to delete temporary file that indicated a device reboot was in progress!"
                  + " Duplicate successful reboot operation responses may be sent to Cumulocity.");
        }
      } catch (IOException e) {
        Logger.LOG_CRITICAL(
            "Unable to read temporary file to mark device reboot as successful! The operation may"
                + " appear as stalled in Cumulocity.");
        Logger.LOG_EXCEPTION(e);
      } catch (Exception e) {
        Logger.LOG_CRITICAL(
            "Unable to send reboot operation successful message! The operation may appear as"
                + " stalled in Cumulocity.");
        Logger.LOG_EXCEPTION(e);
      }
    }

    // Report config restart operation as successful if it was previously in progress
    File tempConfigRestartFile = new File(TEMPORARY_CONFIG_RESTART_FILE_PATH);
    if (tempConfigRestartFile.exists()) {
      try {
        String[] successfulOperationParameters = {};
        String originalOperationTopic =
            FileAccessManager.readFileToString(TEMPORARY_CONFIG_RESTART_FILE_PATH);
        String operationResponsePayload =
            CConnectorApiMessageBuilder.setOperationToSuccessful_503(
                CUMULOCITY_CONFIGURATION_OPERATION_ID, successfulOperationParameters);
        mqttMgr.sendOperationResponse(originalOperationTopic, operationResponsePayload);
        Logger.LOG_DEBUG("Successfully reported configuration operation as successful.");
        boolean deleted = tempConfigRestartFile.delete();
        if (!deleted) {
          Logger.LOG_CRITICAL(
              "Unable to delete temporary file that indicated a configuration reboot was in"
                  + " progress! Duplicate successful configuration operation responses may be sent"
                  + " to Cumulocity.");
        }
      } catch (IOException e) {
        Logger.LOG_CRITICAL(
            "Unable to read temporary file to mark configuration reboot as successful! The"
                + " operation may appear as stalled in Cumulocity.");
        Logger.LOG_EXCEPTION(e);
      } catch (Exception e) {
        Logger.LOG_CRITICAL(
            "Unable to send configuration operation successful message! The operation may appear as"
                + " stalled in Cumulocity.");
        Logger.LOG_EXCEPTION(e);
      }
    }

    // Report firmware restart operation as successful if it was previously in progress
    File tempFirmwareRestartFile = new File(TEMPORARY_FIRMWARE_RESTART_FILE_PATH);
    if (tempFirmwareRestartFile.exists()) {
      try {
        String[] successfulOperationParameters = {};
        String originalOperationTopic =
            FileAccessManager.readFileToString(TEMPORARY_FIRMWARE_RESTART_FILE_PATH);
        String operationResponsePayload =
            CConnectorApiMessageBuilder.setOperationToSuccessful_503(
                CUMULOCITY_FIRMWARE_OPERATION_ID, successfulOperationParameters);
        mqttMgr.sendOperationResponse(originalOperationTopic, operationResponsePayload);
        Logger.LOG_DEBUG("Successfully reported firmware install operation as successful.");
        boolean deleted = tempFirmwareRestartFile.delete();
        if (!deleted) {
          Logger.LOG_CRITICAL(
              "Unable to delete temporary file that indicated a firmware install reboot was in"
                  + " progress! Duplicate successful firmware install operation responses may be"
                  + " sent to Cumulocity.");
        }
      } catch (IOException e) {
        Logger.LOG_CRITICAL(
            "Unable to read temporary file to mark firmware install reboot as successful! The"
                + " operation may appear as stalled in Cumulocity.");
        Logger.LOG_EXCEPTION(e);
      } catch (Exception e) {
        Logger.LOG_CRITICAL(
            "Unable to send firmware install operation successful message! The operation may appear"
                + " as stalled in Cumulocity.");
        Logger.LOG_EXCEPTION(e);
      }
    }
  }

  /**
   * Parses and handles the specified MQTT message which was received on the specified {@link
   * CConnectorMqttMgr}, then returns a String result indicating success or failure reason.
   *
   * @param mqttMgr MQTT manager that received the message
   * @param mqttTopic MQTT topic on which the message was received
   * @param message message received on the MQTT manager
   * @param expectedDeviceId expected device ID of the message
   */
  public static void parseMessage(
      CConnectorMqttMgr mqttMgr, String mqttTopic, String message, String expectedDeviceId) {
    if (isErrorResponse(message)) {
      parseErrorResponse(mqttMgr, mqttTopic, message, expectedDeviceId);
    } else if (isRestartDevice_510(message)) {
      restartDevice_510(mqttMgr, mqttTopic, message, expectedDeviceId);
    } else if (isRunCommand_511(message)) {
      runCommand_511(mqttMgr, mqttTopic, message, expectedDeviceId);
    } else if (isSetConfiguration_513(message)) {
      setConfiguration_513(mqttMgr, mqttTopic, message, expectedDeviceId);
    } else if (isInstallFirmware_515(message)) {
      installFirmware_515(mqttMgr, mqttTopic, message, expectedDeviceId);
    } else {
      Logger.LOG_SERIOUS(
          "An unknown message or operation was received from Cumulocity: " + message);
    }
  }

  /**
   * Gets a boolean indicating if the specified message is a valid error response message.
   *
   * @param message message to be checked
   * @return true if the specified message is a valid error response message, false otherwise
   */
  public static boolean isErrorResponse(String message) {
    return message.startsWith("41,");
  }

  /**
   * Parses a specified error response message and logs it to the realtime logs.
   *
   * @param mqttMgr MQTT manager that received the error message
   * @param mqttTopic MQTT topic on which the message was received
   * @param message error message received on the MQTT manager
   * @param expectedDeviceId expected device ID of the message
   */
  public static void parseErrorResponse(
      CConnectorMqttMgr mqttMgr, String mqttTopic, String message, String expectedDeviceId) {
    // Extract errored template number and reason
    final int erroredTemplateIndex = 1;
    final int erroredTemplateReasonIndex = 2;
    List parts = StringUtils.split(message, ",");
    String erroredTemplate = (String) parts.get(erroredTemplateIndex);
    String erroredTemplateReason = (String) parts.get(erroredTemplateReasonIndex);

    // Log errored template and reason
    Logger.LOG_SERIOUS(
        "An error was received from Cumulocity for the template ["
            + erroredTemplate
            + "]: "
            + erroredTemplateReason);
  }

  /**
   * Not yet implemented (Unavailable).
   *
   * @param message Not yet implemented (Unavailable)
   * @return Not yet implemented (Unavailable)
   */
  public static boolean getChildenOfDevice_106(String message) {
    return false;
  }

  /**
   * Gets a boolean indicating if the specified message is a valid 510/restart device message.
   *
   * @param message message to be checked
   * @return true if the specified message is a valid 510/restart device message, false otherwise
   */
  public static boolean isRestartDevice_510(String message) {
    return message.startsWith("510");
  }

  /**
   * Parses a specified 510/restart device message and returns a boolean indicating if the device ID
   * matches.
   *
   * @param mqttMgr MQTT manager that received the message
   * @param mqttTopic MQTT topic where the message was received
   * @param message 510/restart device message to parse
   * @param expectedDeviceId expected ID of the device to be restarted
   */
  public static void restartDevice_510(
      CConnectorMqttMgr mqttMgr, String mqttTopic, String message, String expectedDeviceId) {
    // Extract device ID from message
    final int deviceIdIndex = 1;
    List parts = StringUtils.split(message, ",");
    String deviceId = (String) parts.get(deviceIdIndex);

    // Update state to executing
    String operationResponsePayloadExecuting =
        CConnectorApiMessageBuilder.setOperationToExecuting_501(CUMULOCITY_RESTART_OPERATION_ID);
    mqttMgr.sendOperationResponse(mqttTopic, operationResponsePayloadExecuting);

    // Check if device ID matches
    if (deviceId.equals(expectedDeviceId)) {
      // Create temporary file to store in progress reboot operation information
      try {
        FileAccessManager.writeStringToFile(TEMPORARY_RESTART_FILE_PATH, mqttTopic);
        Logger.LOG_CRITICAL("Cumulocity has requested a restart of the device. Restarting...");
        CConnectorMain.shutdownConnectorAndRestartDevice();
      } catch (IOException e) {
        Logger.LOG_CRITICAL(RESPONSE_TEMPORARY_FILE_CREATE_ERROR);
        Logger.LOG_EXCEPTION(e);
        String operationResponsePayload =
            CConnectorApiMessageBuilder.setOperationToFailed_502(
                CUMULOCITY_RESTART_OPERATION_ID, RESPONSE_TEMPORARY_FILE_CREATE_ERROR);
        mqttMgr.sendOperationResponse(mqttTopic, operationResponsePayload);
      }
    } else {
      String operationResponsePayload =
          CConnectorApiMessageBuilder.setOperationToFailed_502(
              CUMULOCITY_RESTART_OPERATION_ID, RESPONSE_DEVICE_ID_MISMATCH);
      mqttMgr.sendOperationResponse(mqttTopic, operationResponsePayload);
    }
  }

  /**
   * Gets a boolean indicating if the specified message is a valid 511/runCommand device message.
   *
   * @param message message to be checked
   * @return true if the specified message is a valid 511/runCommand device message, false otherwise
   */
  public static boolean isRunCommand_511(String message) {
    return message.startsWith("511");
  }

  /**
   * Parses a specified 511/runCommand device message and returns a boolean indicating if the device
   * ID matches.
   *
   * @param mqttMgr MQTT manager that received the message
   * @param mqttTopic MQTT topic where the message was received
   * @param message 511/runCommand device message to parse
   * @param expectedDeviceId expected ID of the device to run command on
   */
  public static void runCommand_511(
      CConnectorMqttMgr mqttMgr, String mqttTopic, String message, String expectedDeviceId) {
    // Update state to executing
    String operationResponsePayloadExecuting =
        CConnectorApiMessageBuilder.setOperationToExecuting_501(
            CUMULOCITY_RUN_COMMAND_OPERATION_ID);
    mqttMgr.sendOperationResponse(mqttTopic, operationResponsePayloadExecuting);

    // Extract message parts
    final int deviceIdIndex = 1;
    final int commandIndex = 2;
    List parts = StringUtils.split(message, ",");
    String deviceId = (String) parts.get(deviceIdIndex);
    String command = (String) parts.get(commandIndex);

    // Remove quotes around command, if present
    if (command.startsWith("\"") && command.endsWith("\"")) {
      command = command.substring(1, command.length() - 1);
    }

    List topicParts = StringUtils.split(mqttTopic, "/");
    String childDeviceName;
    if (topicParts.size() > 2) {
      childDeviceName = (String) topicParts.get(2);
    } else {
      childDeviceName = null;
    }

    // Split command to parts
    List commandParts = StringUtils.split(command, " ");

    // Execute command
    boolean deviceIdMatches = deviceId.equals(expectedDeviceId);
    if (deviceIdMatches) {
      Logger.LOG_SERIOUS(
          "Executing command: "
              + command
              + " on device: "
              + expectedDeviceId
              + (childDeviceName != null ? " (" + childDeviceName + ")" : ""));

      // Check if set/setf tag command
      if (command.startsWith("set") || command.startsWith("setf")) {
        String tagName;
        String tagValue;
        try {
          if (command.startsWith("setf")) {
            tagName =
                (String) commandParts.get(1)
                    + CConnectorDataMgr.SPLIT_TAG_NAME_DELIMITER
                    + (String) commandParts.get(2);
            tagValue = (String) commandParts.get(3);
          } else {
            tagName = (String) commandParts.get(1);
            tagValue = (String) commandParts.get(2);
          }

          // Append child device name if present/applicable
          if (childDeviceName != null) {
            tagName = childDeviceName + CConnectorDataMgr.SPLIT_TAG_NAME_DELIMITER + tagName;
          }
        } catch (IndexOutOfBoundsException e) {
          tagName = null;
          tagValue = null;
        }

        // Only continue if tag name and value are not null (command format was parsable)
        if (tagName != null && tagValue != null) {
          // Check if tag name is valid
          TagControl tagControl = null;
          try {
            tagControl = new TagControl(tagName);
          } catch (Exception e) {
            Logger.LOG_WARN(RESPONSE_UNABLE_SET_COMMAND_NO_TAG_CTRL);
            Logger.LOG_EXCEPTION(e);
            String operationResponsePayload =
                CConnectorApiMessageBuilder.setOperationToFailed_502(
                    CUMULOCITY_RUN_COMMAND_OPERATION_ID, RESPONSE_UNABLE_SET_COMMAND_NO_TAG_CTRL);
            mqttMgr.sendOperationResponse(mqttTopic, operationResponsePayload);
          }

          // Check if tag value is valid
          try {
            if (tagControl != null) {
              int tagId = tagControl.getTagId();
              TagInfo tagInfoForId = TagInfoManager.getTagInfoFromTagId(tagId);
              if (tagInfoForId.getType() == TagType.BOOLEAN) {
                int tagValueInt;
                if (tagValue.equalsIgnoreCase("true") || tagValue.equalsIgnoreCase("false")) {
                  tagValueInt = tagValue.equalsIgnoreCase("true") ? 1 : 0;
                } else {
                  tagValueInt = Integer.parseInt(tagValue);
                }
                tagControl.setTagValueAsInt(tagValueInt);
              } else if (tagInfoForId.getType() == TagType.STRING) {
                tagControl.setTagValueAsString(tagValue);
              } else if (tagInfoForId.getType() == TagType.DWORD) {
                long tagValueLong = Long.valueOf(tagValue).longValue();
                tagControl.setTagValueAsLong(tagValueLong);
              } else if (tagInfoForId.getType() == TagType.FLOAT) {
                double tagValueDouble = Double.valueOf(tagValue).doubleValue();
                tagControl.setTagValueAsDouble(tagValueDouble);
              } else {
                int tagValueInt = Integer.parseInt(tagValue);
                tagControl.setTagValueAsInt(tagValueInt);
              }
              String operationResponsePayload =
                  CConnectorApiMessageBuilder.setOperationToSuccessful_503(
                      CUMULOCITY_RUN_COMMAND_OPERATION_ID, new String[] {RESPONSE_SUCCESS});
              mqttMgr.sendOperationResponse(mqttTopic, operationResponsePayload);
            }
          } catch (Exception e) {
            Logger.LOG_WARN(RESPONSE_UNABLE_SET_COMMAND_EXCEPTION);
            Logger.LOG_EXCEPTION(e);
            String operationResponsePayload =
                CConnectorApiMessageBuilder.setOperationToFailed_502(
                    CUMULOCITY_RUN_COMMAND_OPERATION_ID, RESPONSE_UNABLE_SET_COMMAND_EXCEPTION);
            mqttMgr.sendOperationResponse(mqttTopic, operationResponsePayload);
          }
        } else {
          Logger.LOG_WARN(RESPONSE_UNABLE_SET_COMMAND_FORMAT);
          String operationResponsePayload =
              CConnectorApiMessageBuilder.setOperationToFailed_502(
                  CUMULOCITY_RUN_COMMAND_OPERATION_ID, RESPONSE_UNABLE_SET_COMMAND_FORMAT);
          mqttMgr.sendOperationResponse(mqttTopic, operationResponsePayload);
        }
      } else if (command.startsWith("measurements")) {
        try {
          String measurementEnableFlag = (String) commandParts.get(1);
          TagControl measurementEnableTag =
              CConnectorMain.getConnectorMeasurementEnableControlTag();
          if (measurementEnableTag != null) {
            if (measurementEnableFlag.equalsIgnoreCase("enable")
                || measurementEnableFlag.equalsIgnoreCase("disable")) {
              measurementEnableTag.setTagValueAsInt(
                  measurementEnableFlag.equalsIgnoreCase("enable") ? 1 : 0);
              String operationResponsePayload =
                  CConnectorApiMessageBuilder.setOperationToSuccessful_503(
                      CUMULOCITY_RUN_COMMAND_OPERATION_ID, new String[] {RESPONSE_SUCCESS});
              mqttMgr.sendOperationResponse(mqttTopic, operationResponsePayload);
            } else {
              Logger.LOG_WARN(RESPONSE_UNABLE_SET_MEASUREMENTS_COMMAND_FORMAT);
              String operationResponsePayload =
                  CConnectorApiMessageBuilder.setOperationToFailed_502(
                      CUMULOCITY_RUN_COMMAND_OPERATION_ID,
                      RESPONSE_UNABLE_SET_MEASUREMENTS_COMMAND_FORMAT);
              mqttMgr.sendOperationResponse(mqttTopic, operationResponsePayload);
            }

          } else {
            Logger.LOG_WARN(RESPONSE_UNABLE_SET_MEASUREMENTS_COMMAND_NO_TAG_CTRL);
            String operationResponsePayload =
                CConnectorApiMessageBuilder.setOperationToFailed_502(
                    CUMULOCITY_RUN_COMMAND_OPERATION_ID,
                    RESPONSE_UNABLE_SET_MEASUREMENTS_COMMAND_NO_TAG_CTRL);
            mqttMgr.sendOperationResponse(mqttTopic, operationResponsePayload);
          }
        } catch (Exception e) {
          Logger.LOG_WARN(RESPONSE_UNABLE_SET_MEASUREMENTS_COMMAND_EXCEPTION);
          String operationResponsePayload =
              CConnectorApiMessageBuilder.setOperationToFailed_502(
                  CUMULOCITY_RUN_COMMAND_OPERATION_ID,
                  RESPONSE_UNABLE_SET_MEASUREMENTS_COMMAND_EXCEPTION);
          mqttMgr.sendOperationResponse(mqttTopic, operationResponsePayload);
        }
      } else {
        String operationResponsePayload =
            CConnectorApiMessageBuilder.setOperationToFailed_502(
                CUMULOCITY_RUN_COMMAND_OPERATION_ID, RESPONSE_COMMAND_UNKNOWN_NOT_SUPPORTED);
        mqttMgr.sendOperationResponse(mqttTopic, operationResponsePayload);
      }
    } else {
      String operationResponsePayload =
          CConnectorApiMessageBuilder.setOperationToFailed_502(
              CUMULOCITY_RUN_COMMAND_OPERATION_ID, RESPONSE_DEVICE_ID_MISMATCH);
      mqttMgr.sendOperationResponse(mqttTopic, operationResponsePayload);
    }
  }

  /**
   * Gets a boolean indicating if the specified message is a valid 513/setConfiguration device
   * message.
   *
   * @param message message to be checked
   * @return true if the specified message is a valid 513/setConfiguration device message, false
   *     otherwise
   */
  public static boolean isSetConfiguration_513(String message) {
    return message.startsWith("513");
  }

  /**
   * Parses a specified 513/setConfiguration device message and returns a boolean indicating if the
   * device ID matches.
   *
   * @param mqttMgr MQTT manager that received the message
   * @param mqttTopic MQTT topic where the message was received
   * @param message 513/setConfiguration device message to parse
   * @param expectedDeviceId expected ID of the device to be configured
   */
  public static void setConfiguration_513(
      CConnectorMqttMgr mqttMgr, String mqttTopic, String message, String expectedDeviceId) {
    // Update state to executing
    String operationResponsePayloadExecuting =
        CConnectorApiMessageBuilder.setOperationToExecuting_501(
            CUMULOCITY_CONFIGURATION_OPERATION_ID);
    mqttMgr.sendOperationResponse(mqttTopic, operationResponsePayloadExecuting);

    // Extract device ID and escaped config file string from message
    final int deviceIdIndex = 1;
    final int escapedConfigFileStringIndex = 2;
    List parts = StringUtils.split(message, ",");
    String deviceId = (String) parts.get(deviceIdIndex);
    String escapedConfigFileString = (String) parts.get(escapedConfigFileStringIndex);

    // Parse configuration file string
    boolean deviceIdMatches = deviceId.equals(expectedDeviceId);
    if (deviceIdMatches) {
      try {
        CConnectorMain.getConnectorConfig().parseConfigFileEscapedString(escapedConfigFileString);

        // Create temporary file to store in progress configuration reboot operation information
        try {
          FileAccessManager.writeStringToFile(TEMPORARY_CONFIG_RESTART_FILE_PATH, mqttTopic);
          CConnectorMain.shutdownAndRestartConnector();
        } catch (IOException e) {
          Logger.LOG_CRITICAL(RESPONSE_TEMPORARY_FILE_CREATE_ERROR);
          Logger.LOG_EXCEPTION(e);
          String operationResponsePayload =
              CConnectorApiMessageBuilder.setOperationToFailed_502(
                  CUMULOCITY_CONFIGURATION_OPERATION_ID, RESPONSE_TEMPORARY_FILE_CREATE_ERROR);
          mqttMgr.sendOperationResponse(mqttTopic, operationResponsePayload);
        }
      } catch (JSONException e) {
        String operationResponsePayload =
            CConnectorApiMessageBuilder.setOperationToFailed_502(
                CUMULOCITY_CONFIGURATION_OPERATION_ID, RESPONSE_CONFIG_FILE_PARSE_ERROR);
        mqttMgr.sendOperationResponse(mqttTopic, operationResponsePayload);
      }
    } else {
      String operationResponsePayload =
          CConnectorApiMessageBuilder.setOperationToFailed_502(
              CUMULOCITY_CONFIGURATION_OPERATION_ID, RESPONSE_DEVICE_ID_MISMATCH);
      mqttMgr.sendOperationResponse(mqttTopic, operationResponsePayload);
    }
  }

  /**
   * Gets a boolean indicating if the specified message is a valid 515/installFirmware device
   * message.
   *
   * @param message message to be checked
   * @return true if the specified message is a valid 515/installFirmware device message, false
   *     otherwise
   */
  public static boolean isInstallFirmware_515(String message) {
    return message.startsWith("515");
  }

  /**
   * Parses a specified 515/installFirmware device message and returns a boolean indicating if the
   * device ID matches.
   *
   * @param mqttMgr MQTT manager that received the message
   * @param mqttTopic MQTT topic where the message was received
   * @param message 515/installFirmware device message to parse
   * @param expectedDeviceId expected ID of the device to be updated
   */
  public static void installFirmware_515(
      CConnectorMqttMgr mqttMgr, String mqttTopic, String message, String expectedDeviceId) {
    // Update state to executing
    String operationResponsePayloadExecuting =
        CConnectorApiMessageBuilder.setOperationToExecuting_501(CUMULOCITY_FIRMWARE_OPERATION_ID);
    mqttMgr.sendOperationResponse(mqttTopic, operationResponsePayloadExecuting);

    // Extract message parts
    final int deviceIdIndex = 1;
    final int firmwareNameIndex = 2;
    final int firmwareVersionIndex = 3;
    final int firmwareUrlIndex = 4;
    List parts = StringUtils.split(message, ",");
    String deviceId = (String) parts.get(deviceIdIndex);
    String firmwareName = (String) parts.get(firmwareNameIndex);
    String firmwareVersion = (String) parts.get(firmwareVersionIndex);
    String firmwareUrl = (String) parts.get(firmwareUrlIndex);

    // Perform firmware update if device ID matches
    boolean deviceIdMatches = deviceId.equals(expectedDeviceId);
    if (deviceIdMatches) {
      // Create temporary file to store in progress firmware reboot operation information
      try {
        FileAccessManager.writeStringToFile(TEMPORARY_FIRMWARE_RESTART_FILE_PATH, mqttTopic);
      } catch (IOException e) {
        Logger.LOG_CRITICAL(RESPONSE_TEMPORARY_FILE_CREATE_ERROR);
        Logger.LOG_EXCEPTION(e);
        String operationResponsePayload =
            CConnectorApiMessageBuilder.setOperationToFailed_502(
                CUMULOCITY_FIRMWARE_OPERATION_ID, RESPONSE_TEMPORARY_FILE_CREATE_ERROR);
        mqttMgr.sendOperationResponse(mqttTopic, operationResponsePayload);
      }

      // Download firmware file
      Logger.LOG_SERIOUS(
          "Downloading new firmware ("
              + firmwareName
              + ", "
              + firmwareVersion
              + ") from "
              + firmwareUrl);
      try {
        /* Perform firmware download and if unsuccessful, store the returned result. If successful,
        the method does not return because the application will be terminated. */
        String result = downloadFirmwareFile(firmwareUrl);

        // Update state to failed with result (device reboots instead of returning when successful)
        String operationResponsePayload =
            CConnectorApiMessageBuilder.setOperationToFailed_502(
                CUMULOCITY_FIRMWARE_OPERATION_ID, result);
        mqttMgr.sendOperationResponse(mqttTopic, operationResponsePayload);
        Logger.LOG_SERIOUS(result);
      } catch (Exception e) {
        String operationResponsePayload =
            CConnectorApiMessageBuilder.setOperationToFailed_502(
                CUMULOCITY_FIRMWARE_OPERATION_ID, RESPONSE_DOWNLOAD_FIRMWARE_EXCEPTION);
        mqttMgr.sendOperationResponse(mqttTopic, operationResponsePayload);
        Logger.LOG_SERIOUS(
            "An exception occurred while downloading a firmware file from Cumulocity!");
        Logger.LOG_EXCEPTION(e);

        // Delete firmware restart file
        File tempFirmwareRestartFile = new File(TEMPORARY_FIRMWARE_RESTART_FILE_PATH);
        if (tempFirmwareRestartFile.exists()) {
          tempFirmwareRestartFile.delete();
        }
      }
    } else {
      String operationResponsePayload =
          CConnectorApiMessageBuilder.setOperationToFailed_502(
              CUMULOCITY_FIRMWARE_OPERATION_ID, RESPONSE_DEVICE_ID_MISMATCH);
      mqttMgr.sendOperationResponse(mqttTopic, operationResponsePayload);
    }
  }

  /**
   * Not yet implemented (Unavailable).
   *
   * @param mqttMgr Not yet implemented (Unavailable)
   * @param mqttTopic Not yet implemented (Unavailable)
   * @param message Not yet implemented (Unavailable)
   * @param expectedDeviceId Not yet implemented (Unavailable)
   * @return Not yet implemented (Unavailable)
   */
  public static boolean installSoftwareList_516(
      CConnectorMqttMgr mqttMgr, String mqttTopic, String message, String expectedDeviceId) {
    return false;
  }

  /**
   * Not yet implemented (Unavailable).
   *
   * @param mqttMgr Not yet implemented (Unavailable)
   * @param mqttTopic Not yet implemented (Unavailable)
   * @param message Not yet implemented (Unavailable)
   * @param expectedDeviceId Not yet implemented (Unavailable)
   * @return Not yet implemented (Unavailable)
   */
  public static boolean measurementRequestOperation_517(
      CConnectorMqttMgr mqttMgr, String mqttTopic, String message, String expectedDeviceId) {
    return false;
  }

  /**
   * Not yet implemented (Unavailable).
   *
   * @param mqttMgr Not yet implemented (Unavailable)
   * @param mqttTopic Not yet implemented (Unavailable)
   * @param message Not yet implemented (Unavailable)
   * @param expectedDeviceId Not yet implemented (Unavailable)
   * @return Not yet implemented (Unavailable)
   */
  public static boolean openCloseRelay_518(
      CConnectorMqttMgr mqttMgr, String mqttTopic, String message, String expectedDeviceId) {
    return false;
  }

  /**
   * Not yet implemented (Unavailable).
   *
   * @param mqttMgr Not yet implemented (Unavailable)
   * @param mqttTopic Not yet implemented (Unavailable)
   * @param message Not yet implemented (Unavailable)
   * @param expectedDeviceId Not yet implemented (Unavailable)
   * @return Not yet implemented (Unavailable)
   */
  public static boolean openCloseRelayArray_519(
      CConnectorMqttMgr mqttMgr, String mqttTopic, String message, String expectedDeviceId) {
    return false;
  }

  /**
   * Not yet implemented (Unavailable).
   *
   * @param mqttMgr Not yet implemented (Unavailable)
   * @param mqttTopic Not yet implemented (Unavailable)
   * @param message Not yet implemented (Unavailable)
   * @param expectedDeviceId Not yet implemented (Unavailable)
   * @return Not yet implemented (Unavailable)
   */
  public static boolean uploadConfigurationFile_520(
      CConnectorMqttMgr mqttMgr, String mqttTopic, String message, String expectedDeviceId) {
    return false;
  }

  /**
   * Not yet implemented (Unavailable).
   *
   * @param mqttMgr Not yet implemented (Unavailable)
   * @param mqttTopic Not yet implemented (Unavailable)
   * @param message Not yet implemented (Unavailable)
   * @param expectedDeviceId Not yet implemented (Unavailable)
   * @return Not yet implemented (Unavailable)
   */
  public static boolean downloadConfigurationFile_521(
      CConnectorMqttMgr mqttMgr, String mqttTopic, String message, String expectedDeviceId) {
    return false;
  }

  /**
   * Not yet implemented (Unavailable).
   *
   * @param mqttMgr Not yet implemented (Unavailable)
   * @param mqttTopic Not yet implemented (Unavailable)
   * @param message Not yet implemented (Unavailable)
   * @param expectedDeviceId Not yet implemented (Unavailable)
   * @return Not yet implemented (Unavailable)
   */
  public static boolean logFileRequest_522(
      CConnectorMqttMgr mqttMgr, String mqttTopic, String message, String expectedDeviceId) {
    return false;
  }

  /**
   * Not yet implemented (Unavailable).
   *
   * @param mqttMgr Not yet implemented (Unavailable)
   * @param mqttTopic Not yet implemented (Unavailable)
   * @param message Not yet implemented (Unavailable)
   * @param expectedDeviceId Not yet implemented (Unavailable)
   * @return Not yet implemented (Unavailable)
   */
  public static boolean changeCommunicationMode_523(
      CConnectorMqttMgr mqttMgr, String mqttTopic, String message, String expectedDeviceId) {
    return false;
  }

  /**
   * Not yet implemented (Unavailable).
   *
   * @param mqttMgr Not yet implemented (Unavailable)
   * @param mqttTopic Not yet implemented (Unavailable)
   * @param message Not yet implemented (Unavailable)
   * @param expectedDeviceId Not yet implemented (Unavailable)
   * @return Not yet implemented (Unavailable)
   */
  public static boolean downloadConfigurationFileWithType_524(
      CConnectorMqttMgr mqttMgr, String mqttTopic, String message, String expectedDeviceId) {
    return false;
  }

  /**
   * Not yet implemented (Unavailable).
   *
   * @param mqttMgr Not yet implemented (Unavailable)
   * @param mqttTopic Not yet implemented (Unavailable)
   * @param message Not yet implemented (Unavailable)
   * @param expectedDeviceId Not yet implemented (Unavailable)
   * @return Not yet implemented (Unavailable)
   */
  public static boolean installFirmwareFromPatch_525(
      CConnectorMqttMgr mqttMgr, String mqttTopic, String message, String expectedDeviceId) {
    return false;
  }

  /**
   * Not yet implemented (Unavailable).
   *
   * @param mqttMgr Not yet implemented (Unavailable)
   * @param mqttTopic Not yet implemented (Unavailable)
   * @param message Not yet implemented (Unavailable)
   * @param expectedDeviceId Not yet implemented (Unavailable)
   * @return Not yet implemented (Unavailable)
   */
  public static boolean uploadConfigurationFileWithType_526(
      CConnectorMqttMgr mqttMgr, String mqttTopic, String message, String expectedDeviceId) {
    return false;
  }

  /**
   * Not yet implemented (Unavailable).
   *
   * @param mqttMgr Not yet implemented (Unavailable)
   * @param mqttTopic Not yet implemented (Unavailable)
   * @param message Not yet implemented (Unavailable)
   * @param expectedDeviceId Not yet implemented (Unavailable)
   * @return Not yet implemented (Unavailable)
   */
  public static boolean setDeviceProfiles_527(
      CConnectorMqttMgr mqttMgr, String mqttTopic, String message, String expectedDeviceId) {
    return false;
  }

  /**
   * Not yet implemented (Unavailable).
   *
   * @param mqttMgr Not yet implemented (Unavailable)
   * @param mqttTopic Not yet implemented (Unavailable)
   * @param message Not yet implemented (Unavailable)
   * @param expectedDeviceId Not yet implemented (Unavailable)
   * @return Not yet implemented (Unavailable)
   */
  public static boolean updateSoftware_528(
      CConnectorMqttMgr mqttMgr, String mqttTopic, String message, String expectedDeviceId) {
    return false;
  }

  /**
   * Not yet implemented (Unavailable).
   *
   * @param mqttMgr Not yet implemented (Unavailable)
   * @param mqttTopic Not yet implemented (Unavailable)
   * @param message Not yet implemented (Unavailable)
   * @param expectedDeviceId Not yet implemented (Unavailable)
   * @return Not yet implemented (Unavailable)
   */
  public static boolean cloudRemoteAccessConnect_530(
      CConnectorMqttMgr mqttMgr, String mqttTopic, String message, String expectedDeviceId) {
    return false;
  }

  /**
   * Downloads the firmware file from the specified remote URL to the specified local file.
   *
   * @param remoteFileUrl The URL of the remote file to download the firmware file from.
   * @return A string indicating the reason for download failure, otherwise, null if successful.
   * @throws EWException if the download fails, see the Ewon event logs.
   */
  private static String downloadFirmwareFile(String remoteFileUrl)
      throws EWException, MalformedURLException, JSONException {
    // Disable application auto-restart otherwise firmware update gets interrupted.
    SCAppManagement.disableAppAutoRestart();

    // Disable app watchdog otherwise firmware update gets interrupted.
    final int watchDogTimeoutDisabled = 0;
    RuntimeControl.configureAppWatchdog(watchDogTimeoutDisabled);

    // Perform GET request to specified URL
    final String urlAuthNotEncoded =
        CConnectorMain.getConnectorConfig().getCumulocityDeviceTenant()
            + "/"
            + CConnectorMain.getConnectorConfig().getCumulocityDeviceUsername()
            + ":"
            + CConnectorMain.getConnectorConfig().getCumulocityDevicePassword();
    final String urlAuthEncoded = Base64.encodeBytes(urlAuthNotEncoded.getBytes());
    final String authHeader = "Authorization=Basic " + urlAuthEncoded;
    int httpStatus =
        ScheduledActionManager.RequestHttpX(
            remoteFileUrl, "GET", authHeader, "", "", "/ewonfwr.edf");

    // Read response contents and return
    String resultString = null;
    if (httpStatus == SCHttpUtility.HTTPX_CODE_EWON_ERROR) {
      resultString =
          "An Ewon error ("
              + httpStatus
              + ") was encountered while attempting to download a firmware file.";
    } else if (httpStatus == SCHttpUtility.HTTPX_CODE_AUTH_ERROR) {
      resultString =
          "An authentication error ("
              + httpStatus
              + ") was encountered while attempting to download a firmware file.";
    } else if (httpStatus == SCHttpUtility.HTTPX_CODE_CONNECTION_ERROR) {
      resultString =
          "A connection error ("
              + httpStatus
              + ") was encountered while attempting to download a firmware file.";
    } else if (httpStatus != SCHttpUtility.HTTPX_CODE_NO_ERROR) {
      resultString =
          "An unknown error ("
              + httpStatus
              + ") was encountered while attempting to download a firmware file.";
    }
    return resultString;
  }
}
