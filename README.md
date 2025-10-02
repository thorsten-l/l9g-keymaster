# L9G Keymaster - Project Overview

This project is a command-line interface (CLI) for managing a Keycloak instance. It is built using Java, Spring Boot, and Spring Shell. The application provides a set of commands to interact with the Keycloak Admin API, allowing users to manage realms, clients, roles, and users.

## Key Technologies

*   **Java 21**
*   **Spring Boot 3.5.6**
*   **Spring Shell 3.4.1**
*   **Keycloak Admin Client 26.0.6**
*   **Maven**

# Building and Running

## Prerequisites

*   Java 21
*   Maven

## Building

To build the project, run the following command:

```bash
mvn clean install
```

This will create a JAR file in the `target` directory.

## Running

To run the application, you need to provide the Keycloak connection details in a `config.yaml` file in the root of the project. The file should have the following content:

```yaml
keycloak:
  realm: "<your-realm>"
  client-secret: "<your-client-secret>"
```

Once the `config.yaml` file is created, you can run the application using the following command:

```bash
java -jar target/keymaster.jar
```

This will start the interactive shell, and you can use the available commands to manage your Keycloak instance.

## Available Commands

*   `delete-realm-roles-with-null-description`: delete realm roles with null description
*   `list-users`: users in selected realm
*   `list-realms`: list realms
*   `list-client-scopes`: list client scopes
*   `list-realm-roles`: list realm roles
*   `list-clients`: list all clients
*   `list-client-roles`: list client roles
*   `show-user`: show user details by username
*   `show-user-by-id`: show user details by ID

# Development Conventions

## Coding Style

The project uses the standard Java coding conventions.

## Contribution

The project is licensed under the Apache-2.0 license. Contributions are welcome.
