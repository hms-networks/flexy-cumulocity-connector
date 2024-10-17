# Ewon Flexy Cumulocity Connector Changelog

## Version 1.4.4
### Features
- Update extension library to version 1.16.0
### Bug Fixes
- Handles changes in timezone UTC offset

## Version 1.4.3
### Features
- Added support for Cumulocity inventory updates 

## Version 1.4.2
### Features
- Update deployment dependencies
### Bug Fixes
- Fixed an issue where the main thread ends, but the event handler thread continues to run

## Version 1.4.1
### Features
- Added support for globally setting the Cumulocity data processing mode for tag data
- Added support for changing the 'type' value in aggregated data payloads for the parent
  (main/non-child) device
### Bug Fixes
- Fixed an issue where erroneous calculations on boolean tag values could prevent data aggregation
  from working properly
- Fixed missing entry in README.md documentation table of contents
### Other
- Updated com.hms_networks.americas.sc:extensions library to version 1.15.9
- Improved MQTT client status code tracking to facilitate better connection failure detection
    - Data polling is skipped when the MQTT client does not report a successful/healthy connection
- Improved speed of status reporting for pending operations such as firmware updates and
  configuration changes
- Removed c8y_Software from supported Cumulocity operations as it is not supported
- Documented the supported Cumulocity operations in the README.md file

## Version 1.4.0
### Features
- Added support for historical data queue aggregation (disabled by default)
  - Added CConnectorJsonDataPayload.java to support aggregation payload construction
  - Added MQTT manager support for retrying and sending JSON data payloads
### Bug Fixes
- Removed unused @throws declaration in CConnectorApiMessageReader.java
- Fixed Javadocs in the main class and MQTT manager classes
### Other
- Added standalone tag parsing utility class to facilitate cleaner code
- Updated com.hms_networks.americas.sc:extensions library to version 1.15.8
- Updated IDE files

## Version 1.3.6
### Features
- N/A
### Bug Fixes
- N/A
### Other
- Switch historical data queue diagnostic tags to use the new native implementation from the extensions library.

## Version 1.3.5
### Features
- N/A
### Bug Fixes
- Fixed an issue where the connector would try to download a server certificate before the WAN was initialized. This will resolve MQTT-related HTTPS errors on cellular connections. 
### Other
- Formatting change 

## Version 1.3.4
### Features
- N/A
### Bug Fixes
- Fixed a bug which could cause the application to trigger a restart when initialization or startup is paused or delayed
- Fixed a bug which could cause the application to hang when a shutdown or restart was requested before initialization or startup was complete
### Other
- Improved connector restart HTTP API to show an error when the required connector auto-restart feature is not enabled
- Improved support for shutting down or restarting when requested before initialization or startup is complete by cancelling additional tasks
- Corrected documentation to include configuration file parameters for the improved certificate URL settings from v1.3.3.

## Version 1.3.3
### Features
- Added support for remotely setting the bootstrap configuration using the REST API
  - Support for overwriting an existing bootstrap configuration is also available via the REST API
### Bug Fixes
- N/A
### Other
- Made the custom certificate URL setting optional
  - New installations will by default use the default Cumulocity Root CA certificate setting
  - Existing installations will continue to use their existing Cumulocity Custom CA certificate URL setting
    - The previous `CertificateUrl` setting will be moved to the new `CustomCertificateUrl` setting and `CustomCertificateUrlEnabled` will be set to `true`.
  - Note: The default Cumulocity Root CA certificate setting, when `CustomCertificateUrlEnabled` is set to `false`, uses a locally cached copy of the default Cumulocity Cloud Root CA certificate.

## Version 1.3.2
### Features
- Added support for queueing messages to be retried when MQTT connection is lost or not available
  - Upon reconnection, the queued messages will be sent
### Bug Fixes
- Fixed an issue where the connector improperly attempted provisioning when disconnected from MQTT
- Fixed an issue where the connector watchdog was not services during provisioning
### Other
- Added documentation for accessing the REST API via M2Web

## Version 1.3.1
### Features
- N/A
### Bug Fixes
- N/A
### Other
- Updated GitHub Actions to use the latest available versions from sc-java-maven-starter-project
  - Files from the starting-files folder are now supplied as release artifacts
  - Documentation is now supplied in PDF format as release artifacts

## Version 1.3.0
### Features
- Added option to change Cumulocity server certificate in configuration file
- Update MQTT to use new library ConstrainedMqttManager.java which simplifies and improves error handling
### Bug Fixes
- N/A
### Other
- Updated com.hms_networks.americas.sc:extensions library to version 1.12.0

## Version 1.2.0
### Features
- Added option to change MQTT port in configuration file
### Bug Fixes
- N/A
### Other
- N/A

## Version 1.1.1
### Features
- N/A
### Bug Fixes
- Corrected improper jvmrun file
### Other
- N/A

## Version 1.1.0
### Features
- Added control/status API
### Bug Fixes
- N/A
### Other
- N/A

## Version 1.0.3
### Features
- N/A
### Bug Fixes
- N/A
### Other
- Made jar and jvmrun file available directly from connector releases

## Version 1.0.2
### Features
- N/A
### Bug Fixes
- Fixed bug with queue diagnostic tags not being updated
### Other
- N/A

## Version 1.0.1
### Features
- N/A
### Bug Fixes
- Fixed verification of firmware update operations
  - Improved reporting of firmware update failures
### Other
- N/A


## Version 1.0.0
### Features
- Initial Release
### Bug Fixes
- N/A
### Other
- N/A
