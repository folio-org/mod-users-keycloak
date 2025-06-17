# mod-users-keycloak

Copyright (C) 2023-2023 The Open Library Foundation

This software is distributed under the terms of the Apache License,
Version 2.0. See the file "[LICENSE](LICENSE)" for more information.

## Table of contents

* [Introduction](#introduction)
  * [ModuleDescriptor](#moduledescriptor)
* [API documentation](#api-documentation)
* [Environment Variables](#environment-variables)
  * [Kafka environment variables](#kafka-environment-variables)
  * [System User Environment Variables](#system-user-environment-variables)
  * [Secure storage environment variables](#secure-storage-environment-variables)
    * [AWS-SSM](#aws-ssm)
    * [Vault](#vault)
    * [Folio Secure Store Proxy (FSSP)](#folio-secure-store-proxy-fssp)
  * [Keycloak environment variables](#keycloak-environment-variables)
  * [mod-configuration properties](#mod-configuration-properties)
* [Loading of client IDs/secrets](#loading-of-client-idssecrets)

## Introduction

Business logic "join" module to provide simple access to all user-centric data.

The module combines interfaces normally provided by `mod-users` and `mod-users-bl` Folio modules. Thus its output are
either User model from mod-users or Composite User from mod-users-bl.

The module also operates with Keycloak to manage authentication information of users (`authUser`). Auth users created
and updated in Keycloak on per-realm basis.

### ModuleDescriptor

See the built `target/ModuleDescriptor.json` for the interfaces that this module
requires and provides, the permissions, and the additional module metadata.

## API documentation

API documentation can be generated with the following command:

```shell
mvn clean generate-sources
```

After that the documentation will be available in `target/docs/mod-users-keycloakindex.html`

## Environment Variables

| Name                             | Default value              | Required | Description                                                                                                                          |
|:---------------------------------|:---------------------------|:--------:|:-------------------------------------------------------------------------------------------------------------------------------------|
| DB_HOST                          | localhost                  |  false   | Postgres hostname                                                                                                                    |
| DB_PORT                          | 5432                       |  false   | Postgres port                                                                                                                        |
| DB_USERNAME                      | postgres                   |  false   | Postgres username                                                                                                                    |
| DB_PASSWORD                      | postgres                   |  false   | Postgres username password                                                                                                           |
| DB_DATABASE                      | postgres                   |  false   | Postgres database name                                                                                                               |
| KC_URL                           | -                          |   true   | Keycloak URL used to perform HTTP requests by `KeycloakClient`.                                                                      |
| KC_ADMIN_CLIENT_ID               | folio-backend-admin-client |   true   | Keycloak client id                                                                                                                   |
| KC_ADMIN_GRANT_TYPE              | client_credentials         |  false   | Defines grant type for issuing Keycloak token                                                                                        |
| KC_PASSWORD_RESET_CLIENT_ID      | password-reset-client      |  false   | Keycloak password reset client                                                                                                       |
| KC_ADMIN_TOKEN_TTL               | 60s                        |  false   | ttl value for Keycloak token to persist in cache                                                                                     |
| KC_CONFIG_TTL                    | 3600s                      |  false   | Client credentials expiration timeout                                                                                                |
| KC_LOGIN_CLIENT_SUFFIX           | -login-application         |  false   | Suffix of a Keycloak client who owns the authorization resources. It is used as `audience` for keycloak when evaluating permissions. |
| MIGRATION_BATCH_SIZE             | 20                         |  false   | Batch size for user migration. Max value is 50                                                                                       |
| IDP_MIGRATION_BATCH_SIZE         | 20                         |  false   | Batch size for user identity provider (IDP) linking migration. Max value is 50                                                       |
| DEFAULT_PASSWORDS_ON_MIGRATION   | false                      |  false   | If specified to true migrated user’s password being set to their username otherwise migrated users not having any credentials set    |
| INCLUDE_ONLY_VISIBLE_PERMISSIONS | true                       |  false   | Defines if onlyVisible (UI permissions/permission-set names) will be returned using `_self` endpoint                                 |
| SINGLE_TENANT_UX                 | false                      |  false   | Defines if the module is running in single tenant UX mode                                                                            |
| IDENTITY_PROVIDER_SUFFIX         | -keycloak-oidc             |  false   | Suffix of a Keycloak OIDC identity provider who will perform federated authentication requests. Used if SINGLE_TENANT_UX is enabled  |

### Kafka environment variables

| Name                                | Default value                                                             | Required | Description                                                                                                                                                |
|:------------------------------------|:--------------------------------------------------------------------------|:--------:|:-----------------------------------------------------------------------------------------------------------------------------------------------------------|
| KAFKA_HOST                          | kafka                                                                     |  false   | Kafka broker hostname                                                                                                                                      |
| KAFKA_PORT                          | 9092                                                                      |  false   | Kafka broker port                                                                                                                                          |
| KAFKA_SECURITY_PROTOCOL             | PLAINTEXT                                                                 |  false   | Kafka security protocol used to communicate with brokers (SSL or PLAINTEXT)                                                                                |
| KAFKA_SSL_KEYSTORE_LOCATION         | -                                                                         |  false   | The location of the Kafka key store file. This is optional for client and can be used for two-way authentication for client.                               |
| KAFKA_SSL_KEYSTORE_PASSWORD         | -                                                                         |  false   | The store password for the Kafka key store file. This is optional for client and only needed if 'ssl.keystore.location' is configured.                     |
| KAFKA_SSL_TRUSTSTORE_LOCATION       | -                                                                         |  false   | The location of the Kafka trust store file.                                                                                                                |
| KAFKA_SSL_TRUSTSTORE_PASSWORD       | -                                                                         |  false   | The password for the Kafka trust store file. If a password is not set, trust store file configured will still be used, but integrity checking is disabled. |
| KAFKA_SYS_USER_TOPIC_RETRY_DELAY    | 1s                                                                        |  false   | `system-user` topic retry delay if tenant is not initialized                                                                                               |
| KAFKA_SYS_USER_TOPIC_RETRY_ATTEMPTS | 9223372036854775807                                                       |  false   | `system-user` topic retry attempts if tenant is not initialized (default value is Long.MAX_VALUE ~= infinite amount of retries)                            |
| KAFKA_SYS_USER_TOPIC_PATTERN        | `(${application.environment}\.)(.*\.)mgr-tenant-entitlements.system-user` |  false   | Topic pattern for `system-user` topic filled by mgr-tenants-entitlement                                                                                    |
| KAFKA_SYS_USER_ROLE_RETRY_ATTEMPTS  | 60                                                                        |  false   | Number of retry attempts to upsert loadable roles and assign it to (module) system user                                                                    |
| KAFKA_SYS_USER_ROLE_RETRY_DELAY     | 5s                                                                        |  false   | Duration between retry attempts (with time unit suffix) to upsert loadable roles and assign it to (module) system user                                     |

### System User Environment Variables

| Name                          | Default value                    | Required | Description                                                            |
|:------------------------------|:---------------------------------|:--------:|:-----------------------------------------------------------------------|
| SYSTEM_USER_USERNAME_TEMPLATE | {tenantId}-system-user           |  false   | System user username template, used to generate system user `username` |
| SYSTEM_USER_EMAIL_TEMPLATE    | {tenantId}-system-user@folio.org |  false   | System user email template, used to generate system user `email`.      |
| SYSTEM_USER_ROLE              | System                           |  false   | System user role name. It will be assigned on tenant initialization    |
| SYSTEM_USER_PASSWORD_LENGTH   | 32                               |  false   | Batch size for user migration. Max value is 50                         |
| SYSTEM_USER_RETRY_COUNT       | 10                               |  false   | Number of retry attempts to create a system user                       |
| SYSTEM_USER_RETRY_DELAY       | 250                              |  false   | Amount of milliseconds between retry attempts                          |

### Secure storage environment variables

#### AWS-SSM

Required when `SECRET_STORE_TYPE=AWS_SSM`

| Name                                          | Default value | Description                                                                                                                                                    |
|:----------------------------------------------|:--------------|:---------------------------------------------------------------------------------------------------------------------------------------------------------------|
| SECRET_STORE_AWS_SSM_REGION                   | -             | The AWS region to pass to the AWS SSM Client Builder. If not set, the AWS Default Region Provider Chain is used to determine which region to use.              |
| SECRET_STORE_AWS_SSM_USE_IAM                  | true          | If true, will rely on the current IAM role for authorization instead of explicitly providing AWS credentials (access_key/secret_key)                           |
| SECRET_STORE_AWS_SSM_ECS_CREDENTIALS_ENDPOINT | -             | The HTTP endpoint to use for retrieving AWS credentials. This is ignored if useIAM is true                                                                     |
| SECRET_STORE_AWS_SSM_ECS_CREDENTIALS_PATH     | -             | The path component of the credentials endpoint URI. This value is appended to the credentials endpoint to form the URI from which credentials can be obtained. |

#### Vault

Required when `SECRET_STORE_STORE_TYPE=VAULT`

| Name                                    | Default value | Description                                                                         |
|:----------------------------------------|:--------------|:------------------------------------------------------------------------------------|
| SECRET_STORE_VAULT_TOKEN                | -             | token for accessing vault, may be a root token                                      |
| SECRET_STORE_VAULT_ADDRESS              | -             | the address of your vault                                                           |
| SECRET_STORE_VAULT_ENABLE_SSL           | false         | whether or not to use SSL                                                           |
| SECRET_STORE_VAULT_PEM_FILE_PATH        | -             | the path to an X.509 certificate in unencrypted PEM format, using UTF-8 encoding    |
| SECRET_STORE_VAULT_KEYSTORE_PASSWORD    | -             | the password used to access the JKS keystore (optional)                             |
| SECRET_STORE_VAULT_KEYSTORE_FILE_PATH   | -             | the path to a JKS keystore file containing a client cert and private key            |
| SECRET_STORE_VAULT_TRUSTSTORE_FILE_PATH | -             | the path to a JKS truststore file containing Vault server certs that can be trusted |

#### Folio Secure Store Proxy (FSSP)

Required when `SECRET_STORE_TYPE=FSSP`

| Name                                   | Default value         | Description                                          |
|:---------------------------------------|:----------------------|:-----------------------------------------------------|
| SECRET_STORE_FSSP_ADDRESS              | -                     | The address (URL) of the FSSP service.               |
| SECRET_STORE_FSSP_SECRET_PATH          | secure-store/entries  | The path in FSSP where secrets are stored/retrieved. |
| SECRET_STORE_FSSP_ENABLE_SSL           | false                 | Whether to use SSL when connecting to FSSP.          |
| SECRET_STORE_FSSP_TRUSTSTORE_PATH      | -                     | Path to the truststore file for SSL connections.     |
| SECRET_STORE_FSSP_TRUSTSTORE_FILE_TYPE | -                     | The type of the truststore file (e.g., JKS, PKCS12). |
| SECRET_STORE_FSSP_TRUSTSTORE_PASSWORD  | -                     | The password for the truststore file.                |

### Keycloak environment variables

Keycloak all configuration properties: https://www.keycloak.org/server/all-config

| Name                              | Description                                                                                                                                                                |
|:----------------------------------|:---------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| KC_HOSTNAME                       | Keycloak hostname, will be added to returned endpoints, for example for openid-configuration                                                                               |
| KC_ADMIN                          | Initial admin username                                                                                                                                                     |
| KC_ADMIN_PASSWORD                 | Initial admin password                                                                                                                                                     |
| KC_DB                             | Database type                                                                                                                                                              |
| KC_DB_URL_DATABASE                | Sets the database name of the default JDBC URL of the chosen vendor. If the DB_URL option is set, this option is ignored.                                                  |
| KC_DB_URL_HOST                    | Sets the hostname of the default JDBC URL of the chosen vendor. If the DB_URL option is set, this option is ignored.                                                       |
| KC_DB_URL_PORT                    | Sets the port of the default JDBC URL of the chosen vendor. If the DB_URL option is set, this option is ignored.                                                           |
| KC_DB_USERNAME                    | Database Username                                                                                                                                                          |
| KC_DB_PASSWORD                    | Database Password                                                                                                                                                          |
| KC_PROXY                          | The proxy address forwarding mode if the server is behind a reverse proxy. Possible values are: edge, reencrypt, passthrough. https://www.keycloak.org/server/reverseproxy |
| KC_HOSTNAME_STRICT                | Disables dynamically resolving the hostname from request headers. Should always be set to true in production, unless proxy verifies the Host header.                       |
| KC_HOSTNAME_PORT                  | The port used by the proxy when exposing the hostname. Set this option if the proxy uses a port other than the default HTTP and HTTPS ports. Defaults to -1.               |
| KC_CLIENT_TLS_ENABLED             | Enables TLS for keycloak clients.                                                                                                                                          |
| KC_CLIENT_TLS_TRUSTSTORE_PATH     | Truststore file path for keycloak clients.                                                                                                                                 |
| KC_CLIENT_TLS_TRUSTSTORE_PASSWORD | Truststore password for keycloak clients.                                                                                                                                  |
| KC_CLIENT_TLS_TRUSTSTORE_TYPE     | Truststore file type for keycloak clients.                                                                                                                                 |

### mod-configuration properties

Configuration properties must be created for a module: `USERSBL`

Request example:

```HTTP
POST /configurations/entries HTTP/1.1
Host: <gateway>
x-okapi-tenant: <tenant_name>
x-okapi-token: <auth-token>

{
  "module": "USERSBL",
  "configName": "validation_rules",
  "code": "FOLIO_HOST",
  "description": "Host value for password reset",
  "default": true,
  "enabled": true,
  "value": "https://ui-host:3000"
}
```

> **_NOTE:_** Fields `code` and `value` are mandatory, if any of them are not specified - default value will be used.

| Name                                        |     Default value     | Description                                                                                                                                       |
|:--------------------------------------------|:---------------------:|:--------------------------------------------------------------------------------------------------------------------------------------------------|
| FOLIO_HOST                                  | http://localhost:3000 | Folio host used to generate password reset link                                                                                                   |
| RESET_PASSWORD_UI_PATH                      |    /reset-password    | UI path, added to the folio host, took value firstly from `mod-configuration`, then from application property `reset-password.ui-path.default`    |
| RESET_PASSWORD_LINK_EXPIRATION_TIME         |          24           | A duration value when the reset password token will be expired                                                                                    |
| RESET_PASSWORD_LINK_EXPIRATION_UNIT_OF_TIME |         hours         | A duration unit when the reset password token will be expired                                                                                     |
| PUT_RESET_TOKEN_IN_QUERY_PARAMS             |         false         | Defines if reset token will be included in the path (if value is not set or set as `false`) or as a query parameter (if value is set to a `true`) |

## Loading of client IDs/secrets

The module pulls client_secret for client_id from AWS Parameter store, Vault or other reliable secret storages when they
are required for login. The credentials are cached for 3600s.
