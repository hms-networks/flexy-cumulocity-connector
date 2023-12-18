package com.hms_networks.sc.cumulocity.data;

import com.hms_networks.americas.sc.extensions.json.JSONException;
import com.hms_networks.americas.sc.extensions.json.JSONObject;
import com.hms_networks.americas.sc.extensions.system.time.SCTimeUtils;
import com.hms_networks.americas.sc.extensions.util.RawNumberValueUtils;
import com.hms_networks.sc.cumulocity.CConnectorMain;
import java.util.*;

/**
 * Class for building a JSON data payload for sending data to Cumulocity. This class is used to
 * aggregate data from the historical data queue and build a JSON payload using the configured
 * aggregation method.
 *
 * @since 1.4.0
 * @version 1.0.1
 * @author HMS Networks, MU Americas Solution Center
 */
public class CConnectorJsonDataPayload {

  /**
   * Constant for the key used to store the time field in the JSON data payload.
   *
   * @since 1.0.0
   */
  private static final String KEY_TIME = "time";

  /**
   * Constant for the key used to store the external source field in the JSON data payload.
   *
   * @since 1.0.0
   */
  private static final String KEY_EXTERNAL_SOURCE = "externalSource";

  /**
   * Constant for the key used to store the type field in the JSON data payload.
   *
   * <p>This key is also used as the key for the type field in the external source object in the
   * JSON data payload.
   *
   * @since 1.0.0
   */
  private static final String KEY_TYPE = "type";

  /**
   * Constant for the key used to store the value field in the JSON data payload.
   *
   * @since 1.0.0
   */
  private static final String KEY_VALUE = "value";

  /**
   * Constant for the key used to store the unit field in the JSON data payload.
   *
   * @since 1.0.0
   */
  private static final String KEY_UNIT = "unit";

  /**
   * Constant for the key used to store the external ID field in the external source object in the
   * JSON data payload.
   *
   * @since 1.0.0
   */
  private static final String KEY_EXTERNAL_SOURCE_ID = "externalId";

  /**
   * Constant for the type field value in the external source object which indicates that the data
   * payload is associated with a child device.
   *
   * @since 1.0.0
   */
  private static final String EXTERNAL_SOURCE_TYPE_C8Y_SERIAL = "c8y_Serial";

  /**
   * Constant for the {@link #type} field value used to indicate that the data payload is not
   * associated with a child device.
   *
   * @since 1.0.0
   */
  private static final String TYPE_NONE = "None";

  /**
   * The {@link Date} object representing the timestamp of the data payload. This is the timestamp
   * associated with the data in the payload (usually aggregated).
   *
   * @since 1.0.0
   */
  private final Date time;

  /**
   * The {@link JSONObject} representing the external source information of the data payload. This
   * is the information indicating whether the data is associated with a child device or not.
   *
   * <p>If the data/payload is not associated with a child device, this field is {@code null} and
   * not included in the resulting JSON payload.
   *
   * @since 1.0.0
   */
  private final JSONObject externalSource;

  /**
   * The {@link String} object representing the type of the data payload. This usually is either
   * {@link #TYPE_NONE} or the name of the child device the data is associated with.
   *
   * @since 1.0.0
   */
  private final String type;

  /**
   * The {@link Map} object representing the fragments of the data payload. This is the map of
   * fragment names to the associated fragment data.
   *
   * @since 1.0.0
   */
  private final Map fragments = new HashMap(); // Map<String,Fragment>

  /**
   * Internal class for representing a fragment of data in the data payload. This class is used to
   * store the series of data for a fragment.
   *
   * @since 1.0.0
   */
  public static class Fragment {

    /**
     * The {@link Map} object representing the series of data for the fragment. This is the map of
     * series names to the list of associated series data.
     *
     * <p>The type-parameterized version of this map would be {@code Map<String,List<Series>>}.
     *
     * @since 1.0.0
     */
    private final Map seriesMap = new HashMap(); // Map<String,List<Series>>

