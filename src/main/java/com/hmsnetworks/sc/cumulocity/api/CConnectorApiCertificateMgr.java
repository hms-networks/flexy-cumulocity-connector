package com.hmsnetworks.sc.cumulocity.api;

import com.hms_networks.americas.sc.extensions.fileutils.FileAccessManager;
import com.hms_networks.americas.sc.extensions.logging.Logger;
import com.hms_networks.americas.sc.extensions.system.application.SCAppManagement;
import com.hms_networks.americas.sc.extensions.system.http.SCHttpUtility;
import com.hmsnetworks.sc.cumulocity.CConnectorMain;
import java.io.File;

/**
 * Utility class for managing the download and storage of the root CA certificate for Cumulocity
 * (GoDaddy Class 2 Certification Authority Root Certificate).
 *
 * @author HMS Networks, MU Americas Solution Center
 * @since 1.0.0
 */
public class CConnectorApiCertificateMgr {

  /** The file path for storing the Cumulocity Default CA. */
  public static final String CUMULOCITY_DEFAULT_CA_CERT_FILE_PATH =
      "/usr/CumulocityCertificates/defaultCA.crt";

  /**
   * The file contents of the Cumulocity Default CA. This is the string representation of GoDaddy's
   * Global Root CA - G2 certificate in PEM format. See certificate file at <a
   * href="https://certs.godaddy.com/repository/gdroot-g2.crt">https://certs.godaddy.com/repository/gdroot-g2.crt</a>.
   */
  public static final String CUMULOCITY_DEFAULT_CA_CERT_FILE_CONTENTS =
      "-----BEGIN CERTIFICATE-----\r\n"
          + "MIIDxTCCAq2gAwIBAgIBADANBgkqhkiG9w0BAQsFADCBgzELMAkGA1UEBhMCVVMx\r\n"
          + "EDAOBgNVBAgTB0FyaXpvbmExEzARBgNVBAcTClNjb3R0c2RhbGUxGjAYBgNVBAoT\r\n"
          + "EUdvRGFkZHkuY29tLCBJbmMuMTEwLwYDVQQDEyhHbyBEYWRkeSBSb290IENlcnRp\r\n"
          + "ZmljYXRlIEF1dGhvcml0eSAtIEcyMB4XDTA5MDkwMTAwMDAwMFoXDTM3MTIzMTIz\r\n"
          + "NTk1OVowgYMxCzAJBgNVBAYTAlVTMRAwDgYDVQQIEwdBcml6b25hMRMwEQYDVQQH\r\n"
          + "EwpTY290dHNkYWxlMRowGAYDVQQKExFHb0RhZGR5LmNvbSwgSW5jLjExMC8GA1UE\r\n"
          + "AxMoR28gRGFkZHkgUm9vdCBDZXJ0aWZpY2F0ZSBBdXRob3JpdHkgLSBHMjCCASIw\r\n"
          + "DQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAL9xYgjx+lk09xvJGKP3gElY6SKD\r\n"
          + "E6bFIEMBO4Tx5oVJnyfq9oQbTqC023CYxzIBsQU+B07u9PpPL1kwIuerGVZr4oAH\r\n"
          + "/PMWdYA5UXvl+TW2dE6pjYIT5LY/qQOD+qK+ihVqf94Lw7YZFAXK6sOoBJQ7Rnwy\r\n"
          + "DfMAZiLIjWltNowRGLfTshxgtDj6AozO091GB94KPutdfMh8+7ArU6SSYmlRJQVh\r\n"
          + "GkSBjCypQ5Yj36w6gZoOKcUcqeldHraenjAKOc7xiID7S13MMuyFYkMlNAJWJwGR\r\n"
          + "tDtwKj9useiciAF9n9T521NtYJ2/LOdYq7hfRvzOxBsDPAnrSTFcaUaz4EcCAwEA\r\n"
          + "AaNCMEAwDwYDVR0TAQH/BAUwAwEB/zAOBgNVHQ8BAf8EBAMCAQYwHQYDVR0OBBYE\r\n"
          + "FDqahQcQZyi27/a9BUFuIMGU2g/eMA0GCSqGSIb3DQEBCwUAA4IBAQCZ21151fmX\r\n"
          + "WWcDYfF+OwYxdS2hII5PZYe096acvNjpL9DbWu7PdIxztDhC2gV7+AJ1uP2lsdeu\r\n"
          + "9tfeE8tTEH6KRtGX+rcuKxGrkLAngPnon1rpN5+r5N9ss4UXnT3ZJE95kTXWXwTr\r\n"
          + "gIOrmgIttRD02JDHBHNA7XIloKmf7J6raBKZV8aPEjoJpL1E/QYVN8Gb5DKj7Tjo\r\n"
          + "2GTzLH4U/ALqn83/B2gX2yKQOC16jdFU8WnjXzPKej17CuPKf1855eJ1usV2GDPO\r\n"
          + "LPAvTK33sefOT6jEm0pUBsV/fdUID+Ic/n4XuKxe9tQWskMJDE32p2u0mYRlynqI\r\n"
          + "4uJEvlz36hz1\r\n"
          + "-----END CERTIFICATE-----\r\n";

