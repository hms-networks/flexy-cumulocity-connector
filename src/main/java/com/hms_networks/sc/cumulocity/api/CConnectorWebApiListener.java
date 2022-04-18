package com.hms_networks.sc.cumulocity.api;

import com.hms_networks.americas.sc.extensions.api.ApplicationControlApiListener;
import com.hms_networks.americas.sc.extensions.string.StringUtils;
import com.hms_networks.sc.cumulocity.CConnectorMain;

/**
 * The listener class for receiving HTTP application control API requests for the connector's status
 * and control operations.
 *
 * @since 1.1.0
 * @author HMS Networks, MU Americas Solution Center
 */
public class CConnectorWebApiListener extends ApplicationControlApiListener {

  /**
   * The handler for application restart requests via the control API.
   *
   * @param value the value specified in the API request
   * @return the response to the API request
   */
  public String onRestart(String value) {
    CConnectorMain.shutdownAndRestartConnector();
    return "{\"status\":\"ok\"}";
  }

  /**
   * The handler for application shutdown requests via the control API.
   *
   * @param value the value specified in the API request
   * @return the response to the API request
   */
  public String onShutdown(String value) {
    CConnectorMain.shutdownConnector();
    return "{\"status\":\"ok\"}";
  }

  /**
   * The handler for application version information requests via the control API.
   *
   * @param value the value specified in the API request
   * @return the response to the API request with version information
   */
  public String onGetVersion(String value) {
    return "{\"status\":\"ok\",\"response\":\"" + CConnectorMain.getConnectorFriendlyName() + "\"}";
  }

  /**
   * The handler for application configuration file requests via the control API.
   *
   * @param value the value specified in the API request
   * @return the response to the API request with configuration file contents
   */
  public String onGetConfig(String value) {
    final boolean maskPasswords = true;
    String getConfigResponse;
    try {
      String configFileEscapedString =
          CConnectorMain.getConnectorConfig().getConfigFileEscapedString(maskPasswords);
      configFileEscapedString = StringUtils.replace(configFileEscapedString, "\r", "\\r");
      configFileEscapedString = StringUtils.replace(configFileEscapedString, "\n", "\\n");
      getConfigResponse = "{\"status\":\"ok\",\"response\":\"" + configFileEscapedString + "\"}";
    } catch (Exception e) {
      getConfigResponse = "{\"status\":\"error\",\"error\":\"" + e.getMessage() + "\"}";
    }
    return getConfigResponse;
  }
}