    /**
     * Internal class for representing a series of data in a fragment of the data payload. This
     * class is used to store the value and unit of a series.
     *
     * <p>There are methods for accessing the value as a generic {@link Object}, as well as methods
     * for accessing the value as each of the supported primitive types.
     *
     * @since 1.0.0
     */
    public static class Series {

      /**
       * The value of the series.
       *
       * @since 1.0.0
       */
      private final Object value;

      /**
       * The unit of the series (associated with the value).
       *
       * @since 1.0.0
       */
      private final String unit;

      /**
       * The original time of the series data.
       *
       * @since 1.0.0
       */
      private final Date originalTime;

      /**
       * Constructor for a new {@link Series} object with the specified value and unit.
       *
       * <p>The {@code value} or the corresponding primitive type (e.g. {@link Integer}, {@link
       * Double}, etc.).
       *
       * @param value the value of the series
       * @param unit the unit of the series
       * @param originalTime the original time of the series data
       * @throws IllegalArgumentException if the value is not a number
       * @since 1.0.0
       */
      public Series(Object value, String unit, Date originalTime) throws IllegalArgumentException {
        if (!(value instanceof Boolean) && !(value instanceof Number)) {
          throw new IllegalArgumentException("Value must be a number or boolean.");
        }
        this.value = value;
        this.unit = unit;
        this.originalTime = originalTime;
      }

      /**
       * Gets the value of the series as a generic {@link Object}.
       *
       * @return the value of the series as a generic {@link Object}
       * @since 1.0.0
       */
      public synchronized Object getValue() {
        return value;
      }

      /**
       * Gets the unit of the series (associated with the value).
       *
       * @return the unit of the series (associated with the value)
       * @since 1.0.0
       */
      public synchronized String getUnit() {
        return unit;
      }

      /**
       * Gets the original time of the series data.
       *
       * @return the original time of the series data
       * @since 1.0.0
       */
      public synchronized Date getOriginalTime() {
        return originalTime;
      }
    }

    /**
     * Gets the {@link Map} object representing the series of data for the fragment. This is the map
     * of series names to the list of associated series data.
     *
     * <p>The type-parameterized version of this map would be {@code Map<String,List<Series>>}.
     *
     * @return the {@link Map} object representing the series of data for the fragment
     * @since 1.0.0
     */
    public synchronized Map getSeriesMap() {
      return seriesMap;
    }

    /**
     * Adds the specified series to the fragment with the specified name. If there is already a
     * series with the specified name, the series is updated with the new series data.
     *
     * @param seriesName the name of the series
     * @param series the series to add
     * @since 1.0.0
     */
    public synchronized void addSeries(String seriesName, Series series) {
      if (seriesMap.containsKey(seriesName)) {
        List existingSeriesList = (List) seriesMap.get(seriesName); // List<Series>
        existingSeriesList.add(series);
      } else {
        List seriesList = new ArrayList(); // List<Series>
        seriesList.add(series);
        seriesMap.put(seriesName, seriesList);
      }
    }
  }

  /**
   * Constructor for a new {@link CConnectorJsonDataPayload} object with the specified date and type
   * (child device).
   *
   * @param time the {@link Date} object representing the timestamp of the data payload. This is the
   *     timestamp associated with the aggregated data in the payload.
   * @param type the {@link String} object representing the type of the data payload. This usually
   *     is the name of the child device the data is associated with, or {@link #TYPE_NONE} if the
   *     data is not associated with a child device. If {@code null} is specified, {@link
   *     #TYPE_NONE} is used.
   * @throws JSONException if unable to populate the external source information object when the
   *     specified {@code type} value is non-null
   * @since 1.0.0
   */
  public CConnectorJsonDataPayload(Date time, String type) throws JSONException {
    this.time = time;
    this.type = type != null ? type : TYPE_NONE;

    // Populate external source field if the type is not null/none, otherwise set to null
    if (!this.type.equals(TYPE_NONE)) {
      externalSource = new JSONObject();
      // External source ID is main/host device ID, underscore, then the type/child device name
      final String externalSourceId = CConnectorMain.getMqttMgr().getMqttId() + "_" + type;
      externalSource.put(KEY_EXTERNAL_SOURCE_ID, externalSourceId);
      externalSource.put(KEY_TYPE, EXTERNAL_SOURCE_TYPE_C8Y_SERIAL);
    } else {
      externalSource = null;
    }
  }

