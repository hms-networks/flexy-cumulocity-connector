package com.hms_networks.sc.cumulocity.data;

import com.ewon.ewonitf.TagControl;
import com.hms_networks.americas.sc.extensions.logging.Logger;

/**
 * This class will read tag values and prepare an update message for Cumulocity's inventory managed
 * object on behalf of the child device. <br>
 * The intended use is as follows: <br>
 *
 * <pre>
 * String payload = new CConnectorChildUpdate("MDHT-20_05").getChildUpdateData();
 * publish("inventory/managedObjects/update/MDHT-20_05",payload)
 * </pre>
 *
 * @since 1.5.0
 * @version 1.0.0
 * @author HMS Networks: Americas
 */
public class CConnectorChildUpdate {

  /** This special token is part of a tag name that denotes a tag that contains object data. */
  private static final String TAG_METADATA_TOKEN = "__METADATA";

  /** Special postfix for tag names that designate hardware model. */
  private static final String TAG_POSTFIX_HARDWARE_MODEL = "HardwareModel";

  /** Special postfix for tag names that designate hardware revision. */
  private static final String TAG_POSTFIX_HARDWARE_REVISION = "HardwareRevision";

  /** Special postfix for tag names that designate hardware serial number. */
  private static final String TAG_POSTFIX_HARDWARE_SERIAL_NUM = "HardwareSerialNumber";

  /** Special postfix for tag names that designate firmware version. */
  private static final String TAG_POSTFIX_FIRMWARE_VERSION = "FirmwareVersion";

  /** Special postfix for tag names that designate firmware name. */
  private static final String TAG_POSTFIX_FIRMWARE_NAME = "FirmwareName";

  /** Special postfix for tag names that designate firmware URL. */
  private static final String TAG_POSTFIX_FIRMWARE_URL = "FirmwareUrl";

  /** JSON key for firmware object. */
  private static final String C8Y_FRAGMENT_FIRMWARE_KEY = "c8y_Firmware";

  /** JSON key for hardware object. */
  private static final String C8Y_FRAGMENT_HARDWARE_KEY = "c8y_Hardware";

  /** JSON key for firmware name. */
  private static final String C8Y_NAME_KEY = "name";

  /** JSON key for firmware version. */
  private static final String C8Y_VERSION_KEY = "version";

  /** JSON key for firmware URL. */
  private static final String C8Y_URL_KEY = "url";

  /** JSON key for hardware model. */
  private static final String C8Y_MODEL_KEY = "model";

  /** JSON key for hardware revision. */
  private static final String C8Y_REVISION_KEY = "revision";

  /** JSON key for hardware serial number . */
  private static final String C8Y_SERIAL_KEY = "serialNumber";

  /** Cumulocity ID for the child - derived from from tag names */
  private String childDeviceId;

  /** Value of the HardwareModel tag. */
  private String hardwareModel = null;

  /** Value of the HardwareVersion tag - used for c8y_Hardware revision key. */
  private String hardwareRevision = null;

  /** Value of the HardwareSerialNumber tag. */
  private String hardwareSerialNum = null;

  /** Value of the FirmwareName tag. */
  private String firmwareName = null;

  /** Value of the FirmwareVersion tag. */
  private String firmwareVersion = null;

  /** Value of the FirmwareUrl tag. */
  private String firmwareUrl = null;

  /** Track the number of tags read. */
  private int numReadTags = 0;

  /**
   * Constructor for the CConnectorChildUpdate class. This constructor will read the tags for the
   * child ID provided.
   *
   * @param childId the id of the child device
   * @since 1.0.0
   */
  public CConnectorChildUpdate(String childId) {
    this.childDeviceId = childId;
    readHardwareTags();
    readFirmwareTags();
  }

  /**
   * Try to read the hardware specific c8y tags.
   *
   * @since 1.0.0
   */
  private void readHardwareTags() {
    hardwareModel = tryGetMetaDataTag(TAG_POSTFIX_HARDWARE_MODEL);
    hardwareRevision = tryGetMetaDataTag(TAG_POSTFIX_HARDWARE_REVISION);
    hardwareSerialNum = tryGetMetaDataTag(TAG_POSTFIX_HARDWARE_SERIAL_NUM);
  }

  /**
   * Try to read the firmware specific c8y tags.
   *
   * @since 1.0.0
   */
  private void readFirmwareTags() {
    firmwareName = tryGetMetaDataTag(TAG_POSTFIX_FIRMWARE_NAME);
    firmwareVersion = tryGetMetaDataTag(TAG_POSTFIX_FIRMWARE_VERSION);
    firmwareUrl = tryGetMetaDataTag(TAG_POSTFIX_FIRMWARE_URL);
  }

