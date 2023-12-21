package com.hms_networks.sc.cumulocity.data;

/**
 * An enum-like class for Java 1.4 compatibility that provides constants for the types of messages
 * sent by the Flexy Cumulocity connector. This is useful for determining the type of message to
 * ensure it is sent to the correct topic.
 *
 * @since 1.4.1
 * @version 1.0.0
 * @author HMS Networks, MU Americas Solution Center
 */
public class CConnectorMessageType {

  /**
   * Integer value representing the "data" message type.
   *
   * @see #DATA
   * @since 1.0.0
   */
  private static final int ENUM_VAL_DATA = 0;

  /**
   * Integer value representing the "JSON data" message type.
   *
   * @see #JSON_DATA
   * @since 1.0.0
   */
  private static final int ENUM_VAL_JSON_DATA = 1;

  /**
   * Integer value representing the "other" message type.
   *
   * @see #OTHER
   * @since 1.0.0
   */
  private static final int ENUM_VAL_OTHER = 2;

  /**
   * Constant representing the "data" message type. This is used for messages containing data in the
   * standard Cumulocity MQTT static template format.
   *
   * @since 1.0.0
   */
  public static final CConnectorMessageType DATA = new CConnectorMessageType(ENUM_VAL_DATA);

  /**
   * Constant representing the "JSON data" message type. This is used for messages containing data
   * in the Cumulocity MQTT JSON template format.
   *
   * @since 1.0.0
   */
  public static final CConnectorMessageType JSON_DATA =
      new CConnectorMessageType(ENUM_VAL_JSON_DATA);

  /**
   * Constant representing the "other" message type. This is used for messages that do not contain
   * data in the standard Cumulocity MQTT static template format or the Cumulocity MQTT JSON
   * template format.
   *
   * @since 1.0.0
   */
  public static final CConnectorMessageType OTHER = new CConnectorMessageType(ENUM_VAL_OTHER);

  /**
   * The integer value of the message type. This is used to store the message type internally, and
   * to represent the message type in the configuration file.
   *
   * @since 1.0.0
   */
  private final int messageTypeEnumVal;

  /**
   * Private/internal constructor to create a message type enum-like constant with the specified
   * integer value.
   *
   * @param messageTypeEnumVal integer value of the message type
   * @since 1.0.0
   */
  private CConnectorMessageType(int messageTypeEnumVal) {
    this.messageTypeEnumVal = messageTypeEnumVal;
  }

  /**
   * Get the integer value of the message type.
   *
   * @return integer value of the message type
   * @since 1.0.0
   */
  public int getValue() {
    return messageTypeEnumVal;
  }

  /**
   * Get the message type from the specified integer value.
   *
   * @param value integer value of the message type
   * @return message type
   * @throws IllegalArgumentException if the specified integer value is not a valid message type
   * @since 1.0.0
   */
  public static CConnectorMessageType fromValue(int value) {
    CConnectorMessageType messageType;
    switch (value) {
      case ENUM_VAL_DATA:
        messageType = DATA;
        break;
      case ENUM_VAL_JSON_DATA:
        messageType = JSON_DATA;
        break;
      case ENUM_VAL_OTHER:
        messageType = OTHER;
        break;
      default:
        throw new IllegalArgumentException("Invalid message type value.");
    }
    return messageType;
  }
}
