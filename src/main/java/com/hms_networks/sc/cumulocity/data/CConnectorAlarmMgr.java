package com.hms_networks.sc.cumulocity.data;

import com.hms_networks.americas.sc.extensions.alarms.AlarmMonitor;
import com.hms_networks.americas.sc.extensions.logging.Logger;
import com.hms_networks.americas.sc.extensions.string.StringUtils;
import com.hms_networks.americas.sc.extensions.taginfo.TagInfo;
import com.hms_networks.americas.sc.extensions.taginfo.TagInfoManager;
import com.hms_networks.americas.sc.extensions.taginfo.TagType;
import com.hms_networks.sc.cumulocity.CConnectorMain;
import com.hms_networks.sc.cumulocity.api.CConnectorApiMessageBuilder;
import com.hms_networks.sc.cumulocity.api.CConnectorMqttMgr;

/**
 * Class for managing alarms on Ewon tags with alarm monitoring enabled.
 *
 * @author HMS Networks, MU Americas Solution Center
 * @since 1.0.0
 */
public class CConnectorAlarmMgr extends AlarmMonitor {

  /**
   * The alarm hint prefix used to identify alarms which should be identified as critical to
   * Cumulocity.
   */
  private static final String CRITICAL_ALARM_HINT_PREFIX = "Critical:";

  /**
   * The alarm hint prefix used to identify alarms which should be identified as major to
   * Cumulocity.
   */
  private static final String MAJOR_ALARM_HINT_PREFIX = "Major:";

  /**
   * The alarm hint prefix used to identify alarms which should be identified as minor to
   * Cumulocity.
   */
  private static final String MINOR_ALARM_HINT_PREFIX = "Minor:";

  /**
   * The alarm hint prefix used to identify alarms which should be identified as warning to
   * Cumulocity.
   */
  private static final String WARNING_ALARM_HINT_PREFIX = "Warning:";

  /**
   * Method for handling alarm status changes on Ewon tags with alarm monitoring enabled.
   *
   * @param alarmedTagName The name of the tag which triggered the alarm status change.
   * @param alarmedTagId The ID of the tag which triggered the alarm status change.
   * @param alarmedTagType The type of the tag which triggered the alarm status change.
   * @param alarmedTagValue The value of the tag which triggered the alarm status change.
   * @param alarmType The type of the alarm.
   * @param alarmStatus The status of the alarm.
   * @param alarmUtcTimestamp The UTC timestamp of the alarm.
   * @param alarmLocalTimestamp The local timestamp of the alarm.
   */
  public void onTagAlarm(
      String alarmedTagName,
      int alarmedTagId,
      TagType alarmedTagType,
      String alarmedTagValue,
      String alarmType,
      String alarmStatus,
      String alarmUtcTimestamp,
      String alarmLocalTimestamp) {
    // Get alarm hint for alarmed tag ID (without quotes)
    TagInfo tagInfoFromTagId = TagInfoManager.getTagInfoFromTagId(alarmedTagId);
    String unmodifiedAlarmHint =
        tagInfoFromTagId.getAlarmHint().substring(1, tagInfoFromTagId.getAlarmHint().length() - 1);

    Logger.LOG_CRITICAL(
        "ALARM FOR TAG: "
            + alarmedTagName
            + " (ID: "
            + alarmedTagId
            + ", TYPE: "
            + alarmedTagType
            + ", VALUE: "
            + alarmedTagValue
            + ", ALARM TYPE: "
            + alarmType
            + ", ALARM STATUS: "
            + alarmStatus
            + ", ALARM UTC TIMESTAMP: "
            + alarmUtcTimestamp
            + ", ALARM LOCAL TIMESTAMP: "
            + alarmLocalTimestamp
            + ", ALARM HINT: "
            + unmodifiedAlarmHint
            + ")");

    // Get and split tag name of data point
    String[] alarmedSplitTagName = CConnectorDataMgr.getSplitTagName(alarmedTagName);

    // Get data point information
    String childDevice = alarmedSplitTagName[0];
    String fragment = StringUtils.replace(alarmedSplitTagName[1], " ", "_");
    String series = StringUtils.replace(alarmedSplitTagName[2], " ", "_");
    String cumulocityAlarmType = fragment + "_" + series;

    // Pick alarm type (critical, major, etc.) from alarm hint (default to major if no type in hint)
    String alarmMessage;
    if (alarmStatus.equals("END") || alarmStatus.equals("NONE")) {
      alarmMessage = CConnectorApiMessageBuilder.clearExistingAlarm_306(cumulocityAlarmType);
    } else if (unmodifiedAlarmHint.startsWith(CRITICAL_ALARM_HINT_PREFIX)) {
      String alarmHintWithoutPrefix =
          unmodifiedAlarmHint.substring(CRITICAL_ALARM_HINT_PREFIX.length());
      alarmMessage =
          CConnectorApiMessageBuilder.createCriticalAlarm_301(
              cumulocityAlarmType, alarmHintWithoutPrefix, alarmUtcTimestamp);
    } else if (unmodifiedAlarmHint.startsWith(MAJOR_ALARM_HINT_PREFIX)) {
      String alarmHintWithoutPrefix =
          unmodifiedAlarmHint.substring(MAJOR_ALARM_HINT_PREFIX.length());
      alarmMessage =
          CConnectorApiMessageBuilder.createMajorAlarm_302(
              cumulocityAlarmType, alarmHintWithoutPrefix, alarmUtcTimestamp);
    } else if (unmodifiedAlarmHint.startsWith(MINOR_ALARM_HINT_PREFIX)) {
      String alarmHintWithoutPrefix =
          unmodifiedAlarmHint.substring(MINOR_ALARM_HINT_PREFIX.length());
      alarmMessage =
          CConnectorApiMessageBuilder.createMinorAlarm_303(
              cumulocityAlarmType, alarmHintWithoutPrefix, alarmUtcTimestamp);
    } else if (unmodifiedAlarmHint.startsWith(WARNING_ALARM_HINT_PREFIX)) {
      String alarmHintWithoutPrefix =
          unmodifiedAlarmHint.substring(WARNING_ALARM_HINT_PREFIX.length());
      alarmMessage =
          CConnectorApiMessageBuilder.createWarningAlarm_304(
              cumulocityAlarmType, alarmHintWithoutPrefix, alarmUtcTimestamp);
    } else {
      alarmMessage =
          CConnectorApiMessageBuilder.createMajorAlarm_302(
              cumulocityAlarmType, unmodifiedAlarmHint, alarmUtcTimestamp);
    }

    // Get mqtt manager from main class
    CConnectorMqttMgr mqttMgr = CConnectorMain.getMqttMgr();
    if (mqttMgr != null) {
      try {
        mqttMgr.sendMessageWithChildDeviceRouting(alarmMessage, childDevice);
      } catch (Exception e) {
        Logger.LOG_SERIOUS(
            "An alarm was unable to be sent to Cumulocity because of an exception: ["
                + alarmedTagName
                + ": "
                + alarmedTagValue
                + "]");
        Logger.LOG_EXCEPTION(e);
        mqttMgr.addMessageToRetryPending(alarmMessage, childDevice);
      }
    } else {
      Logger.LOG_SERIOUS(
          "An alarm was unable to be sent to Cumulocity because the MQTT manager was not found: ["
              + alarmedTagName
              + ": "
              + alarmedTagValue
              + "]");
    }
  }
}