  /**
   * Try to read the tag value for the given postfix. If the tag does exist, then return the value
   * as a string.
   *
   * @param postfix the specific tag ending to read
   * @return null, if tag could not be read. Otherwise, the value of the tag as a string.
   * @since 1.0.0
   */
  private String tryGetMetaDataTag(String postfix) {
    try {
      TagControl tc = new TagControl(childDeviceId + "/" + TAG_METADATA_TOKEN + "/" + postfix);
      numReadTags++;
      return tc.getTagValueAsString();
      // if the tag does not exist, it is not an EWException that is thrown as JavaDocs suggests, so
      // here we catch all
    } catch (Exception e) {
      Logger.LOG_WARN(
          "Warning, did not find child metadata tag: "
              + childDeviceId
              + "/"
              + TAG_METADATA_TOKEN
              + "/"
              + postfix,
          e);
    }
    return null;
  }

  /**
   * Get the JSON line fragment for the key and value. If the value is null, then an empty string is
   * returned.
   *
   * @param key the key for the JSON object
   * @param value the value for the JSON object
   * @return string containing the JSON line fragment
   * @since 1.0.0
   */
  private String getJsonLine(String key, String value) {
    if (value != null) {
      return "\"" + key + "\": \"" + value + "\"";
    }
    return "";
  }

  /**
   * Helper method to return JSON when lines potentially could be empty strings. Handles where
   * commas might be. C8y_Firmware and c8y_Hardware objects are expected to have 3 lines.
   *
   * @param key the key for the JSON object
   * @param line1 the first line of the JSON object
   * @param line2 the second line of the JSON object
   * @param line3 the third line of the JSON object
   * @return string containing the JSON object
   * @since 1.0.0
   */
  private String getJsonThreeLineObject(String key, String line1, String line2, String line3) {
    StringBuffer sb = new StringBuffer("\"" + key + "\":{");

    if (line1.length() > 0) {
      sb.append(line1);
    }

    if (line2.length() > 0) {
      if (line1.length() > 0) {
        sb.append(",");
      }
      sb.append(line2);
    }

    if (line3.length() > 0) {
      if (line1.length() > 0 || line2.length() > 0) {
        sb.append(",");
      }
      sb.append(line3);
    }

    sb.append("}");
    return sb.toString();
  }

  /**
   * Get the c8y_Hardware object as a JSON string. This method builds the string from the tag values
   * which should have already been read. <br>
   * example return string:
   * "c8y_Hardware":{"model":"child-model-X","revision":"1.0","serialNumber":"123456"}
   *
   * @return string containing the c8y_Hardware JSON object
   * @since 1.0.0
   */
  private String getC8yHardwareObject() {
    String hwRevision = getJsonLine(C8Y_REVISION_KEY, hardwareRevision);
    String hwModel = getJsonLine(C8Y_MODEL_KEY, hardwareModel);
    String hwSerialNum = getJsonLine(C8Y_SERIAL_KEY, hardwareSerialNum);
    return getJsonThreeLineObject(C8Y_FRAGMENT_HARDWARE_KEY, hwRevision, hwModel, hwSerialNum);
  }

  /**
   * Get the c8y_Firmware object as a JSON string. This method builds the string from the tag values
   * which should have already been read. <br>
   * example return value : "c8y_Firmware ":{"name":"fw build
   * x","url":"http://example.com","version":"1.1"}
   *
   * @return string containing the c8y_Firmware JSON object
   */
  private String getC8yFirmwareObject() {
    String fwName = getJsonLine(C8Y_NAME_KEY, firmwareName);
    String fwUrl = getJsonLine(C8Y_URL_KEY, firmwareUrl);
    String fwVersion = getJsonLine(C8Y_VERSION_KEY, firmwareVersion);
    return getJsonThreeLineObject(C8Y_FRAGMENT_FIRMWARE_KEY, fwName, fwUrl, fwVersion);
  }

  /**
   * Get the c8y_isDevice object as a JSON string. This is necessary for child device to indicate
   * that it is a device.
   *
   * @return string containing the c8y_isDevice JSON object
   * @since 1.0.0
   */
  private String getC8yIsDeviceObject() {
    return "\"c8y_isDevice\":{}";
  }

  /**
   * Get the object update message for the child device.
   *
   * @return string containing the c8y_Hardware and c8y_Firmware objects - the ideal object update
   *     message. Or null if no tags were read.
   * @since 1.0.0
   */
  public String getChildUpdateData() {

    if (numReadTags == 0) {
      return null;
    }

    StringBuffer sb = new StringBuffer("{");
    sb.append(getC8yIsDeviceObject());
    sb.append(",");
    sb.append(getC8yHardwareObject());
    sb.append(",");
    sb.append(getC8yFirmwareObject());
    sb.append("}");
    return sb.toString();
  }
}
