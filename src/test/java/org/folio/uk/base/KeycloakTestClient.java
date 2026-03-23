package org.folio.uk.base;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.List;
import org.folio.uk.base.model.IdentityProvider;
import org.folio.uk.integration.keycloak.model.FederatedIdentity;
import org.folio.uk.integration.keycloak.model.KeycloakRole;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

// Endpoints required purely for integration testing
// against a real Keycloak instance
@HttpExchange
public interface KeycloakTestClient {

  @PostExchange(value = "/admin/realms/{realm}/roles/", contentType = APPLICATION_JSON_VALUE)
  void create(@PathVariable String realm,
    @RequestHeader(AUTHORIZATION) String token,
    @RequestBody KeycloakRole keycloakRole);

  @GetExchange("/admin/realms/{realm}/users/{userId}/federated-identity")
  List<FederatedIdentity> getUserIdentityProvider(@PathVariable("realm") String realm,
                                                  @PathVariable("userId") String userId,
                                                  @RequestHeader(AUTHORIZATION) String token);

  @PostExchange(value = "/admin/realms/{realm}/identity-provider/instances", contentType = APPLICATION_JSON_VALUE)
  void createIdentityProvider(@PathVariable("realm") String realm,
                              @RequestBody IdentityProvider identityProvider,
                              @RequestHeader(AUTHORIZATION) String token);

  @DeleteExchange("/admin/realms/{realm}/identity-provider/instances/{providerAlias}")
  void removeIdentityProvider(@PathVariable("realm") String realm,
                              @PathVariable("providerAlias") String providerAlias,
                              @RequestHeader(AUTHORIZATION) String token);
}
