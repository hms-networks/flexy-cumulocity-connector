package com.hms_networks.sc.cumulocity.api;

import com.hms_networks.americas.sc.extensions.json.JSONException;
import com.hms_networks.americas.sc.extensions.json.JSONObject;

/**
 * Helper class for building MQTT payloads corresponding to the available publish templates on
 * Cumulocity.
 *
 * @author HMS Networks, MU Americas Solution Center
 * @since 1.0.0
 */
public class CConnectorApiMessageBuilder {

  /** The device type used to identify the device in Cumulocity. */
  private static final String DEVICECREATION_100_DEVICE_TYPE = "c8y_MQTTDevice";

  /** The device type used to identify children of this device in Cumulocity. */
  private static final String CHILDDEVICECREATION_101_DEVICE_TYPE = "c8y_MQTTChildDevice";

  // region: Inventory Templates (1xx)

  /**
   * Create a new device for the serial number in the inventory if not yet existing. An externalId
   * for the device with type <code>c8y_Serial</code> and the device identifier of the MQTT clientId
   * as value will be created.
   *
   * @param deviceName The name of the device.
   * @return The payload for the device creation.
   */
  public static String deviceCreation_100(String deviceName) {
    return "100," + deviceName + "," + DEVICECREATION_100_DEVICE_TYPE;
  }

  /**
   * Create a new child device for the current device. The newly created object will be added as
   * child device. Additionally, an externalId for the child will be created with type <code>
   * c8y_Serial</code> and the value a combination of the serial of the root device and the unique
   * child ID.
   *
   * @param uniqueChildId The unique ID of the child device.
   * @param deviceName The name of the child device.
   * @return The payload for the child device creation.
   */
  public static String childDeviceCreation_101(String uniqueChildId, String deviceName) {
    return "101," + uniqueChildId + "," + deviceName + "," + CHILDDEVICECREATION_101_DEVICE_TYPE;
  }

  /**
   * Trigger the sending of child devices of the device.
   *
   * @return The payload for the child device trigger.
   */
  public static String getChildDevices_105() {
    return "105";
  }

  /**
   * Remove one or more fragment(s) from a device.
   *
   * @param fragments The fragment(s) to remove.
   * @return The payload to remove the fragment(s).
   */
  public static String clearDevicesFragment_107(String[] fragments) {
    StringBuffer apiPayload = new StringBuffer("107");
    for (int fragment = 0; fragment < fragments.length; fragment++) {
      apiPayload.append(",").append(fragments[fragment]);
    }

    return apiPayload.toString();
  }

  /**
   * Update the hardware properties of the device.
   *
   * @param serialNumber The serial number of the device.
   * @param model The model of the device.
   * @param revision The revision of the device.
   * @return The payload for the hardware properties update.
   */
  public static String configureHardware_110(String serialNumber, String model, String revision) {
    return "110," + serialNumber + "," + model + "," + revision;
  }

  /**
   * Update the mobile properties of the device.
   *
   * @param imei The IMEI of the device.
   * @param iccid The ICCID of the device.
   * @param imsi The IMSI of the device.
   * @param mcc The MCC of the device.
   * @param mnc The MNC of the device.
   * @param lac The LAC of the device.
   * @param cellid The cell ID of the device.
   * @return The payload for the mobile properties update.
   */
  public static String configureMobile_111(
      String imei, String iccid, String imsi, String mcc, String mnc, String lac, String cellid) {
    return "111," + imei + "," + iccid + "," + imsi + "," + mcc + "," + mnc + "," + lac + ","
        + cellid;
  }

  /**
   * Update the position properties of the device.
   *
   * @param latitude The latitude of the device.
   * @param longitude The longitude of the device.
   * @param altitude The altitude of the device.
   * @param accuracy The accuracy of the device.
   * @return The payload for the position properties update.
   */
  public static String configurePosition_112(
      String latitude, String longitude, String altitude, String accuracy) {
    return "112," + latitude + "," + longitude + "," + altitude + "," + accuracy;
  }

  /**
   * Update the configuration properties of the device.
   *
   * @param configuration The configuration of the device.
   * @return The payload for the configuration properties update.
   */
  public static String setConfiguration_113(String configuration) {
    return "113,\"" + configuration + "\"";
  }

  /**
   * Set the supported operations of the device.
   *
   * @param supportedOperations List of supported operations.
   * @return The payload for the supported operations update.
   */
  public static String setSupportedOperations_114(String[] supportedOperations) {
    StringBuffer apiPayload = new StringBuffer("114");
    for (int supportedOperation = 0;
        supportedOperation < supportedOperations.length;
        supportedOperation++) {
      apiPayload.append(",").append(supportedOperations[supportedOperation]);
    }

    return apiPayload.toString();
  }

