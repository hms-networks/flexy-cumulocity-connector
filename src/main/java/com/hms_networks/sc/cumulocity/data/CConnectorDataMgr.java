package com.hms_networks.sc.cumulocity.data;

import com.hms_networks.americas.sc.extensions.datapoint.DataPoint;
import com.hms_networks.americas.sc.extensions.datapoint.DataType;
import com.hms_networks.americas.sc.extensions.historicaldata.HistoricalDataQueueManager;
import com.hms_networks.americas.sc.extensions.logging.Logger;
import com.hms_networks.americas.sc.extensions.string.StringUtils;
import com.hms_networks.americas.sc.extensions.system.time.SCTimeUtils;
import com.hms_networks.sc.cumulocity.CConnectorMain;
import com.hms_networks.sc.cumulocity.api.CConnectorApiMessageBuilder;
import com.hms_networks.sc.cumulocity.api.CConnectorMqttMgr;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class for managing data from the Ewon's historical data queue corresponding systems.
 *
 * @since 1.0.0
 * @author HMS Networks, MU Americas Solution Center
 */
public class CConnectorDataMgr {

  /** The size of the array with expected tag name components. */
  private static final int SPLIT_TAG_NAME_ARRAY_SIZE = 3;

  /** The delimiter used to split the tag name into components. */
  public static final String SPLIT_TAG_NAME_DELIMITER = "/";

  /** The default Cumulocity data series name if one is not included in the tag name. */
  private static final String DEFAULT_SERIES_VALUE = "0";

  /**
   * The default interval (in milliseconds) to poll the historical data queue in the event it cannot
   * be read from the configuration file.
   */
  public static final long QUEUE_DATA_POLL_INTERVAL_MILLIS_DEFAULT = 10000;

  /** The minimum memory (in bytes) required to perform a poll of the data queue. */
  public static final int QUEUE_DATA_POLL_MIN_MEMORY_BYTES = 5000000;

  /** The time (in milliseconds) that the data queue must be behind by before warning the user. */
  public static final long QUEUE_DATA_POLL_BEHIND_MILLIS_WARN = 300000;

  /** The maximum number of historical data queue poll failures before a reset is triggered. */
  public static final int QUEUE_DATA_POLL_FAILURE_RESET_THRESHOLD = 5;

  /** The filler value used in place of blank strings when reporting data points to Cumulocity. */
  private static final String BLANK_STRING_FILLER_VALUE = "<BLANK TAG VALUE>";

  /**
   * Integer counter variable for tracking the number of consecutive failures of polling the
   * historical data queue.
   */
  private static int queuePollFailCount = 0;

  /** Boolean flag indicating if the application is running out of memory */
  private static boolean isMemoryCurrentlyLow;

  /** Long value used to track the last time the application checked for historical data update. */
  private static long lastUpdateTimestampMillis = 0;

  /**
   * Splits a given tag name in to its expected components (child device, fragment, series).
   *
   * @param tagName tag name to split into expected components
   * @return tag name components (child device, fragment, series)
   */
  public static String[] getSplitTagName(String tagName) {
    // Split the tag name into its component parts
    List tagNameComponents = StringUtils.split(tagName, SPLIT_TAG_NAME_DELIMITER);

    // Build the split tag name array
    String[] splitTagName = new String[SPLIT_TAG_NAME_ARRAY_SIZE];
    switch (tagNameComponents.size()) {
      case 1:
        splitTagName[0] = null;
        splitTagName[1] = (String) tagNameComponents.get(0);
        splitTagName[2] = DEFAULT_SERIES_VALUE;
        break;
      case 2:
        splitTagName[0] = null;
        splitTagName[1] = (String) tagNameComponents.get(0);
        splitTagName[2] = (String) tagNameComponents.get(1);
        break;
      case 3:
        splitTagName[0] = (String) tagNameComponents.get(0);
        splitTagName[1] = (String) tagNameComponents.get(1);
        splitTagName[2] = (String) tagNameComponents.get(2);
        break;
      default:
        // Get last three components of the tag name (truncate additional leading components)
        final int firstComponentIndex = tagNameComponents.size() - 3;
        final int secondComponentIndex = tagNameComponents.size() - 2;
        final int thirdComponentIndex = tagNameComponents.size() - 1;
        splitTagName[0] = (String) tagNameComponents.get(firstComponentIndex);
        splitTagName[1] = (String) tagNameComponents.get(secondComponentIndex);
        splitTagName[2] = (String) tagNameComponents.get(thirdComponentIndex);
        break;
    }
    return splitTagName;
  }

