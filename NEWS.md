## Version `v4.0.0` (in progress)
### Changes:
* Set application\_name in spring.datasource.url ([https://folio-org.atlassian.net/browse/MODUSERSKC-84](MODUSERSKC-84))
---

## Version `v3.0.0` (14.03.2025)
### Changes:
* PUB-SUB workaround for Eureka approach (MODUSERSKC-78)
* Create migration endpoint to create Keycloak IP links (MODUSERSKC-75)
* Upgrade Java to version 21 (MODUSERSKC-76)
* Link Keycloak users across realms (MODUSERSKC-71)
* Implement custom authenticator for Single Tenant UX login flow (KEYCLOAK-40)
* Add feature flag for setting default credentials (MODUSERSKC-68)
---

## Version `v2.0.0` (01.11.2024)
### Changes:
* Increase keycloak-admin-client to v25.0.6 (KEYCLOAK-24)
* Update role related enum values to upper case (MODUSERSKC-58)
* Add the permission `notify.users.item.post` (MODUSERSKC-57)
* Rename permission (MODPERMS-233)
---

## Version `v1.6.0` (24.09.2024)
### Changes:
*  Bumped up applications-poc-tools dependencies to 1.5.6 to support Hostname Verification for TLS connections
---

* ## Version `v1.5.3` (14.08.2024)
### Changes:
* Return proper response bodies for error cases on creation of Keycloak user - 404 + body in cause Folio user doesn't exist, 400 + body in case Folio user doesn't have a username (MODUSERSKC-48)
---

## Version `v1.5.2` (10.07.2024)
### Changes:
* Upgrade keycloak-client to v25.0.1 (KEYCLOAK-11)
* API to verify and ensure existence of Keycloak user records corresponding to Folio user records (MODUSERSKC-30)

---
## Version `v1.5.1` (20.06.2024)
### Changes:
* Fetched module system user instead of common system user for update and delete events (MODUSERSKC-11)
* Applied OkHttpClient creation from lib with support of keystore custom type and public trusted certs (APPPOCTOOL-20)
* Packed application into Docker Image and save to Image Store (RANCHER-1515)
* Added handling of delete and update system user events (MODUSERSKC-11)

---
## Version `v1.5.0` (25.05.2024)
### Changes:
* Added profile picture link to user personal (EUREKA-95)

---
## Version `v1.4.0` (16.04.2024)
### Changes:
* Added type to system user (MODUSERSKC-34)
* Added endpoint to resolve user permissions (MODUSERSKC-29)
* Protected endpoint by permissions (MODUSERSKC-29)
* Fixed NPE if config code or value is null (MODUSERSKC-37)

---
## Version `v1.3.0` (26.03.2024)
* Added eventual consistency for user update (MODUSERSKC-28).
* HTTPS access to Keycloak (MODUSERSKC-24).

---
## Version `v1.2.0` (27.02.2024)
### Changes:
* Added deleting policy and roles, capability on deleting user (MODROLESKC-126).
* Moved reset token to query params using mod-configuration (MODUSERSKC-12).
* Added env variable that controls _self endpoint permissions scope (MODUSERSKC-18).