  /**
   * Set the firmware installed on the device.
   *
   * @param name The name of the firmware.
   * @param version The version of the firmware.
   * @param url The URL of the firmware.
   * @return The payload for the firmware information update.
   */
  public static String setFirmware_115(String name, String version, String url) {
    return "115," + name + "," + version + "," + url;
  }

  /**
   * Static utility class used for storing information about an installed software application.
   *
   * @author HMS Networks, MU Americas Solution Center
   * @since 1.0.0
   */
  public static class InstalledSoftware {

    /** The name of the installed software. */
    public final String name;

    /** The version of the installed software. */
    public final String version;

    /** The URL of the installed software. */
    public final String url;

    /**
     * Constructor for the {@link InstalledSoftware} class with specified software name, version,
     * and URL.
     *
     * @param name The name of the installed software.
     * @param version The version of the installed software.
     * @param url The URL of the installed software.
     */
    public InstalledSoftware(String name, String version, String url) {
      this.name = name;
      this.version = version;
      this.url = url;
    }
  }

  /**
   * Set the list of software installed on the device.
   *
   * @param softwareList The list of software installed on the device.
   * @return The payload for the installed software list update.
   */
  public static String setSoftwareList_116(InstalledSoftware[] softwareList) {
    StringBuffer apiPayload = new StringBuffer("116");
    for (int softwareListIndex = 0; softwareListIndex < softwareList.length; softwareListIndex++) {
      apiPayload.append(",").append(softwareList[softwareListIndex].name);
      apiPayload.append(",").append(softwareList[softwareListIndex].version);
      apiPayload.append(",").append(softwareList[softwareListIndex].url);
    }

    return apiPayload.toString();
  }

  /**
   * Set the required interval for availability monitoring as an integer value representing minutes.
   * This will only set the value if it does not exist. Values entered, e.g. through the UI, are not
   * overwritten.
   *
   * @param requiredIntervalMinutes required interval for availability monitoring in minutes.
   * @return The payload for setting the required availability interval.
   */
  public static String setRequiredAvailability_117(int requiredIntervalMinutes) {
    return "117," + requiredIntervalMinutes;
  }

  /**
   * Set the supported log(s) of the device.
   *
   * @param supportedLogs List of supported log(s).
   * @return The payload for the supported log(s) update.
   */
  public static String setSupportedLogs_118(String[] supportedLogs) {
    StringBuffer apiPayload = new StringBuffer("118");
    for (int supportedLog = 0; supportedLog < supportedLogs.length; supportedLog++) {
      apiPayload.append(",").append(supportedLogs[supportedLog]);
    }

    return apiPayload.toString();
  }

  /**
   * Set the supported configuration(s) of the device.
   *
   * @param supportedConfigurations List of supported configuration(s).
   * @return The payload for the supported configuration(s) update.
   */
  public static String setSupportedConfigurations_119(String[] supportedConfigurations) {
    StringBuffer apiPayload = new StringBuffer("119");
    for (int supportedConfiguration = 0;
        supportedConfiguration < supportedConfigurations.length;
        supportedConfiguration++) {
      apiPayload.append(",").append(supportedConfigurations[supportedConfiguration]);
    }

    return apiPayload.toString();
  }

  /**
   * Set currently installed configuration of the device.
   *
   * @param configurationType The currently installed configuration type.
   * @param configurationFileDownloadUrl The URL to download the currently installed configuration.
   * @param fileName The name of the currently installed configuration file.
   * @param configurationApplyDateTime The date and time the currently installed configuration was
   *     applied.
   * @return The payload for the currently installed configuration update.
   */
  public static String setCurrentlyInstalledConfiguration_120(
      String configurationType,
      String configurationFileDownloadUrl,
      String fileName,
      String configurationApplyDateTime) {
    return "120,"
        + configurationType
        + ","
        + configurationFileDownloadUrl
        + ","
        + fileName
        + ","
        + configurationApplyDateTime;
  }

  /**
   * Set device profile that is being applied to the device.
   *
   * @param profileExecuted The device profile that is being applied to the device.
   * @param profileId The ID of the device profile that is being applied to the device.
   * @return The payload for the device profile update.
   */
  public static String setDeviceProfileBeingApplied_121(String profileExecuted, int profileId) {
    return "121," + profileExecuted + "," + profileId;
  }

  // endregion
  // region: Measurement Templates (2xx)

  /**
   * Create a measurement with a given fragment and series.
   *
   * @param fragment The fragment of the measurement.
   * @param series The series of the measurement.
   * @param value The value of the measurement.
   * @param unit The unit of the measurement.
   * @param time The time of the measurement.
   * @return The payload for the custom measurement creation.
   */
  public static String createCustomMeasurement_200(
      String fragment, String series, String value, String unit, String time) {
    // Create required payload sections
    String payload = "200," + fragment + "," + series + "," + value;

    // Add optional payload sections
    if (unit != null) {
      payload += "," + unit;
    }
    if (unit != null && time != null) {
      payload += "," + time;
    }

    return payload;
  }

