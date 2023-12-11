package com.hms_networks.sc.cumulocity.data;

/**
 * An enum-like class for Java 1.4 compatibility that provides constants for the Cumulocity data
 * processing modes supported by the Flexy Cumulocity connector.
 *
 * @since 1.4.1
 * @version 1.0.0
 * @author HMS Networks, MU Americas Solution Center
 */
public class CConnectorDataProcessingMode {

  /**
   * The {@link String} value representing the "persistent" data processing mode. The value of
   * {@code "s"} matches the value used by Cumulocity.
   *
   * @see #PERSISTENT
   * @since 1.0.0
   */
  private static final String ENUM_VAL_PERSISTENT = "s";

  /**
   * The {@link String} value representing the "transient" data processing mode. The value of {@code
   * "t"} matches the value used by Cumulocity.
   *
   * @see #TRANSIENT
   * @since 1.0.0
   */
  private static final String ENUM_VAL_TRANSIENT = "t";

  /**
   * The {@link String} value representing the "quiescent" data processing mode. The value of {@code
   * "q"} matches the value used by Cumulocity.
   *
   * @see #QUIESCENT
   * @since 1.0.0
   */
  private static final String ENUM_VAL_QUIESCENT = "q";

  /**
   * The {@link String} value representing the "CEP" data processing mode. The value of {@code "c"}
   * matches the value used by Cumulocity.
   *
   * @see #CEP
   * @since 1.0.0
   */
  private static final String ENUM_VAL_CEP = "c";

  /**
   * Constant representing the "persistent" data processing mode.
   *
   * @since 1.0.0
   */
  public static final CConnectorDataProcessingMode PERSISTENT =
      new CConnectorDataProcessingMode(ENUM_VAL_PERSISTENT);

  /**
   * Constant representing the "transient" data processing mode.
   *
   * @since 1.0.0
   */
  public static final CConnectorDataProcessingMode TRANSIENT =
      new CConnectorDataProcessingMode(ENUM_VAL_TRANSIENT);

  /**
   * Constant representing the "quiescent" data processing mode.
   *
   * @since 1.0.0
   */
  public static final CConnectorDataProcessingMode QUIESCENT =
      new CConnectorDataProcessingMode(ENUM_VAL_QUIESCENT);

  /**
   * Constant representing the "CEP" data processing mode.
   *
   * @since 1.0.0
   */
  public static final CConnectorDataProcessingMode CEP =
      new CConnectorDataProcessingMode(ENUM_VAL_CEP);

  /**
   * The {@link String} value of the data processing mode. This is used to store the data processing
   * mode internally, and to represent the data processing mode in the configuration file.
   *
   * @since 1.0.0
   */
  private final String dataProcessingModeEnumVal;

  /**
   * Private/internal constructor to create a data processing mode enum-like constant with the
   * specified {@link String} value.
   *
   * @param dataProcessingModeEnumVal {@link String} value of the data processing mode
   * @since 1.0.0
   */
  private CConnectorDataProcessingMode(String dataProcessingModeEnumVal) {
    this.dataProcessingModeEnumVal = dataProcessingModeEnumVal;
  }

  /**
   * Get the {@link String} value of the data processing mode. This method redirects to {@link
   * #toString()}, but is provided to allow for the more intuitive combination of {@code
   * #getValue()} and {@link #fromValue(String)}.
   *
   * @return the {@link String} value of the data processing mode
   * @since 1.0.0
   */
  public String getValue() {
    return toString();
  }

  /**
   * Get the {@link String} value of the data processing mode.
   *
   * @return the {@link String} value of the data processing mode
   * @since 1.0.0
   */
  public String toString() {
    return dataProcessingModeEnumVal;
  }

  /**
   * Get the data processing mode from the specified {@link String} value.
   *
   * @param value the {@link String} value of the aggregation method
   * @return data processing mode
   * @throws IllegalArgumentException if the specified {@link String} value is not a valid data
   *     processing mode
   * @since 1.0.0
   */
  public static CConnectorDataProcessingMode fromValue(String value) {
    CConnectorDataProcessingMode dataProcessingMode;
    if (value.equals(ENUM_VAL_PERSISTENT)) {
      dataProcessingMode = PERSISTENT;
    } else if (value.equals(ENUM_VAL_TRANSIENT)) {
      dataProcessingMode = TRANSIENT;
    } else if (value.equals(ENUM_VAL_QUIESCENT)) {
      dataProcessingMode = QUIESCENT;
    } else if (value.equals(ENUM_VAL_CEP)) {
      dataProcessingMode = CEP;
    } else {
      throw new IllegalArgumentException("Invalid data processing mode value.");
    }
    return dataProcessingMode;
  }
}