  /**
   * Gets the {@link Date} object representing the timestamp of the data payload. This is the
   * timestamp associated with the aggregated data in the payload.
   *
   * @return the {@link Date} object representing the timestamp of the data payload
   * @since 1.0.0
   */
  public synchronized Date getTime() {
    return time;
  }

  /**
   * Gets the {@link String} object representing the type of the data payload. This usually is
   * either {@link #TYPE_NONE} or the name of the child device the data is associated with.
   *
   * @return the {@link String} object representing the type of the data payload
   * @since 1.0.0
   */
  public synchronized String getType() {
    return type;
  }

  /**
   * Adds the specified fragment to the data payload with the specified name.
   *
   * @param fragmentName the name of the fragment
   * @param fragment the fragment to add
   * @since 1.0.0
   */
  public synchronized void addFragment(String fragmentName, Fragment fragment) {
    if (fragments.containsKey(fragmentName)) {
      Fragment existingFragment = (Fragment) fragments.get(fragmentName);
      Iterator seriesIterator =
          fragment.getSeriesMap().entrySet().iterator(); // Iterator<Map.Entry<String,Series>>
      while (seriesIterator.hasNext()) {
        // Get series
        Map.Entry seriesEntry = (Map.Entry) seriesIterator.next(); // Map.Entry<String, Series>
        String seriesName = (String) seriesEntry.getKey();
        Fragment.Series series = (Fragment.Series) seriesEntry.getValue();
        existingFragment.addSeries(seriesName, series);
      }
    } else {
      fragments.put(fragmentName, fragment);
    }
  }

  /**
   * Adds the specified fragment to the data payload with the specified fragment name, series name,
   * and {@link Fragment.Series} object.
   *
   * @param fragmentName the name of the fragment
   * @param seriesName the name of the series
   * @param series the series to add
   * @since 1.0.0
   */
  public synchronized void addFragment(
      String fragmentName, String seriesName, Fragment.Series series) {
    if (fragments.containsKey(fragmentName)) {
      Fragment existingFragment = (Fragment) fragments.get(fragmentName);
      existingFragment.addSeries(seriesName, series);
    } else {
      Fragment fragment = new Fragment();
      fragment.addSeries(seriesName, series);
      addFragment(fragmentName, fragment);
    }
  }

  /**
   * Adds the specified fragment to the data payload with the specified fragment name, series name,
   * value, and unit.
   *
   * @param fragmentName the name of the fragment
   * @param seriesName the name of the series
   * @param value the value of the series
   * @param unit the unit of the series
   * @param originalTime the original time of the series data
   * @throws IllegalArgumentException if the value is not a number
   * @since 1.0.0
   */
  public synchronized void addFragment(
      String fragmentName, String seriesName, Object value, String unit, Date originalTime)
      throws IllegalArgumentException {
    addFragment(fragmentName, seriesName, new Fragment.Series(value, unit, originalTime));
  }

