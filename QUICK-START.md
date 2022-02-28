# Ewon Flexy Cumulocity Connector

Copyright © 2022 HMS Industrial Networks Inc.

The Ewon Flexy Cumulocity Connector package provides a connector-based solution to Cumulocity for
linking Ewon devices using a direct data path with a Flexy Java application.

## Installation

Installation of the Ewon Flexy Cumulocity Connector is simple, and requires only the upload of a
handful of files to your Ewon device.

Using an FTP client of your choice, such as [Filezilla](https://filezilla-project.org/), upload the
following files to the /usr directory of the Ewon device:

1. `flexy-cumulocity-connector-{VERSION}-full.jar`
2. `jvmrun` 
   1. Located in the `starting-files` folder
3. `CumulocityConnectorConfig.json` 
   1. Located in the `starting-files` folder

## Configuration

After the files have been uploaded, a device registration entry must be created in Cumulocity, and
the connector configuration file updated. You'll need to know the bootstrap tenant, username, and
password for your Cumulocity instance prior to configuration.

### Cumulocity Registration

To create a device registration entry in Cumulocity, you will need to open the Cumulocity UI and
navigate to the 'Registration' tab under 'Devices'.

Click the 'Register Device' button to begin the registration process.

![Registration Page Register Device Button](images/RegistrationPage.png)

Select the 'General device registration' option.

*Note: You may select the bulk device registration option if you wish to register multiple devices
at once. These instructions describe only the general device registration process.*

![Registration Page General or Bulk Registration Selection](images/RegistrationGeneralBulk.png)

Enter the device ID of the Ewon Flexy you are registering. The Cumulocity device ID for an Ewon
device is based on the serial number of the Ewon Flexy, and is formatted as `HMS-Flexy-{SERIAL}`.

![Registration Page Device ID Entry](images/RegistrationDeviceId.png)

After entering the device ID, you may optionally specify a group to add the newly registered device
to, or register another device.

When finished, click the 'Next' button to finalize device registration, then click the 'Complete'
button when finished.

![Registration Page Complete Button](images/RegistrationComplete.png)

After the device registration has been completed and the configuration file has been set up as
described below, you may start up the Ewon Flexy Cumulocity Connector and the device will register
itself. Once registered, it will be pending acceptance in the Cumulocity device registration portal.

### Connector Configuration File

The bootstrap credentials from your Cumulocity tenant must be configured in the connector
configuration file prior to starting the connector.

Using an FTP client of your choice, download and edit the `CumulocityConnectorConfig.json` file
located in the `/usr` directory of the Ewon device.

Populate the `BootstrapUsername`, `BootstrapPassword`, and `BootstrapTenant` fields with the
corresponding values for your Cumulocity tenant.

![Connector Configuration File Bootstrap Edit](images/ConnectorConfigBootstrap.png)

Once populated, save and upload the updated file to the Ewon device and restart the connector. In
many scenarios, it may be simplest to restart the connector by restarting the entire Ewon device.

The device will now automatically register itself with Cumulocity. After it has started up, the
device will be pending acceptance in Cumulocity. After it is accepted, the device will be properly
connected and usable.