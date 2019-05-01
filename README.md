# Workflow Server

## Overview
JMS Spring Boot application that starts ASTRA Workflows / Emulators

### Process:
1. The server receives an initiate message from a client
2. The server starts the relevant workflow
3. If in dummy mode an instance of the the ASTRA/GDA Emulator is also started at the same time
4. The enactment ID is returned to the client


The client uses the enactment ID to communicated directly with the instance of the Workflow ignoring the server i.e. the server only configures and starts a new process containing the ASTRA workflow.  The workflow either terminates itself or is terminated by a message directly from the client.

The server will attach itself and all workflows to the GDA Log Server.

All dependencies required by the Workflow jar are containing in a local Maven Repository

## Installation

Use gradle to build (IDE independent)

```bash
gradle clean build bootJar
```

## Run

Run in live mode (without emulators)

```bash
java -jar build/libs/workflow-server-<version>.jar
```

Run in dummy mode (with emulators)

```bash
java -jar build/libs/workflow-server-<version>.jar --use.emulator=true`
```
