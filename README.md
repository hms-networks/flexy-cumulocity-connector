# Solution Center Java Maven Starter Project (sc-java-maven-starter-project)

THE PROJECT IS PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND. HMS DOES NOT WARRANT THAT THE FUNCTIONS OF THE PROJECT WILL MEET YOUR REQUIREMENTS, OR THAT THE OPERATION OF THE PROJECT WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT DEFECTS IN IT CAN BE CORRECTED.
---

## [Table of Contents](#table-of-contents)

1. [Description](#description)
   1. [Required Maven and Java Version](#required-maven-and-java-version)
   2. [Required Firmware Version](#required-firmware-version)
   3. [Libraries and Dependencies](#libraries-and-dependencies)
      1. [Solution Center Repository](#solution-center-repository)
2. [Development Environment](#development-environment)
   1. [Getting Project Name and Version via Maven](#getting-project-name-and-version-via-maven)
   2. [Testing with JUnit](#testing-with-junit)
   3. [IDEs](#ides)
   4. [Command-Line](#command-line)
   5. [Support Notice](#support-notice)
3. [Development Lifecycles](#development-lifecycles)
   1. [Clean Lifecycle](#clean-lifecycle)
   2. [Package Lifecycle](#package-lifecycle)
   3. [Install Lifecycle](#install-lifecycle)
   4. [Deploy Lifecycle](#deploy-lifecycle)
      1. [Deploy Lifecycle (noDebug)](#deploy-lifecycle-nodebug)
      2. [Deploy Lifecycle (debug)](#deploy-lifecycle-debug)
4. [Contributing](#contributing)

---

## [Description](#table-of-contents)

A basic starting project for Java applications developed on the Ewon JTK using Maven. 

This project is intended to replace the functionality provided by the Ewon JTK's `build.xml` Ant build file for projects.

### [Required Maven and Java Version](#table-of-contents)

This project has been designed to work with the latest versions of Maven and Java. There is no specific version of Maven or Java required, although testing has been successfully completed on a host using Java 16 and Maven 3.6.3. 

During the Maven [package lifecycle](#package-lifecycle), a compatible JDK will be automatically downloaded and used to compile the source code. This allows for better cross-platform developer support, compilation consistency, and enables the use of modern Java and Maven environments on the host by isolating source code compilation.

### [Required Firmware Version](#table-of-contents)

This project requires a minimum Ewon firmware version of 14.0 or higher. Older firmware versions may be incompatible and are not supported.

### [Libraries and Dependencies](#table-of-contents)

This project itself does not require any libraries or dependencies. For your convenience, the Ewon ETK is already included as a dependency though. 

If you removed the Ewon JTK dependency, or otherwise need to re-add it, you can include it by adding the `<dependency></dependency>` block in the `<dependencies></dependencies>` section of your `pom.xml` as follows:
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
_Note: The scope must be set to 'provided' for the Ewon ETK. This indicates the the library is provided by the system and does not need to be included in the packaged JAR file._

As required, you can include additional libraries or dependencies using the Maven build system. To add a new library or dependency, add a new `<dependency></dependency>` block in the `<dependencies></dependencies>` section of your `pom.xml`. For example,

```xml
<dependencies>
   ...
   <dependency>
      <groupId>com.hms_networks.americas.sc.mvnlibs</groupId>
      <artifactId>sc-flexy-tag-info-lib</artifactId>
      <version>1.2</version>
   </dependency>
   ...
</dependencies>
```

#### [Solution Center Repository](#table-of-contents)

The Ewon ETK and HMS Americas Solution Center libraries are available via the solution center repository. For your convenience, the solution center repository is already included though.

If you removed the solution center repository, or otherwise need to re-add it, you can include it by adding the following `<repository></repository>` block in the `<repositories></repositories>` section of your `pom.xml` as follows:
```xml
<repositories>
   ...
   <!-- HMS Networks, MU Americas Solution Center Maven Repo -->
   <repository>
      <id>sc-java-maven-repo</id>
      <name>HMS Networks, MU Americas Solution Center Maven Repo</name>
      <url>https://github.com/hms-networks/sc-java-maven-repo/raw/main/</url>
   </repository>
   ...
</repositories>
```

## [Development Environment](#table-of-contents)

This project uses the Maven build system to automatically download libraries and dependencies, and to ensure consistent build behavior.

### [Getting Project Name and Version via Maven](#table-of-contents)

This project includes additional metadata in the JAR file manifest, including the project's name and version from `pom.xml`.

To alleviate the requirement for including a hardcoded project name or version in source code, you can access those properties as described:

```java
String projectName = ExampleMain.class.getPackage().getImplementationTitle();
String projectVersion = ExampleMain.class.getPackage().getImplementationVersion();
```

_Note: The required metadata is only included when the project is packaged via Maven. If you package the project using the `build.xml` Ant build file, the described method of accessing the project name and version may not work or could cause an exception._

### [Testing with JUnit](#table-of-contents)

This project includes basic support for unit testing via the JUnit 3.8.1 test framework. An example test class has been included in this project at `src/test/java/ExampleTest.java`. For detailed information about JUnit 3.8.1 and its capabilities, please refer to [http://junit.sourceforge.net/junit3.8.1/](http://junit.sourceforge.net/junit3.8.1/).

For details about the unit testing in this project, refer to the [test lifecycle](#test-lifecycle) section.

### [IDEs](#table-of-contents)

Support for the following IDEs has been included for this project:

|                             IDE                            | General Config Location(s) |  Launch Config Location(s) |                                                                                                                                                                                   Notes                                                                                                                                                                                  |
|:----------------------------------------------------------:|:--------------------------:|:--------------------------:|:------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------:|
|     [Eclipse Foundation IDE](https://www.eclipse.org/)     |  '.project', '.classpath'  | '.eclipse-launch-configs/' |                                                                                                                                                                                   None                                                                                                                                                                                   |
| [JetBrains IntelliJ IDEA](https://www.jetbrains.com/idea/) |          '.idea/'          | '.idea/runConfigurations/' | The remote debug configuration does not wake  the Ewon debug session from the suspend state. Workaround: Update the remote debug configuration  to use the 'debugNoSuspend' profile. The Ewon will  begin its debug session immediately, therefore,  breakpoints at the beginning of the application  execution may be missed prior to the IDE debug session connecting. |
|     [Visual Studio Code](https://code.visualstudio.com)    |         '.vscode/'         |    '.vscode/tasks.json'    |                                                                   No support has been included for remote debugging of applications on the Ewon. If desired, a remote JVM debugging session will need to be created as described in the [Deploy Lifecycle (debug)] ( #deploy-lifecycle-debug) section.                                                                   |

Note: Additional IDEs with support for the Maven build system may be supported by this project, but have not been tested.

### [Command-Line](#table-of-contents)

Maven includes extensive support for the command-line interface (CLI). For more information about Maven command-line interface support, please refer to [https://maven.apache.org/run.html](https://maven.apache.org/run.html).

### [Support Notice](#table-of-contents)

While this project is intended to replace the functionality provided by the Ewon JTK's `build.xml` Ant build file, the Ewon-supplied `build.xml` Ant build file remains the only supported environment for Ewon Java development. For more information about the official Ewon-supplied `build.xml` Ant build file, please refer to [https://developer.ewon.biz/content/java-0](https://developer.ewon.biz/content/java-0).

This project does not interact with the `build.xml` Ant build file, or the Ant build system entirely, therefore, it is possible to use the official Ewon-supplied `build.xml` Ant build file in conjunction with the supplied `pom.xml` Maven build file. 

## [Development Lifecycles](#table-of-contents)

The supplied `pom.xml` Maven build file includes support for automatically downloading a supported JDK and compiling for the Ewon Flexy ETK version. Additionally, Maven profiles have been included which fully enable remote application debugging on the Ewon Flexy.

### [Clean Lifecycle](#table-of-contents)

The Maven clean lifecycle can be run with the `CLEAN (Remove Build Output)` launch configuration in your IDE, or with the following command: `mvn clean -f pom.xml`.

During this lifecycle, previous build output and artifacts will be cleaned up, and the entire build output directory deleted. 

This lifecycle is not automatically invoked and must be manually run.

### [Test Lifecycle](#table-of-contents)

The Maven test lifecycle can be run with the `TEST (Run JUnit)` launch configuration in your IDE, or with the following command: `mvn test -f pom.xml`.

During this lifecycle, the JUnit test classes in the `src/test/java` class will be run by Maven. In the event of a test failure, the lifecycle will fail. Detailed failure or successful test information can be found in the `target/surefire-reports` folder.

Detailed information about the Maven Surefure Plugin, which is automatically used for JUnit testing, can be found at [https://maven.apache.org/surefire/maven-surefire-plugin/](https://maven.apache.org/surefire/maven-surefire-plugin/).

This lifecycle is automatically invoked by the [package](#package-lifecycle), [install](#install-lifecycle), and [deploy](#deploy-lifecycle) lifecycles.

### [Package Lifecycle](#table-of-contents)

The Maven package lifecycle can be run with the `PACKAGE (Create JAR)` launch configuration in your IDE, or with the following command: `mvn package -f pom.xml`.

During this lifecycle, the application source code and resources will be compiled and packaged in to a JAR file with the name '{artifactId}-{version}-full.jar'. 

This lifecycle is automatically invoked by the [install](#install-lifecycle) and [deploy](#deploy-lifecycle) lifecycles.

### [Install Lifecycle](#table-of-contents)

The Maven install lifecycle can be run with the `INSTALL (Upload to Device)` launch configuration in your IDE, or with the following command: `mvn install -f pom.xml`.

During this lifecycle, the packaged application will be uploaded to the Ewon device using the 'ewon.address,' 'ewon.username,' and 'ewon.password' properties. 

This lifecycle is automatically invoked by the [deploy](#deploy-lifecycle) lifecycle, and automatically invokes [package lifecycle](#package-lifecycle).

### [Deploy Lifecycle](#table-of-contents)

The Maven deploy lifecycle supports multiple Maven profiles, and can be invoked in different ways.

During this lifecycle, the packaged and uploaded application will be run on the Ewon Flexy device. 

This lifecycle automatically invokes [install](#install-lifecycle) and [package](#package-lifecycle) lifecycles.

#### [Deploy Lifecycle (noDebug)](#table-of-contents)

The Maven deploy lifecycle can be run without debugging enabled using the `DEPLOY (Run on Device, No Debug)` launch configuration in your IDE, or with the following command: `mvn deploy -f pom.xml -P noDebug`.

#### [Deploy Lifecycle (debug)](#table-of-contents)

The Maven deploy lifecycle can be run with debugging enabled using the `DEPLOY (Run on Device, Debug)` launch configuration in your IDE. 

The Maven deploy lifecycle with debugging enabled can also be run with the following command: `mvn deploy -f pom.xml -P debug`, but a remote JVM debugging connection must be manually created using the values from the 'ewon.address' and 'project.build.debug.port' properties.

## [Contributing](#table-of-contents)

Detailed information about contributing to this project can be found in [CONTRIBUTING.md](CONTRIBUTING.md).