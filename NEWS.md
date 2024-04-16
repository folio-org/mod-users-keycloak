## Version `v1.5.0` (in progress)

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
