package com.hms_networks.sc.cumulocity;

/**
 * Main class for the Ewon Flexy Cumulocity Connector.
 *
 * @author HMS Networks, MU Americas Solution Center
 * @version 0.0.1
 */
public class CConnectorMain {

  /**
   * Main method for Ewon Flexy Cumulocity Connector.
   *
   * @param args project arguments
   */
  public static void main(String[] args) {
    // Try to output application name from Maven
    System.out.println("App name: " + CConnectorMain.class.getPackage().getImplementationTitle());

    // Try to output application version from Maven
    System.out.println(
        "App version: " + CConnectorMain.class.getPackage().getImplementationVersion());
  }
}
