package com.hms_networks.sc.cumulocity.inventory;

import com.ewon.ewonitf.EWException;
import com.ewon.ewonitf.TagControl;
import com.hms_networks.americas.sc.extensions.fileutils.FileAccessManager;
import com.hms_networks.americas.sc.extensions.logging.Logger;
import com.hms_networks.americas.sc.extensions.mqtt.MqttStatusCode;
import com.hms_networks.sc.cumulocity.CConnectorMain;
import com.hms_networks.sc.cumulocity.api.CConnectorMqttMgr;
import java.io.File;
import java.io.IOException;

/**
 * This class is responsible for reading child object update files and publishing the file contents
 * when triggered.
 *
 * @since 1.5.0
 * @version 1.0.0
 * @author HMS Networks, Americas
 */
public class InventoryUpdateManager {
  /** Folder containing inventory update payloads */
  private static final String INVENTORY_UPDATE_PAYLOAD_FOLDER = "/usr/CumulocityInventoryObjects/";

  /** File extension for the inventory update files. */
  private static final String INVENTORY_UPDATE_PAYLOAD_FILE_EXT = ".json";

  /** Unique file name for parent device inventory object updates. */
  private static final String UNIQUE_PARENT_INVENTORY_FILE_NAME = "parent";

  /** Single thread for checking and publishing payloads. */
  private static Thread inventoryUpdateThread;

  /**
   * Helper to get the child name from the file name. Files must have the exact child name and the
   * .json extension. This method does not verify that the child was registered in the inventory.
   *
   * @param targetFile FILE object that was read from the directory
   * @return child device name extracted from the file name
   * @since 1.0.0
   */
  private static String getChildNameFromFile(File targetFile) {
    String fileName = targetFile.getName();
    return fileName.substring(0, fileName.indexOf(INVENTORY_UPDATE_PAYLOAD_FILE_EXT));
  }

  /**
   * Method to update the device inventory object with the contents of the file. This method will
   * only try to make the update if the connection is established. This method works for both parent
   * and child updates. For a child device, a registration attempt is made before the inventory
   * object update.
   *
   * @param targetFile FILE object that was read from the directory
   * @throws IOException for failures reading the file
   * @throws EWException for MQTT failures
   * @throws IllegalStateException if the MQTT client is not connected
   * @since 1.0.0
   */
  public static void updateDeviceInventoryObjectFromFile(File targetFile)
      throws IOException, EWException, IllegalStateException {

    CConnectorMqttMgr mqttMgr = CConnectorMain.getMqttMgr();
    // check the MQTT connection status, return if not connected
    if (mqttMgr == null || mqttMgr.getLastKnownMqttStatusCode() != MqttStatusCode.CONNECTED) {
      throw new IllegalStateException(
          "MQTT client is not connected, cannot perform inventory update.");
    }

    String childName = getChildNameFromFile(targetFile);
    String fileContents = FileAccessManager.readFileToString(targetFile);

    // Check for parent inventory object update
    if (childName.equals(UNIQUE_PARENT_INVENTORY_FILE_NAME)) {
      mqttMgr.updateParentDeviceInventoryObject(fileContents);
      Logger.LOG_INFO("Publishing parent inventory object update.");
    } else {

      // not parent, but child - verify before updating
      mqttMgr.verifyChildDeviceRegistration(childName);
      mqttMgr.updateChildDeviceInventoryObject(childName, fileContents);
      Logger.LOG_INFO("Publishing inventory update payload from : " + childName);
    }
  }

