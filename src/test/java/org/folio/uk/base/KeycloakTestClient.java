package org.folio.uk.base;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import java.util.List;
import org.folio.uk.base.model.IdentityProvider;
import org.folio.uk.integration.keycloak.config.KeycloakFeignClientConfig;
import org.folio.uk.integration.keycloak.model.FederatedIdentity;
import org.folio.uk.integration.keycloak.model.KeycloakRole;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

// Endpoints required purely for integration testing
// against a real Keycloak instance
@FeignClient(name = "keycloakTestClient",
  url = "#{keycloakProperties.url}",
  configuration = KeycloakFeignClientConfig.class)
public interface KeycloakTestClient {

  @PostMapping("/admin/realms/{realm}/roles/")
  void create(@PathVariable String realm,
    @RequestHeader(AUTHORIZATION) String token,
    @RequestBody KeycloakRole keycloakRole);

  @GetMapping("/admin/realms/{realm}/users/{userId}/federated-identity")
  List<FederatedIdentity> getUserIdentityProvider(@PathVariable("realm") String realm,
                                                  @PathVariable("userId") String userId,
                                                  @RequestHeader(AUTHORIZATION) String token);

  @PostMapping("/admin/realms/{realm}/identity-provider/instances")
  void createIdentityProvider(@PathVariable("realm") String realm,
                              @RequestBody IdentityProvider identityProvider,
                              @RequestHeader(AUTHORIZATION) String token);

  @DeleteMapping("/admin/realms/{realm}/identity-provider/instances/{providerAlias}")
  void removeIdentityProvider(@PathVariable("realm") String realm,
                              @PathVariable("providerAlias") String providerAlias,
                              @RequestHeader(AUTHORIZATION) String token);
}
