# Ewon Flexy Cumulocity Connector

Copyright © 2022 HMS Industrial Networks Inc.

The Ewon Flexy Cumulocity Connector package provides a connector-based solution to Cumulocity for
linking Ewon devices using a direct data path with a Flexy Java application.

## Table of contents

$$$
MOVED ALREADY OR NOT APPLICABLE
$$$

## Installation

$$$
MOVED ALREADY OR NOT APPLICABLE
$$$

### Required Ewon Firmware Version

$$$
MOVED ALREADY OR NOT APPLICABLE
$$$

### Network Requirements

By default, this project connects to Cumulocity using MQTT over port 8883. Port 8883 must be
permitted on the connected Ewon network(s). If the port has been changed, as described in
the [Port (Port) configuration section](#port-port), the desired port must be permitted on the
connected Ewon network(s).

## Configuration

$$$
MOVED ALREADY OR NOT APPLICABLE
$$$

## Telemetry

$$$$
NOTE: There is no current comparable section in the generic documentation.
This should be generalized, refined, and added to the common docs repo,
as well as ported over to the starter project
(for easier/easiest distribution to other projects as required/in the future).
There is a lot of feature alignment between this and the Canary connector, so most documentation
developed for either should be easily portable to the other.
$$$

### Data Source

The telemetry data that is sent to Cumulocity is gathered from the internal Ewon Flexy historical
logs. These logs act as a first-in, first-out (FIFO) buffer, meaning that Cumulocity will receive
the oldest data points from the logs first. The historical logs are stored in nonvolatile memory and
prevent against data point loss from connectivity issues or power loss. The historical log can store
up to 900,000 data points, depending on the memory configuration, before data points are dropped.

The default configuration of the Ewon Flexy is to allocate 6 MB of memory to the historical log and
29 MB to the /usr directory on the file system. Increasing the size of the historical log will
result in a proportional decrease of the size of the /usr directory. This setting can be configured
on the Ewon under Setup > System > Storage > Memory Settings.

Note: *This setting should be configured prior to installing the application, as a complete format
of the Ewon is necessary to apply this setting.*

![Data Source Flow Chart](images/DataSource.png)

#### Tag Eligibility

Each tag that should be sent to Cumulocity must have historical logging enabled. Please
visit [https://www.ewon.biz/technicalsupport/pages/data-services/data-logging](https://www.ewon.biz/technicalsupport/pages/data-services/data-logging)
for information on the Ewon’s historical logging functionality, and how to set it up.

In addition to historical logging being enabled, the Ewon Flexy Cumulocity Connector application
uses tag groups to determine which tags are to be sent to Cumulocity. There are four tag groups, A,
B, C, and D. Any tag assigned to one (or more) of the four tag groups will be sent to Cumulocity,
but tags that have not been assigned a tag group will be ignored.

##### Tag Data Types

The Ewon Flexy Cumulocity connector supports the following Ewon tag data types:

- Integer
- Boolean
- Floating Point
- DWORD
- String

###### String Tag History

The string data type requires an additional EBD (export block descriptor) call, which requires
additional processing power. It is recommended that the string data type be disabled if string tags
will not be used. It can be enabled or disabled as described in
the [Queue Enable String History](#queue-enable-string-history) section under
the [Configuration](#configuration) heading.

Additionally, the default configuration of the Ewon Flexy is to exclude string tags from the
historical log. String tag historization can be enabled on the Ewon under *Setup > System >
Storage > Memory Settings*.

Note: *This setting should be configured prior to installing the application, as a complete format
of the Ewon is necessary to apply this setting.*

### Data Aggregation

$$$
MOVED ALREADY OR NOT APPLICABLE
$$$

## Runtime

### Child Device Support

$$$
MOVED ALREADY OR NOT APPLICABLE
$$$

### Connector Halt Tag

The “CumulocityConnectorHalt” tag allows for a user to halt, or shut down, the application while the
Flexy is running. The application will cyclically poll the “CumulocityConnectorHalt” tag value and
shut down the application when the value is set to one (1). This reduces the CPU load of the Flexy
and allows for maintenance to be completed on the unit. The application can only be stopped in the
telemetry portion of the application and shut down during initialization is not permitted.

### Supported Cumulocity Operations

$$$
MOVED ALREADY OR NOT APPLICABLE
$$$

### Commands from Cumulocity

$$$
MOVED ALREADY OR NOT APPLICABLE
$$$

### REST API

$$$
MOVED ALREADY OR NOT APPLICABLE
$$$

### Log Output

$$$
MOVED ALREADY OR NOT APPLICABLE
$$$

## Development Environment

$$$
MOVED ALREADY OR NOT APPLICABLE
$$$

## Support

$$$
MOVED ALREADY OR NOT APPLICABLE
$$$