  /**
   * Create a measurement of type <code>c8y_SignalStrength</code>.
   *
   * @param rssiValue The RSSI value of the signal strength. (Mandatory if <code>berValue</code> is
   *     not set, otherwise, may be empty string).
   * @param berValue The BER value of the signal strength. (Mandatory if <code>rssiValue</code> is
   *     not set, otherwise, may be empty string).
   * @param time The time of the measurement.
   * @return The payload for the signal strength measurement creation.
   */
  public static String createSignalStrengthMeasurement_210(
      String rssiValue, String berValue, String time) {
    return "210," + rssiValue + "," + berValue + "," + time;
  }

  /**
   * Create a measurement of type <code>c8y_TemperatureMeasurement</code>.
   *
   * @param temperatureValue The temperature value of the measurement.
   * @param time The time of the measurement.
   * @return The payload for the temperature measurement creation.
   */
  public static String createTemperatureMeasurement_211(String temperatureValue, String time) {
    return "211," + temperatureValue + "," + time;
  }

  /**
   * Create a measurement of type <code>c8y_Battery</code>.
   *
   * @param batteryValue The battery value of the measurement.
   * @param time The time of the measurement.
   * @return The payload for the battery measurement creation.
   */
  public static String createBatteryMeasurement_212(String batteryValue, String time) {
    return "212," + batteryValue + "," + time;
  }

  // endregion
  // region: Alarm Templates (3xx)

  /**
   * Create an alarm with specified alarm level.
   *
   * @param level The level of the alarm.
   * @param type The type (fragment/series) of the alarm.
   * @param text The text (hint) of the alarm.
   * @param time The time of the alarm.
   * @return The payload for the alarm creation.
   */
  private static String createAlarm(String level, String type, String text, String time) {
    return level + "," + type + ",\"" + text + "\"," + time;
  }

  /**
   * Create a critical alarm.
   *
   * @param type The type (fragment/series) of the alarm.
   * @param text The text (hint) of the alarm.
   * @param time The time of the alarm.
   * @return The payload for the critical alarm creation.
   */
  public static String createCriticalAlarm_301(String type, String text, String time) {
    return createAlarm("301", type, text, time);
  }

  /**
   * Create a major alarm.
   *
   * @param type The type (fragment/series) of the alarm.
   * @param text The text (hint) of the alarm.
   * @param time The time of the alarm.
   * @return The payload for the major alarm creation.
   */
  public static String createMajorAlarm_302(String type, String text, String time) {
    return createAlarm("302", type, text, time);
  }

  /**
   * Create a minor alarm.
   *
   * @param type The type (fragment/series) of the alarm.
   * @param text The text (hint) of the alarm.
   * @param time The time of the alarm.
   * @return The payload for the minor alarm creation.
   */
  public static String createMinorAlarm_303(String type, String text, String time) {
    return createAlarm("303", type, text, time);
  }

  /**
   * Create a warning alarm.
   *
   * @param type The type (fragment/series) of the alarm.
   * @param text The text (hint) of the alarm.
   * @param time The time of the alarm.
   * @return The payload for the warning alarm creation.
   */
  public static String createWarningAlarm_304(String type, String text, String time) {
    return createAlarm("304", type, text, time);
  }

  /**
   * Change the severity of an existing alarm.
   *
   * @param type The type (fragment/series) of the alarm.
   * @param severity The severity of the alarm (i.e. CRITICAL, WARNING, etc.).
   * @return The payload for the alarm severity change.
   */
  public static String updateSeverityExistingAlarm_305(String type, String severity) {
    return "305," + type + "," + severity;
  }

  /**
   * Clear an existing alarm.
   *
   * @param type The type (fragment/series) of the alarm.
   * @return The payload for the alarm clear.
   */
  public static String clearExistingAlarm_306(String type) {
    return "306," + type;
  }

  /**
   * Remove one or more fragment(s) from an alarm of a specific type.
   *
   * @param alarmType The type (fragment/series) of the alarm.
   * @param fragmentNames The names of the fragment(s) to remove.
   * @return The payload for the alarm fragment removal.
   */
  public static String clearAlarmsFragment_307(String alarmType, String[] fragmentNames) {
    StringBuffer apiPayload = new StringBuffer("307,").append(alarmType);
    for (int fragmentName = 0; fragmentName < fragmentNames.length; fragmentName++) {
      apiPayload.append(",").append(fragmentNames[fragmentName]);
    }

    return apiPayload.toString();
  }

  // endregion
  // region: Event Templates (4xx)

  /**
   * Create an event of given type and text.
   *
   * @param type The type of the event.
   * @param text The text of the event.
   * @param time The time of the event.
   * @return The payload for the event creation.
   */
  public static String createBasicEvent_400(String type, String text, String time) {
    return "400," + type + "," + text + "," + time;
  }

