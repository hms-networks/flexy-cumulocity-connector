package com.hmsnetworks.sc.cumulocity.data;

import com.hms_networks.americas.sc.extensions.datapoint.DataPoint;
import com.hms_networks.americas.sc.extensions.datapoint.DataPointString;
import com.hms_networks.americas.sc.extensions.datapoint.DataType;
import com.hms_networks.americas.sc.extensions.historicaldata.HistoricalDataQueueManager;
import com.hms_networks.americas.sc.extensions.logging.Logger;
import com.hms_networks.americas.sc.extensions.mqtt.MqttStatusCode;
import com.hms_networks.americas.sc.extensions.system.time.SCTimeSpan;
import com.hms_networks.americas.sc.extensions.system.time.SCTimeUtils;
import com.hmsnetworks.sc.cumulocity.CConnectorMain;
import com.hmsnetworks.sc.cumulocity.api.CConnectorApiMessageBuilder;
import com.hmsnetworks.sc.cumulocity.api.CConnectorMqttMgr;
import com.hmsnetworks.sc.cumulocity.config.CConnectorConfigFile;
import java.io.IOException;
import java.util.*;

/**
 * Class for managing data from the Ewon's historical data queue corresponding systems.
 *
 * @since 1.0.0
 * @author HMS Networks, MU Americas Solution Center
 */