  /** The file path for storing the downloaded Cumulocity Custom CA. */
  public static final String CUMULOCITY_CUSTOM_CA_CERT_FILE_PATH =
      "/usr/CumulocityCertificates/customCA.crt";

  /**
   * The file path for storing the URL used to download the stored Cumulocity Custom CA. This is
   * used to determine if the certificate needs to be re-downloaded.
   */
  public static final String CUMULOCITY_CUSTOM_CA_CERT_URL_CACHE_FILE_PATH =
      CUMULOCITY_CUSTOM_CA_CERT_FILE_PATH + ".url";

  /**
   * Return a boolean representing if the Cumulocity Default CA certificate file is present in the
   * file system.
   *
   * @return true if certificate present
   */
  private static boolean isDefaultCaWrittenToFile() {
    File defaultCaFile = new File(CUMULOCITY_DEFAULT_CA_CERT_FILE_PATH);
    return defaultCaFile.isFile();
  }

  /**
   * Return a boolean representing if the Cumulocity Custom CA certificate file is present in the
   * file system.
   *
   * @return true if certificate present
   */
  private static boolean isCustomCaWrittenToFile() {
    File customCaFile = new File(CUMULOCITY_CUSTOM_CA_CERT_FILE_PATH);
    return customCaFile.isFile();
  }

  /**
   * Return a boolean representing if the stored Cumulocity Custom CA certificate is from the same
   * download URL as the specified download URL. This is used to determine if the certificate needs
   * to be re-downloaded.
   *
   * @param downloadUrl the URL to check
   * @return true if the stored certificate is from the same URL
   */
  private static boolean isCustomCaFromSameDownloadUrl(String downloadUrl) {
    boolean isCustomCaFromSameDownloadUrl = false;
    File customCaUrlCacheFile = new File(CUMULOCITY_CUSTOM_CA_CERT_URL_CACHE_FILE_PATH);
    if (customCaUrlCacheFile.isFile()) {
      try {
        String lastUrl =
            FileAccessManager.readFileToString(CUMULOCITY_CUSTOM_CA_CERT_URL_CACHE_FILE_PATH);
        if (lastUrl.equals(downloadUrl)) {
          isCustomCaFromSameDownloadUrl = true;
        }
      } catch (Exception e) {
        Logger.LOG_WARN(
            "Failed to read the download URL of the stored Cumulocity Custom CA certificate.");
        Logger.LOG_EXCEPTION(e);
      }
    }
    return isCustomCaFromSameDownloadUrl;
  }

  /**
   * Downloads the Cumulocity Custom CA certificate to file.
   *
   * @param cumulocityCertificateUrl the configured URL to download the certificate from
   */
  private static void downloadCustomCaToFile(String cumulocityCertificateUrl) {
    // Verify download location exists
    File downloadFolder = new File(CUMULOCITY_CUSTOM_CA_CERT_FILE_PATH).getParentFile();
    downloadFolder.mkdirs();

    // Download to file
    try {
      SCHttpUtility.httpGet(cumulocityCertificateUrl, "", "", CUMULOCITY_CUSTOM_CA_CERT_FILE_PATH);

      // Write URL to file
      FileAccessManager.writeStringToFile(
          CUMULOCITY_CUSTOM_CA_CERT_URL_CACHE_FILE_PATH, cumulocityCertificateUrl);
    } catch (Exception e) {
      Logger.LOG_SERIOUS(
          "Unable to download Cumulocity Custom CA certificate to file. MQTT connections "
              + "may fail!");
      Logger.LOG_EXCEPTION(e);
    }
  }

