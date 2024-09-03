package com.hms_networks.sc.cumulocity.inventory;

import com.ewon.ewonitf.DefaultEventHandler;
import com.ewon.ewonitf.EvtTagValueListener;

/**
 * This class implements a tag value listener for the purpose of triggering Cumulocity inventory
 * object updates. This allows users to define update messages outside the connector software and
 * then trigger updates via the tag value.
 *
 * @since 1.5.0
 * @version 1.0.0
 * @author HMS Networks, Americas
 */
public class InventoryUpdateEvtHandler extends EvtTagValueListener {

  /** Specific trigger tag name. */
  public static final String UPDATE_TRIGGER_TAG_NAME = "CumulocityInventoryUpdate";

  /** Singleton instance of class. */
  private static InventoryUpdateEvtHandler instance = null;

  /**
   * Method to setup the tag value event monitor.
   *
   * @since 1.0.0
   */
  public static void setup() {
    if (instance == null) {
      instance = new InventoryUpdateEvtHandler();
    }
  }

  /**
   * Remove handler, shutdown Inventory update manager.
   *
   * @since 1.0.0
   */
  public static void shutdown() {
    InventoryUpdateManager.shutdown();
    if (instance != null) {
      DefaultEventHandler.delTagValueListener(instance);
      instance = null;
    }
  }

  /**
   * Private constructor to guarantee that multiple event handlers are not registered.
   *
   * @since 1.0.0
   */
  private InventoryUpdateEvtHandler() {
    this.setTagName(UPDATE_TRIGGER_TAG_NAME);
    DefaultEventHandler.addTagValueListener(this);
  }

  /**
   * Method to handle the event of the trigger tag changing. A tag value of 1 triggers an update.
   *
   * @since 1.0.0
   */
  public void callTagChanged() {
    /** Tag value that will cause trigger. */
    final int TRIGGER_VALUE = 1;
    if (getTagValueAsInt() == TRIGGER_VALUE) {
      InventoryUpdateManager.runUpdate();
    }
  }
}