public class CConnectorDataMgr {

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
      } else if (mqttMgr.getLastKnownMqttStatusCode() != MqttStatusCode.CONNECTED) {
        Logger.LOG_WARN(
            "The MQTT manager is not connected or has an unknown status. Skipping data poll!");
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

          // Check aggregation configuration
          ArrayList datapointsReadFromQueue = null;
          Map datapointsReadFromQueueMap = null;
          if (CConnectorMain.getConnectorConfig().getQueueDataAggregationPeriodSecs()
              != CConnectorConfigFile.QUEUE_DATA_AGGREGATION_PERIOD_SECS_DISABLED) {
            SCTimeSpan aggregationPeriodTimeSpan =
                SCTimeSpan.ofSeconds(
                    CConnectorMain.getConnectorConfig().getQueueDataAggregationPeriodSecs());
            datapointsReadFromQueueMap =
                HistoricalDataQueueManager.getFifoNextSpanDataAllGroups(
                    startNewTimeTracker, aggregationPeriodTimeSpan);

            Logger.LOG_DEBUG(
                "Read "
                    + datapointsReadFromQueueMap.size()
                    + " periods of aggregated data points from "
                    + "the historical log.");
          } else {
            datapointsReadFromQueue =
                HistoricalDataQueueManager.getFifoNextSpanDataAllGroups(startNewTimeTracker);

            Logger.LOG_DEBUG(
                "Read " + datapointsReadFromQueue.size() + " data points from the historical log.");
          }

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

          // Process data points and send
          if (datapointsReadFromQueueMap != null) {
            processDataPointsAndSend(
                mqttMgr, datapointsReadFromQueueMap, currentReadTimestampMillis);
          } else {
            processDataPointsAndSend(mqttMgr, datapointsReadFromQueue, currentReadTimestampMillis);
          }
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
   * Processes the aggregated data points read from the queue and sends them to the MQTT broker.
   * (Parameterized map type: {@code Map<Date, List<DataPoint>>})
   *
   * @param mqttMgr MQTT manager to send data points on
   * @param datapointsReadFromQueueMap map of aggregated data points read from the historical data
   *     queue
   * @param currentReadTimestampMillis timestamp of the current historical data queue read (in
   *     millis)
   * @throws Exception if unable to get the ISO 8601 formatted time stamp for a data point
   */
  public static void processDataPointsAndSend(
      CConnectorMqttMgr mqttMgr, Map datapointsReadFromQueueMap, long currentReadTimestampMillis)
      throws Exception {

    // Send data via MQTT
    if (!datapointsReadFromQueueMap.isEmpty()) {

      // Map of child device name to list of CConnectorJsonDataPayload(s)
      Map childDeviceMessageListMap = new HashMap(); // Map<String, List<CConnectorJsonDataPayload>>

      // List of string data points
      List stringDataPointsList = new ArrayList(); // List<DataPoint>

      // Iterate over each aggregation period timestamp in the map
      Iterator datapointsReadFromQueueMapIterator =
          datapointsReadFromQueueMap
              .entrySet()
              .iterator(); // Iterator<Map.Entry<Date, List<DataPoint>>>
      while (datapointsReadFromQueueMapIterator.hasNext()) {
        Map.Entry datapointsReadFromQueueMapEntry =
            (Map.Entry)
                datapointsReadFromQueueMapIterator.next(); // Map.Entry<Date, List<DataPoint>>
        Date timestamp = (Date) datapointsReadFromQueueMapEntry.getKey();
        List datapoints = (List) datapointsReadFromQueueMapEntry.getValue();

        // Create map of child device name to CConnectorJsonDataPayload
        Map childDeviceMessageMap = new HashMap(); // Map<String, CConnectorJsonDataPayload>

        // Iterate over each data point in the list
        Iterator datapointsIterator = datapoints.iterator(); // Iterator<DataPoint>
        while (datapointsIterator.hasNext()) {
          DataPoint datapoint = (DataPoint) datapointsIterator.next();

          // Get and split tag name of data point
          CConnectorTagName datapointTagName = new CConnectorTagName(datapoint.getTagName());

          // Get data point information
          Object value = datapoint.getValueObject();
          String unit = datapoint.getTagUnit();
          String childDevice = datapointTagName.getChildDevice();
          String fragment = datapointTagName.getFragment();
          String series = datapointTagName.getSeries();
          Date originalTimestamp = datapoint.getTimeStampAsDate();

          // Remove wrapping quotes if present
          final char quoteChar = '"';
          final int firstCharIndex = 0;
          final int secondCharIndex = 1;
          final int lastCharIndex = unit.length() - 1;
          if (lastCharIndex > firstCharIndex
              && unit.charAt(firstCharIndex) == quoteChar
              && unit.charAt(lastCharIndex) == quoteChar) {
            unit = unit.substring(secondCharIndex, lastCharIndex);
          }

          // Check if child device is present in child device message map
          if (childDeviceMessageMap.containsKey(childDevice)) {
            // Child device is present, get existing payload
            CConnectorJsonDataPayload payload =
                (CConnectorJsonDataPayload) childDeviceMessageMap.get(childDevice);

            // Add data point to payload
            if (datapoint instanceof DataPointString) {
              stringDataPointsList.add(datapoint);
            } else {
              payload.addFragment(fragment, series, value, unit, originalTimestamp);
            }
          } else {
            // Child device is not present, create new payload
            CConnectorJsonDataPayload payload =
                new CConnectorJsonDataPayload(timestamp, childDevice);

            // Add data point to payload
            if (datapoint instanceof DataPointString) {
              stringDataPointsList.add(datapoint);
            } else {
              payload.addFragment(fragment, series, value, unit, originalTimestamp);
            }

            // Add payload to child device message map
            childDeviceMessageMap.put(childDevice, payload);
          }

          // Update last update time stamp
          lastUpdateTimestampMillis = currentReadTimestampMillis;
        }

        // Add child device message map to child device message list map
        Iterator childDeviceMessageMapIterator = childDeviceMessageMap.entrySet().iterator();
        while (childDeviceMessageMapIterator.hasNext()) {
          Map.Entry childDeviceMessageMapEntry = (Map.Entry) childDeviceMessageMapIterator.next();
          String childDevice = (String) childDeviceMessageMapEntry.getKey();
          CConnectorJsonDataPayload payload =
              (CConnectorJsonDataPayload) childDeviceMessageMapEntry.getValue();
          if (childDeviceMessageListMap.containsKey(childDevice)) {
            List payloadList = (List) childDeviceMessageListMap.get(childDevice);
            payloadList.add(payload);
          } else {
            List payloadList = new ArrayList();
            payloadList.add(payload);
            childDeviceMessageListMap.put(childDevice, payloadList);
          }
        }
      }

      // Send CConnectorJsonDataPayload(s) to MQTT broker
      Iterator childDeviceMessageListMapIterator = childDeviceMessageListMap.entrySet().iterator();
      while (childDeviceMessageListMapIterator.hasNext()) {
        Map.Entry childDeviceMessageListMapEntry =
            (Map.Entry) childDeviceMessageListMapIterator.next();
        String childDevice = (String) childDeviceMessageListMapEntry.getKey();
        List payloadList = (List) childDeviceMessageListMapEntry.getValue();
        Iterator payloadListIterator = payloadList.iterator();
        while (payloadListIterator.hasNext()) {
          CConnectorJsonDataPayload payload =
              (CConnectorJsonDataPayload) payloadListIterator.next();
          String payloadString = payload.getJsonString();
          try {
            mqttMgr.sendMessageWithChildDeviceRouting(
                payloadString, childDevice, CConnectorMessageType.JSON_DATA);
          } catch (Exception e) {
            Logger.LOG_CRITICAL("Unable to send payload to MQTT broker.");
            Logger.LOG_EXCEPTION(e);
            mqttMgr.addMessageToRetryPending(
                payloadString, childDevice, CConnectorMessageType.JSON_DATA);
          }
        }
      }

      // Send String data points to MQTT broker (standard/non-aggregated)
      processDataPointsAndSend(mqttMgr, stringDataPointsList, currentReadTimestampMillis);
    }
  }

  /**
   * Processes the list of non-aggregated data points read from the queue and sends them to the MQTT
   * broker.
   *
   * @param mqttMgr MQTT manager to send data points on
   * @param datapointsReadFromQueue list of non-aggregated data points read from the historical data
   *     queue
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
        CConnectorTagName datapointTagName = new CConnectorTagName(datapoint.getTagName());

        // Get data point information
        String value = datapoint.getValueString();
        String unit = datapoint.getTagUnit();
        String time = SCTimeUtils.getIso8601FormattedTimestampForDataPoint(datapoint);

        // Build payload string contents
        String payloadString;
        if (datapoint.getType() == DataType.STRING) {
          // Handle strings as a basic event (use filler value if tag value is blank)
          String guardedValue = value.trim().length() > 2 ? value : BLANK_STRING_FILLER_VALUE;
          payloadString =
              CConnectorApiMessageBuilder.createBasicEvent_400(
                  datapointTagName.getFragmentQuoted(), guardedValue, time);
        } else {
          // Handle non-strings as a measurement
          payloadString =
              CConnectorApiMessageBuilder.createCustomMeasurement_200(
                  datapointTagName.getFragmentQuoted(),
                  datapointTagName.getSeries(),
                  value,
                  unit,
                  time);
        }

        // Add to child device message map
        if (childDeviceMessageMap.containsKey(datapointTagName.getChildDevice())) {
          String existingMessage =
              (String) childDeviceMessageMap.get(datapointTagName.getChildDevice());
          childDeviceMessageMap.put(
              datapointTagName.getChildDevice(), existingMessage + "\n" + payloadString);
        } else {
          childDeviceMessageMap.put(datapointTagName.getChildDevice(), payloadString);
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
          mqttMgr.sendMessageWithChildDeviceRouting(
              payloadString, childDevice, CConnectorMessageType.DATA);
        } catch (Exception e) {
          Logger.LOG_CRITICAL("Unable to send data point to MQTT broker.");
          Logger.LOG_EXCEPTION(e);
          mqttMgr.addMessageToRetryPending(payloadString, childDevice, CConnectorMessageType.DATA);
        }
      }
    }
  }
}