  /**
   * Checks for historical data in the queue and sends any data points to Cumulocity.
   *
   * @param mqttMgr the MQTT manager to send data points on
   */
  public static void checkForHistoricalDataAndSend(CConnectorMqttMgr mqttMgr) {
    // Grab latest data and send via MQTT
    long currentReadTimestampMillis;

    // Store current timestamp
    currentReadTimestampMillis = System.currentTimeMillis();

    // Update available memory variable
    long availableMemoryBytes = Runtime.getRuntime().freeMemory();

    // Get queue data poll interval (millis) from config
    long queueDataPollIntervalMillis = QUEUE_DATA_POLL_INTERVAL_MILLIS_DEFAULT;
    try {
      queueDataPollIntervalMillis =
          CConnectorMain.getConnectorConfig().getQueueDataPollIntervalMillis();
    } catch (Exception e) {
      Logger.LOG_CRITICAL(
          "Error getting historical data queue poll interval from configuration file. Using"
              + " default: "
              + queueDataPollIntervalMillis);
      Logger.LOG_EXCEPTION(e);
    }

    // Refresh data if within time window
    if ((currentReadTimestampMillis - lastUpdateTimestampMillis) >= queueDataPollIntervalMillis) {
      // Check if memory is within permissible range to poll data queue
      if (availableMemoryBytes < QUEUE_DATA_POLL_MIN_MEMORY_BYTES) {
        // Show low memory warning
        Logger.LOG_WARN("Low memory on device, " + (availableMemoryBytes / 1000) + " MB left!");

        // If low memory flag not set, set it and request garbage collection
        if (!isMemoryCurrentlyLow) {
          // Set low memory flag
          isMemoryCurrentlyLow = true;

          // Tell the JVM that it should garbage collect soon
          System.gc();
        }
      } else if (mqttMgr == null) {
        Logger.LOG_WARN(
            "The MQTT manager is not available to send historical data. Skipping data poll!");
      } else {
        // There is enough memory to run, reset memory state variable.
        if (isMemoryCurrentlyLow) {
          isMemoryCurrentlyLow = false;
        }

        // Retrieve data from queue (if required)
        try {
          // Read data points from queue
          final boolean startNewTimeTracker;
          if (HistoricalDataQueueManager.doesTimeTrackerExist()
              && queuePollFailCount < QUEUE_DATA_POLL_FAILURE_RESET_THRESHOLD) {
            startNewTimeTracker = false;
          } else {
            if (queuePollFailCount >= QUEUE_DATA_POLL_FAILURE_RESET_THRESHOLD) {
              Logger.LOG_WARN(
                  "The maximum number of failures to read the historical "
                      + "data queue has been reached ("
                      + QUEUE_DATA_POLL_FAILURE_RESET_THRESHOLD
                      + "). Forcing a new queue time tracker!");
            }
            startNewTimeTracker = true;
          }
          ArrayList datapointsReadFromQueue =
              HistoricalDataQueueManager.getFifoNextSpanDataAllGroups(startNewTimeTracker);

          Logger.LOG_DEBUG(
              "Read " + datapointsReadFromQueue.size() + " data points from the historical log.");

          // Reset failure counter
          queuePollFailCount = 0;

          // Check if queue is behind
          try {
            long queueBehindMillis = HistoricalDataQueueManager.getQueueTimeBehindMillis();
            if (queueBehindMillis >= QUEUE_DATA_POLL_BEHIND_MILLIS_WARN) {
              Logger.LOG_WARN(
                  "The historical data queue is running behind by "
                      + SCTimeUtils.getDayHourMinSecsForMillis(
                          (int) queueBehindMillis, "days", "hours", "minutes", "seconds"));
            }

          } catch (IOException e) {
            Logger.LOG_SERIOUS("Unable to detect if historical data queue is running behind.");
            Logger.LOG_EXCEPTION(e);
          }

          processDataPointsAndSend(mqttMgr, datapointsReadFromQueue, currentReadTimestampMillis);
        } catch (Exception e) {
          Logger.LOG_CRITICAL(
              "An error occurred while reading "
                  + "data from the historical log. (#"
                  + ++queuePollFailCount
                  + ")");
          Logger.LOG_EXCEPTION(e);
        }
      }
    }
  }