  /**
   * Create typical location update event containing <code>c8y_Position</code>.
   *
   * @param latitude The latitude of the position.
   * @param longitude The longitude of the position.
   * @param altitude The altitude of the position.
   * @param accuracy The accuracy of the position.
   * @param time The time of the position.
   * @return The payload for the location update event creation.
   */
  public static String createLocationUpdateEvent_401(
      String latitude, String longitude, String altitude, String accuracy, String time) {
    return "401," + latitude + "," + longitude + "," + altitude + "," + accuracy + "," + time;
  }

  /**
   * Create typical location update event containing <code>c8y_Position</code>. Additionally, the
   * device will be updated with the same <code>c8y_Position</code> fragment.
   *
   * @param latitude The latitude of the position.
   * @param longitude The longitude of the position.
   * @param altitude The altitude of the position.
   * @param accuracy The accuracy of the position.
   * @param time The time of the position.
   * @return The payload for the location update event creation with device update.
   */
  public static String createLocationUpdateEventWithDeviceUpdate_402(
      String latitude, String longitude, String altitude, String accuracy, String time) {
    return "402," + latitude + "," + longitude + "," + altitude + "," + accuracy + "," + time;
  }

  /**
   * Remove one or more fragment(s) from an event of a specific type.
   *
   * @param eventType The type of the event.
   * @param fragmentNames The names of the fragment(s) to remove.
   * @return The payload for the event fragment removal.
   */
  public static String clearEventsFragment_407(String eventType, String[] fragmentNames) {
    StringBuffer apiPayload = new StringBuffer("407,").append(eventType);
    for (int fragmentName = 0; fragmentName < fragmentNames.length; fragmentName++) {
      apiPayload.append(",").append(fragmentNames[fragmentName]);
    }

    return apiPayload.toString();
  }

  // endregion
  // region: Operation Templates (5xx)

  /**
   * Trigger the sending of all PENDING operation(s) for the agent.
   *
   * @return The payload for the get PENDING operation(s) trigger.
   */
  public static String getPendingOperations_500() {
    return "500";
  }

  /**
   * Set the oldest PENDING operation with given fragment to EXECUTING.
   *
   * @param fragment The fragment of the operation.
   * @return The payload for the set operation to EXECUTING.
   */
  public static String setOperationToExecuting_501(String fragment) {
    return "501," + fragment;
  }

  /**
   * Set the oldest PENDING operation with given fragment to FAILED.
   *
   * @param fragment The fragment of the operation.
   * @param failureReason The reason for the failure.
   * @return The payload for the set operation to FAILED.
   */
  public static String setOperationToFailed_502(String fragment, String failureReason) {
    return "502," + fragment + ",\"" + failureReason + "\"";
  }

  /**
   * Set the oldest EXECUTING operation with given fragment to SUCCESSFUL. It enables the device to
   * send additional parameters that trigger additional steps based on the type of operation sent as
   * fragment.
   *
   * @param fragment The fragment of the operation.
   * @return The payload for the set operation to SUCCESSFUL.
   */
  public static String setOperationToSuccessful_503(String fragment) {
    return setOperationToSuccessful_503(fragment, null);
  }

  /**
   * Set the oldest EXECUTING operation with given fragment to SUCCESSFUL. It enables the device to
   * send additional parameters that trigger additional steps based on the type of operation sent as
   * fragment.
   *
   * @param fragment The fragment of the operation.
   * @param parameters The additional parameters to send.
   * @return The payload for the set operation to SUCCESSFUL.
   */
  public static String setOperationToSuccessful_503(String fragment, String[] parameters) {
    StringBuffer apiPayload = new StringBuffer("503,").append(fragment);
    if (parameters != null) {
      for (int parameter = 0; parameter < parameters.length; parameter++) {
        apiPayload.append(",").append(parameters[parameter]);
      }
    }

    return apiPayload.toString();
  }

  // endregion
  // region: JSON (Non-CSV) Templates

  /**
   * Builds a <code>c8y_Agent</code> information payload with the given agent information.
   *
   * @param name The name of the agent.
   * @param version The version of the agent.
   * @param url The URL of the agent.
   * @return The payload for the agent information update.
   * @throws JSONException If the given information cannot be converted to JSON.
   */
  public static String buildC8YAgentPayload(String name, String version, String url)
      throws JSONException {
    JSONObject rootJsonObject = new JSONObject();
    JSONObject agentJsonObject = new JSONObject();
    agentJsonObject.put("name", name);
    agentJsonObject.put("version", version);
    agentJsonObject.put("url", url);
    rootJsonObject.put("c8y_Agent", agentJsonObject);
    return rootJsonObject.toString();
  }

  // endregion
}
