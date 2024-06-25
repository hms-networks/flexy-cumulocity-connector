package com.hmsnetworks.sc.cumulocity.api;

import com.hms_networks.americas.sc.extensions.api.ApplicationControlApiListener;
import com.hms_networks.americas.sc.extensions.string.StringUtils;
import com.hmsnetworks.sc.cumulocity.CConnectorMain;

/**
 * The listener class for receiving HTTP application control API requests for the connector's status
 * and control operations.
 *
 * @since 1.1.0
 * @author HMS Networks, MU Americas Solution Center
 */
public class CConnectorWebApiListener extends ApplicationControlApiListener {

  /**
   * The name of the custom form to register for setting the Cumulocity bootstrap authentication.
   */
  private static final String REGISTERED_CUSTOM_FORM_SET_BOOTSTRAP_AUTH = "setBootstrapAuth";

  /**
   * The name of the custom form to register for overwriting (and setting) the Cumulocity bootstrap
   * authentication.
   */
  private static final String REGISTERED_CUSTOM_FORM_OVERWRITE_BOOTSTRAP_AUTH =
      "overwriteBootstrapAuth";

  /** The list of custom forms to register for the web API. */
  private static final String[] REGISTERED_CUSTOM_FORMS = {
    REGISTERED_CUSTOM_FORM_SET_BOOTSTRAP_AUTH, REGISTERED_CUSTOM_FORM_OVERWRITE_BOOTSTRAP_AUTH,
  };

  /** The key for the host specified in a web API Bootstrap configuration request. */
  private static final String BOOTSTRAP_AUTH_WEBVAR_KEY_HOST = "host";

  /** The key for the port specified in a web API Bootstrap configuration request. */
  private static final String BOOTSTRAP_AUTH_WEBVAR_KEY_PORT = "port";

  /** The key for the tenant specified in a web API Bootstrap configuration request. */
  private static final String BOOTSTRAP_AUTH_WEBVAR_KEY_TENANT = "tenant";

  /** The key for the username specified in a web API Bootstrap configuration request. */
  private static final String BOOTSTRAP_AUTH_WEBVAR_KEY_USERNAME = "username";

  /** The key for the password specified in a web API Bootstrap configuration request. */
  private static final String BOOTSTRAP_AUTH_WEBVAR_KEY_PASSWORD = "password";

  /** Default constructor for the {@link CConnectorWebApiListener} class. */
  public CConnectorWebApiListener() {
    // Call the super constructor with the list of custom forms to register
    super(REGISTERED_CUSTOM_FORMS);
  }