  /**
   * Method to load all inventory update payload files form the designated directory. For valid
   * files, will call {@link #updateDeviceInventoryObjectFromFile}. Valid files have .json
   * extension. Exceptions are caught and logged.
   *
   * @param onlyParentFile boolean to indicate if only the parent file should be updated. If {@code
   *     true}, files that do not match the parent file name will be skipped.
   * @return integer array of length 2: number of updates performed without error, number of errors
   *     and/or non updates
   * @since 1.0.0
   */
  public static int[] loadInventoryUpdatePayloads(boolean onlyParentFile) {
    // Load all configuration files from the triggered payloads folder
    File folder = new File(INVENTORY_UPDATE_PAYLOAD_FOLDER);
    File[] files = folder.listFiles();

    // count the number of successful updates
    int updateCount[] = {0, 0};

    // check for special case of no files found
    if (files == null || files.length == 0) {
      Logger.LOG_INFO("No inventory update files found.");
      return updateCount;
    }

    for (int i = 0; i < files.length; i++) {
      // Skip directories and non-json files
      if (files[i].isDirectory()
          || !files[i].getName().endsWith(INVENTORY_UPDATE_PAYLOAD_FILE_EXT)) {
        updateCount[1]++;
        continue;
      }

      // Now that basic checks are done, send update message for the files
      try {
        // With the onlyParentFile flag, only update the parent device
        if (onlyParentFile) {
          if (files[i]
              .getName()
              .equals(UNIQUE_PARENT_INVENTORY_FILE_NAME + INVENTORY_UPDATE_PAYLOAD_FILE_EXT)) {
            updateDeviceInventoryObjectFromFile(files[i]);
            updateCount[0]++;
          } else {
            // was not the parent, skip
            updateCount[1]++;
          }
        } else {
          updateDeviceInventoryObjectFromFile(files[i]);
          updateCount[0]++;
        }
      } catch (IOException e) {
        updateCount[1]++;
        Logger.LOG_CRITICAL("Error reading " + files[i].getName() + ": " + e.getMessage());
        Logger.LOG_EXCEPTION(e);
        // for IllegalState and for EWException, log and continue
      } catch (Exception e) {
        updateCount[1]++;
        Logger.LOG_CRITICAL(
            "Unable to update inventory object for device "
                + getChildNameFromFile(files[i])
                + ": "
                + e.getMessage());
        Logger.LOG_EXCEPTION(e);
      }
    }
    return updateCount;
  }

  /*
   * Reset the inventory trigger tag back to 0.
   * @since 1.0.0
   */
  public static void resetTriggerTag() {
    final int TRIGGER_VALUE_RESET = 0;
    try {
      TagControl tc = new TagControl(InventoryUpdateEvtHandler.UPDATE_TRIGGER_TAG_NAME);
      tc.setTagValueAsInt(TRIGGER_VALUE_RESET);
    } catch (EWException e) {
      Logger.LOG_CRITICAL("Error resetting inventory update trigger tag value: " + e.getMessage());
      Logger.LOG_EXCEPTION(e);
    }
  }

  /**
   * Shutdown the inventory update thread, if running.
   *
   * @since 1.0.0
   */
  public static void shutdown() {
    if (inventoryUpdateThread != null) {
      if (inventoryUpdateThread.isAlive()) {
        inventoryUpdateThread.interrupt();
        try {
          final int waitMillis = 1000;
          inventoryUpdateThread.join(waitMillis);
        } catch (InterruptedException e) {
          Logger.LOG_CRITICAL(
              "Exception waiting for inventory update thread to stop: " + e.getMessage());
          Logger.LOG_EXCEPTION(e);
        }
      }
    }
  }

  /**
   * Run updates in a new thread. This ensures that the caller is not blocked.
   *
   * @since 1.0.0
   */
  public static void runUpdate() {

    // check if thread is still running
    if (inventoryUpdateThread != null) {
      if (inventoryUpdateThread.isAlive()) {
        inventoryUpdateThread.interrupt();
        try {
          final int waitMillis = 1000;
          inventoryUpdateThread.join(waitMillis);
        } catch (InterruptedException e) {
          Logger.LOG_CRITICAL(
              "Exception waiting for inventory update thread to stop: " + e.getMessage());
          Logger.LOG_EXCEPTION(e);
        }
      }
    }

    inventoryUpdateThread =
        new Thread(
            new Runnable() {
              public void run() {
                final boolean onlyParentFile = false;
                int[] updatesCount = loadInventoryUpdatePayloads(onlyParentFile);
                // check for no files at all, log message
                if (updatesCount[0] == 0 && updatesCount[1] == 0) {
                  Logger.LOG_INFO("No inventory update files were found.");
                  return;
                }
                // log the number of updates and errors
                if (updatesCount[0] > 0) {
                  resetTriggerTag();
                  Logger.LOG_INFO(
                      Integer.toString(updatesCount[0])
                          + " inventory update file(s) uploaded without error.");
                }
                Logger.LOG_INFO(
                    Integer.toString(updatesCount[1]) + " inventory update file(s) not uploaded.");
              }
            });

    inventoryUpdateThread.start();
  }

  /**
   * The inventory update functionality was designed to work with files created by basic
   * applications. Basic applications cannot create directories. Thus, this method creates the
   * directory if it does not exist.
   */
  public static void createInventoryUpdateFolder() {
    File folder = new File(INVENTORY_UPDATE_PAYLOAD_FOLDER);
    if (!folder.exists()) {
      folder.mkdir();
    }
  }
}
