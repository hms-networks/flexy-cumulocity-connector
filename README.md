# Ewon Flexy Cumulocity Connector
Copyright © 2021 HMS Industrial Networks Inc.

## About
This application provides a connector-based solution development kit for linking the Ewon Flexy to a Cumulocity IoT server.

### Required Firmware Version

This project requires a minimum Ewon firmware version of 14.0 or higher. Older firmware versions may be incompatible and are not supported.

### Libraries and Dependencies

1. Ewon ETK
   ```xml
   <dependencies>
      ...
      <dependency>
         <groupId>com.hms_networks.americas.sc.mvnlibs</groupId>
         <artifactId>ewon-etk</artifactId>
         <version>1.4.4</version>
         <scope>provided</scope>
      </dependency>
      ...
   </dependencies>
   ```
   _Note: The scope must be set to 'provided' for the Ewon ETK dependency. This indicates that the library is provided by the system and does not need to be included in the packaged JAR file._
2. JUnit
   ```xml
   <dependencies>
      ...
      <dependency>
         <groupId>junit</groupId>
         <artifactId>junit</artifactId>
         <version>3.8.1</version>
         <scope>test</scope>
      </dependency>
      ...
   </dependencies>
   ```
   _Note: The scope must be set to 'test' for the JUnit dependency. This indicates that the library is required for code testing and does not need to be included in the packaged JAR file._
3. Ewon Flexy Extensions Library
   ```xml
   <dependencies>
      ...
      <dependency>
         <groupId>com.hms_networks.americas.sc</groupId>
         <artifactId>extensions</artifactId>
         <version>1.0.0</version>
      </dependency>
      ...
   </dependencies>
   ```

As required, you can include additional libraries or dependencies using the Maven build system. To add a new library or dependency, add a new `<dependency></dependency>` block in the `<dependencies></dependencies>` section of your `pom.xml`.

## Confidentiality Notice
This repository contains HMS Industrial Networks Inc. confidential information which may be subject to trade secrets, copyright and/or patent protection. Do not disclose, distribute, or copy any information from this repository without approval by HMS Industrial Networks Inc.

## Development Environment

This project is based on the [Solution Center Maven Starter Project](https://github.com/hms-networks/sc-java-maven-starter-project), and uses the Maven build system for compilation, testing, and packaging.

Maven lifecycle information and other details about the development environment provided by the [Solution Center Maven Starter Project](https://github.com/hms-networks/sc-java-maven-starter-project) can be found in its README.md at [https://github.com/hms-networks/sc-java-maven-starter-project/blob/main/README.md](https://github.com/hms-networks/sc-java-maven-starter-project/blob/main/README.md).

## Contributing

Detailed information about contributing to this project can be found in [CONTRIBUTING.md](CONTRIBUTING.md).