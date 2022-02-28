package com.hms_networks.sc.cumulocity.api;

import com.ewon.ewonitf.EWException;
import com.ewon.ewonitf.ScheduledActionManager;
import com.hms_networks.americas.sc.extensions.logging.Logger;
import com.hms_networks.americas.sc.extensions.system.http.SCHttpUtility;
import java.io.File;

/**
 * Utility class for managing the download and storage of the root CA certificate for Cumulocity
 * (GoDaddy Class 2 Certification Authority Root Certificate).
 *
 * @author HMS Networks, MU Americas Solution Center
 * @since 1.0.0
 */
public class CConnectorApiCertificateMgr {

  /** The host which serves the Cumulocity Root CA. */
  public static final String CUMULOCITY_ROOT_CA_DOWNLOAD_HOST = "https://certs.godaddy.com";

  /** The path to download the Cumulocity Root CA. */
  public static final String CUMULOCITY_ROOT_CA_DOWNLOAD_PATH = "/repository/gdroot-g2.crt";

  /** The file path for storing the downloaded Cumulocity Root CA. */
  public static final String CUMULOCITY_ROOT_CA_CERT_FILE_PATH =
      "/usr/CumulocityCertificates/rootCA.crt";

  /**
   * Return a boolean representing if the Cumulocity Root CA certificate file is present in the file
   * system.
   *
   * @return true if certificate present
   */
  private static boolean isRootCaWrittenToFile() {
    File rootCaFile = new File(CUMULOCITY_ROOT_CA_CERT_FILE_PATH);
    return rootCaFile.isFile();
  }

  /** Downloads the Cumulocity Root CA certificate to file. */
  private static void downloadRootCaToFile() {
    // Verify download location exists
    File downloadFolder = new File(CUMULOCITY_ROOT_CA_CERT_FILE_PATH).getParentFile();
    downloadFolder.mkdirs();

    // Download to file
    int result = SCHttpUtility.HTTPX_CODE_NO_ERROR;
    try {
      result =
          ScheduledActionManager.GetHttp(
              CUMULOCITY_ROOT_CA_DOWNLOAD_HOST,
              CUMULOCITY_ROOT_CA_CERT_FILE_PATH,
              CUMULOCITY_ROOT_CA_DOWNLOAD_PATH);
    } catch (EWException e) {
      Logger.LOG_SERIOUS(
          "Unable to download Cumulocity Root CA certificate to file. MQTT connections "
              + "may fail!");
      Logger.LOG_EXCEPTION(e);
    }

    // Log error(s) (if necessary/applicable)
    if (result != SCHttpUtility.HTTPX_CODE_NO_ERROR) {
      if (result == SCHttpUtility.HTTPX_CODE_CONNECTION_ERROR) {
        Logger.LOG_SERIOUS(
            "A connection error has occurred while downloading the Cumulocity Root CA "
                + "certificate.");
      } else if (result == SCHttpUtility.HTTPX_CODE_AUTH_ERROR) {
        Logger.LOG_SERIOUS(
            "An authentication error has occurred while downloading the Cumulocity Root CA "
                + "certificate.");
      } else if (result == SCHttpUtility.HTTPX_CODE_EWON_ERROR) {
        Logger.LOG_SERIOUS(
            "An Ewon error has occurred while downloading the Cumulocity Root CA "
                + "certificate.");
      } else {
        Logger.LOG_SERIOUS(
            "A generic error has occurred while downloading the Cumulocity Root CA "
                + "certificate. Error Code: "
                + result);
      }
    }
  }

  /**
   * Ensures that the Cumulocity Root CA certificate has been downloaded and is present on the file
   * system, then returns the file path to the downloaded certificate.
   *
   * @return Cumulocity Root CA certificate file path
   */
  public static String getRootCaFilePath() {
    // Ensure certificate exists on file system
    if (!isRootCaWrittenToFile()) {
      downloadRootCaToFile();
    }

    // Return path
    return CUMULOCITY_ROOT_CA_CERT_FILE_PATH;
  }
}
