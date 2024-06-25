package com.hmsnetworks.sc.cumulocity.data;

/**
 * An enum-like class for Java 1.4 compatibility that provides constants for the data aggregation
 * methods supported by the Flexy Cumulocity connector.
 *
 * @since 1.4.0
 * @version 1.0.0
 * @author HMS Networks, MU Americas Solution Center
 */
public class CConnectorAggregationMethod {

  /**
   * Integer value representing the "last recorded data" aggregation method.
   *
   * @see #LAST_RECORDED_DATA
   * @since 1.0.0
   */
  private static final int ENUM_VAL_LAST_RECORDED_DATA = 0;

  /**
   * Integer value representing the "first recorded data" aggregation method.
   *
   * @see #FIRST_RECORDED_DATA
   * @since 1.0.0
   */
  private static final int ENUM_VAL_FIRST_RECORDED_DATA = 1;

  /**
   * Integer value representing the "minimum recorded data" aggregation method.
   *
   * @see #MIN_RECORDED_DATA
   * @since 1.0.0
   */
  private static final int ENUM_VAL_MIN_RECORDED_DATA = 2;

  /**
   * Integer value representing the "maximum recorded data" aggregation method.
   *
   * @see #MAX_RECORDED_DATA
   * @since 1.0.0
   */
  private static final int ENUM_VAL_MAX_RECORDED_DATA = 3;

  /**
   * Integer value representing the "average recorded data" aggregation method.
   *
   * @see #AVERAGE_RECORDED_DATA
   * @since 1.0.0
   */
  private static final int ENUM_VAL_AVERAGE_RECORDED_DATA = 4;

  /**
   * Constant representing the "last recorded data" aggregation method.
   *
   * @since 1.0.0
   */
  public static final CConnectorAggregationMethod LAST_RECORDED_DATA =
      new CConnectorAggregationMethod(ENUM_VAL_LAST_RECORDED_DATA);

  /**
   * Constant representing the "first recorded data" aggregation method.
   *
   * @since 1.0.0
   */
  public static final CConnectorAggregationMethod FIRST_RECORDED_DATA =
      new CConnectorAggregationMethod(ENUM_VAL_FIRST_RECORDED_DATA);

  /**
   * Constant representing the "minimum recorded data" aggregation method.
   *
   * @since 1.0.0
   */
  public static final CConnectorAggregationMethod MIN_RECORDED_DATA =
      new CConnectorAggregationMethod(ENUM_VAL_MIN_RECORDED_DATA);

  /**
   * Constant representing the "maximum recorded data" aggregation method.
   *
   * @since 1.0.0
   */
  public static final CConnectorAggregationMethod MAX_RECORDED_DATA =
      new CConnectorAggregationMethod(ENUM_VAL_MAX_RECORDED_DATA);

  /**
   * Constant representing the "average recorded data" aggregation method.
   *
   * @since 1.0.0
   */
  public static final CConnectorAggregationMethod AVERAGE_RECORDED_DATA =
      new CConnectorAggregationMethod(ENUM_VAL_AVERAGE_RECORDED_DATA);

  /**
   * The integer value of the aggregation method. This is used to store the aggregation method
   * internally, and to represent the aggregation method in the configuration file.
   *
   * @since 1.0.0
   */
  private final int aggregationMethodEnumVal;

  /**
   * Private/internal constructor to create an aggregation method enum-like constant with the
   * specified integer value.
   *
   * @param aggregationMethodEnumVal integer value of the aggregation method
   * @since 1.0.0
   */
  private CConnectorAggregationMethod(int aggregationMethodEnumVal) {
    this.aggregationMethodEnumVal = aggregationMethodEnumVal;
  }

  /**
   * Get the integer value of the aggregation method.
   *
   * @return integer value of the aggregation method
   * @since 1.0.0
   */
  public int getValue() {
    return aggregationMethodEnumVal;
  }

  /**
   * Get the aggregation method from the specified integer value.
   *
   * @param value integer value of the aggregation method
   * @return aggregation method
   * @throws IllegalArgumentException if the specified integer value is not a valid aggregation
   *     method
   * @since 1.0.0
   */
  public static CConnectorAggregationMethod fromValue(int value) {
    CConnectorAggregationMethod aggregationMethod;
    switch (value) {
      case ENUM_VAL_LAST_RECORDED_DATA:
        aggregationMethod = LAST_RECORDED_DATA;
        break;
      case ENUM_VAL_FIRST_RECORDED_DATA:
        aggregationMethod = FIRST_RECORDED_DATA;
        break;
      case ENUM_VAL_MIN_RECORDED_DATA:
        aggregationMethod = MIN_RECORDED_DATA;
        break;
      case ENUM_VAL_MAX_RECORDED_DATA:
        aggregationMethod = MAX_RECORDED_DATA;
        break;
      case ENUM_VAL_AVERAGE_RECORDED_DATA:
        aggregationMethod = AVERAGE_RECORDED_DATA;
        break;
      default:
        throw new IllegalArgumentException("Invalid aggregation method value.");
    }
    return aggregationMethod;
  }
}
