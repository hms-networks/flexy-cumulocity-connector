package com.hmsnetworks.sc.cumulocity.data;

/**
 * Utility class for storing the content and retry count for an MQTT message to Cumulocity. This
 * class is used when the initial attempt to send the message to Cumulocity fails, and it needs to
 * be retried.
 *
 * @author HMS Networks, MU Americas Solution Center
 * @since 1.3.2
 */
public class CConnectorRetryMessage {

  /** The {@link String} message payload content. */
  private final String messagePayload;

  /** The child device name for the message, if applicable. */
  private final String childDevice;

  /** The value indicating the type of the message. */
  private final CConnectorMessageType messageType;

  /**
   * The number of times the message has been retried. This value is incremented each time the
   * message is retried.
   */
  private int retryCount;

  /**
   * Constructor for a new {@link CConnectorRetryMessage} object with the specified message payload
   * content and child device name (null if not applicable). The retry count is initialized to 0.
   *
   * @param messagePayload the {@link String} message payload content
   * @param childDevice the child device to route the message to (if not null)
   * @param messageType the value indicating the type of the message
   */
  public CConnectorRetryMessage(
      String messagePayload, String childDevice, CConnectorMessageType messageType) {
    this.messagePayload = messagePayload;
    this.childDevice = childDevice;
    this.messageType = messageType;
    this.retryCount = 0;
  }

  /**
   * Gets the {@link String} message payload content.
   *
   * @return the {@link String} message payload content
   */
  public String getMessagePayload() {
    return messagePayload;
  }

  /**
   * Gets the name of the child device to route the message to (if not null).
   *
   * @return the name of the child device to route the message to (if not null)
   */
  public String getChildDevice() {
    return childDevice;
  }

  /**
   * Gets the value indicating the type of the message.
   *
   * @return the value indicating the type of the message
   */
  public CConnectorMessageType getMessageType() {
    return messageType;
  }

  /**
   * Gets the number of times the message has been retried.
   *
   * @return the number of times the message has been retried
   */
  public int getRetryCount() {
    return retryCount;
  }

  /** Increments the retry count by 1. */
  public void incrementRetryCount() {
    retryCount++;
  }
}