  /**
   * Gets the {@link JSONObject} representing the data payload. This is the JSON object
   * representation of the payload that can be sent to Cumulocity.
   *
   * @return the {@link JSONObject} representing the data payload
   * @throws JSONException if an error occurs while building the JSON object
   * @throws Exception if an error occurs while formatting the timestamp
   * @since 1.0.0
   */
  public synchronized JSONObject getJsonObject() throws Exception {
    // Create new JSONObject
    JSONObject jsonObject = new JSONObject();

    // Add time and type
    jsonObject.put(KEY_TIME, SCTimeUtils.getIso8601FormattedTimestampForDate(time));
    jsonObject.put(KEY_TYPE, type);

    // Add external source if not null
    if (externalSource != null) {
      jsonObject.put(KEY_EXTERNAL_SOURCE, externalSource);
    }

    // Get aggregation method from config
    CConnectorAggregationMethod queueDataAggregationMethod =
        CConnectorMain.getConnectorConfig().getQueueDataAggregationMethod();

    // Add fragments
    Iterator fragmentsIterator =
        fragments.entrySet().iterator(); // Iterator<Map.Entry<String,Fragment>>
    while (fragmentsIterator.hasNext()) {
      // Get fragment
      Map.Entry fragmentEntry = (Map.Entry) fragmentsIterator.next(); // Map.Entry<String, Fragment>
      String fragmentName = (String) fragmentEntry.getKey();
      Fragment fragment = (Fragment) fragmentEntry.getValue();

      // Create new fragment JSONObject
      JSONObject fragmentJsonObject = new JSONObject();

      // Add series
      Iterator seriesIterator =
          fragment.getSeriesMap().entrySet().iterator(); // Iterator<Map.Entry<String,List<Series>>>
      while (seriesIterator.hasNext()) {
        // Get series
        Map.Entry seriesEntry =
            (Map.Entry) seriesIterator.next(); // Map.Entry<String, List<Series>>
        String seriesName = (String) seriesEntry.getKey();
        List seriesList = (List) seriesEntry.getValue(); // List<Series>

        // Create series list iterator
        Iterator seriesListIterator = seriesList.iterator(); // Iterator<Series>

        // Create series min/max/first/last/avg objects
        Fragment.Series selectedSeries = (Fragment.Series) seriesList.get(0);
        Object[] seriesValues = new Object[seriesList.size()];
        int seriesValuesIndex = 0;

        // Check for boolean value
        boolean booleanValueDetected = selectedSeries.getValue() instanceof Boolean;

        // Loop through seriesList and find min/max/first/last/avg
        while (seriesListIterator.hasNext()) {
          Fragment.Series seriesListItem = (Fragment.Series) seriesListIterator.next();
          Object currentSeriesValue = seriesListItem.getValue();
          booleanValueDetected = booleanValueDetected || currentSeriesValue instanceof Boolean;

          // Check min (boolean)
          if (queueDataAggregationMethod == CConnectorAggregationMethod.MIN_RECORDED_DATA
              && booleanValueDetected
              && selectedSeries.getValue().equals(Boolean.TRUE)
              && currentSeriesValue.equals(Boolean.FALSE)) {
            selectedSeries = seriesListItem;
            break; // Break because min boolean value found. No need to check other values.
          }
          // Check max (boolean)
          else if (queueDataAggregationMethod == CConnectorAggregationMethod.MAX_RECORDED_DATA
              && booleanValueDetected
              && selectedSeries.getValue().equals(Boolean.FALSE)
              && currentSeriesValue.equals(Boolean.TRUE)) {
            selectedSeries = seriesListItem;
            break; // Break because max boolean value found. No need to check other values.
          }
          // Check min (non-boolean)
          if (queueDataAggregationMethod == CConnectorAggregationMethod.MIN_RECORDED_DATA
              && !booleanValueDetected
              && RawNumberValueUtils.getValueMin(selectedSeries.getValue(), currentSeriesValue)
                  .equals(currentSeriesValue)) {
            selectedSeries = seriesListItem;
          }
          // Check max (non-boolean)
          else if (queueDataAggregationMethod == CConnectorAggregationMethod.MAX_RECORDED_DATA
              && !booleanValueDetected
              && RawNumberValueUtils.getValueMax(selectedSeries.getValue(), currentSeriesValue)
                  .equals(currentSeriesValue)) {
            selectedSeries = seriesListItem;
          }
          // Check first
          else if (queueDataAggregationMethod == CConnectorAggregationMethod.FIRST_RECORDED_DATA
              && seriesListItem.getOriginalTime().before(selectedSeries.getOriginalTime())) {
            selectedSeries = seriesListItem;
          }
          // Check last
          else if (queueDataAggregationMethod == CConnectorAggregationMethod.LAST_RECORDED_DATA
              && seriesListItem.getOriginalTime().after(selectedSeries.getOriginalTime())) {
            selectedSeries = seriesListItem;
          }
          // Check average (boolean)
          else if (queueDataAggregationMethod == CConnectorAggregationMethod.AVERAGE_RECORDED_DATA
              && booleanValueDetected) {
            // Add value to array
            seriesValues[seriesValuesIndex++] = seriesListItem.getValue();

            // Check if last item in list
            if (!seriesListIterator.hasNext()) {
              // Calculate average
              Object valueAvg = getBooleanValueAvg(seriesValues);
              selectedSeries =
                  new Fragment.Series(
                      valueAvg, selectedSeries.getUnit(), selectedSeries.getOriginalTime());
            }
          }
          // Check average (non-boolean)
          else if (queueDataAggregationMethod
              == CConnectorAggregationMethod.AVERAGE_RECORDED_DATA) {
            // Add value to array
            seriesValues[seriesValuesIndex++] = seriesListItem.getValue();

            // Check if last item in list
            if (!seriesListIterator.hasNext()) {
              // Calculate average
              Object valueAvg = RawNumberValueUtils.getValueAvg(seriesValues);
              selectedSeries =
                  new Fragment.Series(
                      valueAvg, selectedSeries.getUnit(), selectedSeries.getOriginalTime());
            }
          }
        }

        // Create new series JSONObject
        JSONObject seriesJsonObject = new JSONObject();

        // Add value and unit
        seriesJsonObject.put(KEY_VALUE, selectedSeries.getValue());
        seriesJsonObject.put(KEY_UNIT, selectedSeries.getUnit());

        // Add series to fragment
        fragmentJsonObject.put(seriesName, seriesJsonObject);
      }

      // Add fragment to payload
      jsonObject.put(fragmentName, fragmentJsonObject);
    }

    // Return resulting JSONObject
    return jsonObject;
  }

