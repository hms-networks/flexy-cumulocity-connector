package com.hms_networks.sc.cumulocity.api;

import com.hms_networks.americas.sc.extensions.fileutils.FileAccessManager;
import com.hms_networks.americas.sc.extensions.logging.Logger;
import com.hms_networks.americas.sc.extensions.system.http.SCHttpUtility;
import com.hms_networks.sc.cumulocity.CConnectorMain;
import java.io.File;

/**
 * Utility class for managing the download and storage of the root CA certificate for Cumulocity
 * (GoDaddy Class 2 Certification Authority Root Certificate).
 *
 * @author HMS Networks, MU Americas Solution Center
 * @since 1.0.0
 */
public class CConnectorApiCertificateMgr {

  /** The file path for storing the downloaded Cumulocity Root CA. */
  public static final String CUMULOCITY_ROOT_CA_CERT_FILE_PATH =
      "/usr/CumulocityCertificates/rootCA.crt";

  /**
   * The file path for storing the URL used to download the stored Cumulocity Root CA. This is used
   * to determine if the certificate needs to be re-downloaded.
   */
  public static final String CUMULOCITY_ROOT_CA_CERT_URL_CACHE_FILE_PATH =
      CUMULOCITY_ROOT_CA_CERT_FILE_PATH + ".url";

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

  /**
   * Return a boolean representing if the stored Cumulocity Root CA certificate is from the same
   * download URL as the specified download URL. This is used to determine if the certificate needs
   * to be re-downloaded.
   *
   * @param downloadUrl the URL to check
   * @return true if the stored certificate is from the same URL
   */
  private static boolean isRootCaFromSameDownloadUrl(String downloadUrl) {
    boolean isRootCaFromSameDownloadUrl = false;
    File rootCaUrlCacheFile = new File(CUMULOCITY_ROOT_CA_CERT_URL_CACHE_FILE_PATH);
    if (rootCaUrlCacheFile.isFile()) {
      try {
        String lastUrl =
            FileAccessManager.readFileToString(CUMULOCITY_ROOT_CA_CERT_URL_CACHE_FILE_PATH);
        if (lastUrl.equals(downloadUrl)) {
          isRootCaFromSameDownloadUrl = true;
        }
      } catch (Exception e) {
        Logger.LOG_WARN(
            "Failed to read the download URL of the stored Cumulocity Root CA certificate.");
        Logger.LOG_EXCEPTION(e);
      }
    }
    return isRootCaFromSameDownloadUrl;
  }

  /**
   * Downloads the Cumulocity Root CA certificate to file.
   *
   * @param cumulocityCertificateUrl the configured URL to download the certificate from
   */
  private static void downloadRootCaToFile(String cumulocityCertificateUrl) {
    // Verify download location exists
    File downloadFolder = new File(CUMULOCITY_ROOT_CA_CERT_FILE_PATH).getParentFile();
    downloadFolder.mkdirs();

    // Download to file
    try {
      SCHttpUtility.httpGet(cumulocityCertificateUrl, "", "", CUMULOCITY_ROOT_CA_CERT_FILE_PATH);

      // Write URL to file
      FileAccessManager.writeStringToFile(
          CUMULOCITY_ROOT_CA_CERT_URL_CACHE_FILE_PATH, cumulocityCertificateUrl);
    } catch (Exception e) {
      Logger.LOG_SERIOUS(
          "Unable to download Cumulocity Root CA certificate to file. MQTT connections "
              + "may fail!");
      Logger.LOG_EXCEPTION(e);
    }
  }

  /**
   * Ensures that the Cumulocity Root CA certificate has been downloaded and is present on the file
   * system, then returns the file path to the downloaded certificate.
   *
   * @return Cumulocity Root CA certificate file path
   */
  public static String getRootCaFilePath() {
    // Get certificate URL
    String cumulocityCertificateUrl =
        CConnectorMain.getConnectorConfig().getCumulocityCertificateUrl();

    // Ensure certificate exists on file system and is from the same URL
    boolean downloadCertificate = false;
    if (!isRootCaWrittenToFile()) {
      Logger.LOG_INFO("Cumulocity Root CA certificate not found on file system. Downloading...");
      downloadCertificate = true;
    } else if (!isRootCaFromSameDownloadUrl(cumulocityCertificateUrl)) {
      Logger.LOG_INFO(
          "Cumulocity Root CA certificate found on file system, but is from a different URL. "
              + "Downloading...");
      downloadCertificate = true;
    }

    // Download certificate if needed
    if (downloadCertificate) {
      downloadRootCaToFile(cumulocityCertificateUrl);
      Logger.LOG_INFO("Cumulocity Root CA certificate downloaded to file system.");
    }

    // Return path
    return CUMULOCITY_ROOT_CA_CERT_FILE_PATH;
  }
}
