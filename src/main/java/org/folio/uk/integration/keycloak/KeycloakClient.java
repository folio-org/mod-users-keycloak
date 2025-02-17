package org.folio.uk.integration.keycloak;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.folio.uk.integration.keycloak.config.KeycloakFeignClientConfig;
import org.folio.uk.integration.keycloak.model.Client;
import org.folio.uk.integration.keycloak.model.FederatedIdentity;
import org.folio.uk.integration.keycloak.model.KeycloakRole;
import org.folio.uk.integration.keycloak.model.KeycloakUser;
import org.folio.uk.integration.keycloak.model.ScopePermission;
import org.folio.uk.integration.keycloak.model.TokenResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "keycloak",
  url = "#{keycloakProperties.url}",
  configuration = KeycloakFeignClientConfig.class)
public interface KeycloakClient {

  @PostMapping(value = "/realms/master/protocol/openid-connect/token", consumes = APPLICATION_FORM_URLENCODED_VALUE)
  TokenResponse login(@RequestBody Map<String, ?> loginRequest);

  @PostMapping(value = "/realms/{tenant}/protocol/openid-connect/token", consumes = APPLICATION_FORM_URLENCODED_VALUE)
  TokenResponse login(@RequestBody Map<String, ?> loginRequest, @PathVariable("tenant") String tenant);

  @PostMapping(value = "/admin/realms/{realm}/users", consumes = APPLICATION_JSON_VALUE)
  ResponseEntity<Void> createUser(@PathVariable("realm") String realmName,
    @RequestBody KeycloakUser user,
    @RequestHeader(AUTHORIZATION) String token);

  @GetMapping(value = "/admin/realms/{realm}/users/{id}", produces = APPLICATION_JSON_VALUE)
  KeycloakUser getUser(@PathVariable("realm") String realmName,
    @PathVariable("id") String userId,
    @RequestHeader(AUTHORIZATION) String token);

  @GetMapping(value = "/admin/realms/{realm}/users?q={attrQuery}&briefRepresentation={brief}",
    produces = APPLICATION_JSON_VALUE)
  List<KeycloakUser> getUsersWithAttrs(@PathVariable("realm") String realmName,
    @PathVariable("attrQuery") String attrQuery,
    @PathVariable("brief") boolean briefRepresentation,
    @RequestHeader(AUTHORIZATION) String token);

  @PutMapping(value = "/admin/realms/{realm}/users/{id}", consumes = APPLICATION_JSON_VALUE)
  void updateUser(@PathVariable("realm") String realmName,
    @PathVariable("id") String userId,
    @RequestBody KeycloakUser user,
    @RequestHeader(AUTHORIZATION) String token);

  @DeleteMapping(value = "/admin/realms/{realm}/users/{id}")
  void deleteUser(@PathVariable("realm") String realmName,
    @PathVariable("id") String userId,
    @RequestHeader(AUTHORIZATION) String token);

  /**
   * Searches users by username exactly.
   *
   * @param realmName - realm name
   * @param username - username
   * @param briefRepresentation - Boolean which defines whether brief representations are returned
   * @param token - keycloak authentication token
   * @return {@link List} with found {@link KeycloakUser} objects
   */
  @GetMapping(value = "/admin/realms/{realm}/users?exact=true&first=0&max=1",
    produces = APPLICATION_JSON_VALUE)
  List<KeycloakUser> findUsersByUsername(@PathVariable("realm") String realmName,
    @RequestParam("username") String username,
    @RequestParam("briefRepresentation") boolean briefRepresentation,
    @RequestHeader(AUTHORIZATION) String token);

  /**
   * Find single role by name.
   *
   * @param role - name of searching role
   * @param token - authorization header value
   * @return single {@link KeycloakRole} object.
   */
  @GetMapping("/admin/realms/{realm}/roles/{role}")
  KeycloakRole findByName(@PathVariable("realm") String realm, @PathVariable("role") String role,
    @RequestHeader(AUTHORIZATION) String token);

  /**
   * Assign all request's roles from user. If role is not assigned to user, keycloak returns 204. If there is no at
   * least one of the request's roles in realm, keycloak returns 404 and doesn't assign any role from request.
   *
   * @param realm - tenant identifier
   * @param userId - keycloak user unique identifier
   */
  @PostMapping("/admin/realms/{realm}/users/{userId}/role-mappings/realm")
  void assignRolesToUser(@PathVariable("realm") String realm, @PathVariable("userId") String userId,
    @RequestBody List<KeycloakRole> request, @RequestHeader(AUTHORIZATION) String token);

  /**
   * Provides a list of keycloak user roles.
   *
   * @param realm - realm name
   * @param userId - user identifier
   * @param token - authentication token
   * @return {@link List} with {@link KeycloakRole} object linked with user
   */
  @GetMapping("/admin/realms/{realm}/users/{userId}/role-mappings/realm")
  List<KeycloakRole> findUserRoles(@PathVariable("realm") String realm, @PathVariable("userId") String userId,
    @RequestHeader(AUTHORIZATION) String token);

  @PostMapping("/admin/realms/{realmId}/clients/{clientId}/authz/resource-server/permission/scope")
  ScopePermission createScopePermission(@PathVariable("realmId") String realmId,
    @PathVariable("clientId") UUID clientId,
    @RequestBody ScopePermission scopePermission,
    @RequestHeader(AUTHORIZATION) String token);

  @GetMapping("/admin/realms/{realmId}/clients/{clientId}/authz/resource-server/permission/scope?first=0&max=100")
  List<ScopePermission> findScopePermission(@PathVariable("realmId") String realmId,
    @PathVariable("clientId") UUID clientId,
    @RequestParam("name") String name,
    @RequestHeader(AUTHORIZATION) String token);

  @DeleteMapping("/admin/realms/{realmId}/clients/{clientId}/authz/resource-server/permission/scope/{permissionId}")
  void deleteScopePermission(@PathVariable("realmId") String realmId,
    @PathVariable("clientId") UUID clientId,
    @PathVariable("permissionId") String permissionId,
    @RequestHeader(AUTHORIZATION) String token);

  @GetMapping(value = "/admin/realms/{realmId}/clients?clientId={clientId}")
  List<Client> findClientsByClientId(@PathVariable("realmId") String realmId,
    @PathVariable("clientId") String clientId,
    @RequestHeader(AUTHORIZATION) String token);

  /**
   * Get identity provider linked to a user.
   *
   * @param realm - tenant identifier
   * @param userId - keycloak user unique identifier
   */
  @GetMapping("/admin/realms/{realm}/users/{userId}/federated-identity")
  List<FederatedIdentity> getUserIdentityProvider(@PathVariable("realm") String realm,
                                                  @PathVariable("userId") String userId,
                                                  @RequestHeader(AUTHORIZATION) String token);

  /**
   * Link an identity provider to user.
   *
   * @param realm - tenant identifier
   * @param userId - keycloak user unique identifier
   * @param providerAlias - keycloak identity provider alias
   * @param federatedIdentity - federated identity payload
   * @param token - authorization token
   */
  @PostMapping("/admin/realms/{realm}/users/{userId}/federated-identity/{providerAlias}")
  void linkIdentityProviderToUser(@PathVariable("realm") String realm,
                                  @PathVariable("userId") String userId,
                                  @PathVariable("providerAlias") String providerAlias,
                                  @RequestBody FederatedIdentity federatedIdentity,
                                  @RequestHeader(AUTHORIZATION) String token);
}