  /** Writes the Cumulocity Default CA certificate to file. */
  private static void writeDefaultCaToFile() {
    // Verify write location exists
    File writeFolder = new File(CUMULOCITY_DEFAULT_CA_CERT_FILE_PATH).getParentFile();
    writeFolder.mkdirs();

    // Download to file
    try {
      // Write default CA contents to file
      FileAccessManager.writeStringToFile(
          CUMULOCITY_DEFAULT_CA_CERT_FILE_PATH, CUMULOCITY_DEFAULT_CA_CERT_FILE_CONTENTS);
    } catch (Exception e) {
      Logger.LOG_SERIOUS(
          "Unable to write Cumulocity Default CA certificate to file. MQTT connections "
              + "may fail!");
      Logger.LOG_EXCEPTION(e);
    }
  }

  /**
   * Ensures that the Cumulocity Default CA certificate has been written to the file system, then
   * returns the file path to the downloaded certificate.
   *
   * @return Cumulocity Default CA certificate file path
   */
  public static String getDefaultCaFilePath() {
    // Ensure certificate exists on file system and is from the same URL
    boolean writeCertificate = false;
    if (!isDefaultCaWrittenToFile()) {
      Logger.LOG_INFO(
          "Cumulocity Default CA certificate not found on file system. Writing from cache...");
      writeCertificate = true;
    }

    // Download certificate if needed
    if (writeCertificate) {
      writeDefaultCaToFile();
      Logger.LOG_INFO("Cumulocity Default CA certificate written to file system.");
    }

    // Return path
    return CUMULOCITY_DEFAULT_CA_CERT_FILE_PATH;
  }

  /**
   * Ensures that the Cumulocity Custom CA certificate has been downloaded and is present on the
   * file system, then returns the file path to the downloaded certificate.
   *
   * @return Cumulocity Custom CA certificate file path
   */
  public static String getCustomCaFilePath() {
    // Get certificate URL
    String cumulocityCertificateUrl =
        CConnectorMain.getConnectorConfig().getCumulocityCustomCertificateUrl();

    // Ensure certificate exists on file system and is from the same URL
    boolean downloadCertificate = false;
    if (!isCustomCaWrittenToFile()) {
      Logger.LOG_INFO("Cumulocity Custom CA certificate not found on file system. Downloading...");
      downloadCertificate = true;
    } else if (!isCustomCaFromSameDownloadUrl(cumulocityCertificateUrl)) {
      Logger.LOG_INFO(
          "Cumulocity Custom CA certificate found on file system, but is from a different URL. "
              + "Downloading...");
      downloadCertificate = true;
    }

    // Download certificate if needed
    if (downloadCertificate) {
      try {
        SCAppManagement.waitForWanIp();
      } catch (InterruptedException e) {
        Logger.LOG_CRITICAL("Interrupted Exception thrown during wait for WAN IP!");
        Logger.LOG_EXCEPTION(e);
      }
      downloadCustomCaToFile(cumulocityCertificateUrl);
      Logger.LOG_INFO("Cumulocity Custom CA certificate downloaded to file system.");
    }

    // Return path
    return CUMULOCITY_CUSTOM_CA_CERT_FILE_PATH;
  }

  /**
   * Gets the relevant Cumulocity Root CA certificate file path based on if a custom certificate URL
   * is enabled via the connector configuration file.
   *
   * @return Cumulocity Root CA certificate file path
   */
  public static String getRootCaFilePath() {
    if (CConnectorMain.getConnectorConfig().getCumulocityCustomCertificateUrlEnabled()) {
      return getCustomCaFilePath();
    } else {
      return getDefaultCaFilePath();
    }
  }
}