  /**
   * Gets the boolean value average from the specified array of values.
   *
   * <p>If the number of true values is greater than or equal to the number of false values, {@link
   * Boolean#TRUE} is returned. Otherwise, {@link Boolean#FALSE} is returned.
   *
   * @param seriesValues the array of values to get the average from
   * @return the boolean value average (most common value)
   * @since 1.0.1
   */
  private static synchronized Object getBooleanValueAvg(Object[] seriesValues) {
    // Count true and false values
    int trueCount = 0;
    int falseCount = 0;
    for (int i = 0; i < seriesValues.length; i++) {
      if (Boolean.TRUE.equals(seriesValues[i])) {
        trueCount++;
      } else if (Boolean.FALSE.equals(seriesValues[i])) {
        falseCount++;
      }
    }

    // Return average (true if true count is greater than or equal to false count, false otherwise)
    return trueCount >= falseCount ? Boolean.TRUE : Boolean.FALSE;
  }

  /**
   * Gets the {@link String} representing the data payload. This is the JSON string representation
   * of the payload that can be sent to Cumulocity.
   *
   * @return the {@link String} representing the data payload
   * @throws JSONException if an error occurs while building the JSON object
   * @throws Exception if an error occurs while formatting the timestamp
   * @since 1.0.0
   */
  public synchronized String getJsonString() throws Exception {
    return getJsonObject().toString();
  }

  /**
   * Gets the {@link String} representing the data payload. This is the JSON string representation
   * of the payload that can be sent to Cumulocity.
   *
   * @param indentFactor the number of spaces to indent each level
   * @return the {@link String} representing the data payload
   * @throws JSONException if an error occurs while building the JSON object
   * @throws Exception if an error occurs while formatting the timestamp
   * @since 1.0.0
   */
  public synchronized String getJsonString(int indentFactor) throws Exception {
    return getJsonObject().toString(indentFactor);
  }
}