  /**
   * Processes the data points read from the queue and sends them to the MQTT broker.
   *
   * @param mqttMgr MQTT manager to send data points on
   * @param datapointsReadFromQueue list of data points read from the historical data queue
   * @param currentReadTimestampMillis timestamp of the current historical data queue read (in
   *     millis)
   * @throws Exception if unable to get the ISO 8601 formatted time stamp for a data point
   */
  public static void processDataPointsAndSend(
      CConnectorMqttMgr mqttMgr, List datapointsReadFromQueue, long currentReadTimestampMillis)
      throws Exception {
    // Send data via MQTT
    if (datapointsReadFromQueue.size() > 0) {
      Map childDeviceMessageMap = new HashMap();
      for (int i = 0; i < datapointsReadFromQueue.size(); i++) {
        DataPoint datapoint = (DataPoint) datapointsReadFromQueue.get(i);

        // Get and split tag name of data point
        String datapointTagName = datapoint.getTagName();
        String[] datapointSplitTagName = getSplitTagName(datapointTagName);

        // Get data point information
        String childDevice = datapointSplitTagName[0];
        String fragment = "\"" + datapointSplitTagName[1] + "\"";
        String series = datapointSplitTagName[2];
        String value = datapoint.getValueString();
        String unit = datapoint.getTagUnit();
        String time = SCTimeUtils.getIso8601FormattedTimestampForDataPoint(datapoint);

        // Build payload string contents
        String payloadString;
        if (datapoint.getType() == DataType.STRING) {
          // Handle strings as a basic event (use filler value if tag value is blank)
          String guardedValue = value.trim().length() > 2 ? value : BLANK_STRING_FILLER_VALUE;
          payloadString =
              CConnectorApiMessageBuilder.createBasicEvent_400(fragment, guardedValue, time);
        } else {
          // Handle non-strings as a measurement
          payloadString =
              CConnectorApiMessageBuilder.createCustomMeasurement_200(
                  fragment, series, value, unit, time);
        }

        // Add to child device message map
        if (childDeviceMessageMap.containsKey(childDevice)) {
          String existingMessage = (String) childDeviceMessageMap.get(childDevice);
          childDeviceMessageMap.put(childDevice, existingMessage + "\n" + payloadString);
        } else {
          childDeviceMessageMap.put(childDevice, payloadString);
        }

        // Update last update time stamp
        lastUpdateTimestampMillis = currentReadTimestampMillis;
      }

      Object[] childDeviceMessageMapKeysArray = childDeviceMessageMap.keySet().toArray();
      for (int x = 0; x < childDeviceMessageMapKeysArray.length; x++) {
        // Send payload with child device name if present
        String childDevice = (String) childDeviceMessageMapKeysArray[x];
        String payloadString = (String) childDeviceMessageMap.get(childDevice);
        try {
          mqttMgr.sendMessageWithChildDeviceRouting(payloadString, childDevice);
        } catch (Exception e) {
          Logger.LOG_CRITICAL("Unable to send data point to MQTT broker.");
          Logger.LOG_EXCEPTION(e);
          mqttMgr.addMessageToRetryPending(payloadString, childDevice);
        }
      }
    }
  }
}