  /**
   * The handler for application restart requests via the control API.
   *
   * @param value the value specified in the API request
   * @return the response to the API request
   */
  public String onRestart(String value) {
    String response;

    // Check if the application auto-restart is enabled
    if (!CConnectorMain.isAppAutoRestartEnabled()) {
      response =
          "{\"status\":\"error\",\"error\":\"Application auto-restart is not enabled. "
              + "Please check that the jvmrun file is properly uploaded.\"}";
    } else {
      CConnectorMain.shutdownAndRestartConnector();
      response = "{\"status\":\"ok\"}";
    }

    return response;
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

  /**
   * The handler for registered application-specific (custom) forms.
   *
   * <p>Custom form parameter(s) are retrieved and parsed using the {@link #getWebVar(String,
   * String)} method. If the given parameter(s) are invalid or not supported, a null value is
   * returned to trigger a {@link #RESPONSE_UNKNOWN_PARAM} response.
   *
   * @param form the name of the custom form
   * @return the response to the request, or null to trigger a {@link #RESPONSE_UNKNOWN_PARAM}
   *     response.
   */
  public String onCustomForm(String form) {
    String response = null;
    if (form.equals(REGISTERED_CUSTOM_FORM_SET_BOOTSTRAP_AUTH)) {
      response = onSetBootstrapAuth();
    } else if (form.equals(REGISTERED_CUSTOM_FORM_OVERWRITE_BOOTSTRAP_AUTH)) {
      response = onOverwriteBootstrapAuth();
    }
    return response;
  }

  /**
   * The handler for requests sent to the {@link #REGISTERED_CUSTOM_FORM_SET_BOOTSTRAP_AUTH} custom
   * form.
   *
   * @return the response to the request
   */
  private String onSetBootstrapAuth() {
    String response;

    // Check if the application auto-restart is enabled
    if (!CConnectorMain.isAppAutoRestartEnabled()) {
      response =
          "{\"status\":\"error\",\"error\":\"Application auto-restart is not enabled. "
              + "Please check that the jvmrun file is properly uploaded.\"}";
    }
    // Check if the bootstrap authentication is already set
    else if (CConnectorMain.isBootstrapComplete()) {
      response =
          "{\"status\":\"error\",\"error\":\"Bootstrap authentication is already set. "
              + "Please use the "
              + REGISTERED_CUSTOM_FORM_OVERWRITE_BOOTSTRAP_AUTH
              + " form to overwrite the existing bootstrap authentication.\"}";
    }
    // Parse input and build response
    else {
      boolean operationSuccess;

      // Parse input for bootstrap authentication values
      String host = getWebVar(BOOTSTRAP_AUTH_WEBVAR_KEY_HOST, "");
      String port = getWebVar(BOOTSTRAP_AUTH_WEBVAR_KEY_PORT, "");
      String bootstrapTenant = getWebVar(BOOTSTRAP_AUTH_WEBVAR_KEY_TENANT, "");
      String bootstrapUsername = getWebVar(BOOTSTRAP_AUTH_WEBVAR_KEY_USERNAME, "");
      String bootstrapPassword = getWebVar(BOOTSTRAP_AUTH_WEBVAR_KEY_PASSWORD, "");

      if (host.length() == 0
          || port.length() == 0
          || bootstrapTenant.length() == 0
          || bootstrapUsername.length() == 0
          || bootstrapPassword.length() == 0) {
        response =
            "{\"status\":\"error\",\"error\":\"Invalid input. Please check that all required "
                + "parameters are specified.\"}";
        return response;
      } else {
        // Set the bootstrap authentication
        final boolean isOverwrite = false;
        operationSuccess =
            CConnectorMain.setBootstrapConfiguration(
                host, port, bootstrapTenant, bootstrapUsername, bootstrapPassword, isOverwrite);
      }

      if (operationSuccess) {
        response =
            "{\"status\":\"ok\",\"response\":\"Bootstrap authentication was set successfully. "
                + "The connector will be started with the updated bootstrap authentication.\"}";
      } else {
        response = "{\"status\":\"error\",\"error\":\"Failed to set bootstrap authentication.\"}";
      }
    }

    return response;
  }

  /**
   * The handler for requests sent to the {@link #REGISTERED_CUSTOM_FORM_OVERWRITE_BOOTSTRAP_AUTH}
   * custom form.
   *
   * @return the response to the request
   */
  private String onOverwriteBootstrapAuth() {
    String response;

    // Check if the application auto-restart is enabled
    if (!CConnectorMain.isAppAutoRestartEnabled()) {
      response =
          "{\"status\":\"error\",\"error\":\"Application auto-restart is not enabled. "
              + "Please check that the jvmrun file is properly uploaded.\"}";
    }
    // Parse input and build response
    else {
      boolean isOverwrite = CConnectorMain.isBootstrapComplete();
      boolean operationSuccess;

      // Parse input for bootstrap authentication values
      String host = getWebVar(BOOTSTRAP_AUTH_WEBVAR_KEY_HOST, "");
      String port = getWebVar(BOOTSTRAP_AUTH_WEBVAR_KEY_PORT, "");
      String bootstrapTenant = getWebVar(BOOTSTRAP_AUTH_WEBVAR_KEY_TENANT, "");
      String bootstrapUsername = getWebVar(BOOTSTRAP_AUTH_WEBVAR_KEY_USERNAME, "");
      String bootstrapPassword = getWebVar(BOOTSTRAP_AUTH_WEBVAR_KEY_PASSWORD, "");

      if (host.length() == 0
          || port.length() == 0
          || bootstrapTenant.length() == 0
          || bootstrapUsername.length() == 0
          || bootstrapPassword.length() == 0) {
        response =
            "{\"status\":\"error\",\"error\":\"Invalid input. Please check that all required "
                + "parameters are specified.\"}";
        return response;
      } else {
        // Set the bootstrap authentication
        operationSuccess =
            CConnectorMain.setBootstrapConfiguration(
                host, port, bootstrapTenant, bootstrapUsername, bootstrapPassword, isOverwrite);

        if (operationSuccess) {
          if (isOverwrite) {
            response =
                "{\"status\":\"ok\",\"response\":\"Previous bootstrap authentication was"
                    + " overwritten successfully. The connector will be restarted.\"}";
          } else {
            response =
                "{\"status\":\"ok\",\"response\":\"Bootstrap authentication was set successfully. "
                    + "The connector will be started with the updated bootstrap authentication.\"}";
          }
        } else {
          response = "{\"status\":\"error\",\"error\":\"Failed to set bootstrap authentication.\"}";
        }

        // Restart connector if overwriting
        if (isOverwrite) {
          CConnectorMain.shutdownAndRestartConnector();
        }
      }
    }

    return response;
  }
}
