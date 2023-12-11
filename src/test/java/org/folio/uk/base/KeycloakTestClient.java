package org.folio.uk.base;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import org.folio.uk.integration.keycloak.config.KeycloakFeignClientConfig;
import org.folio.uk.integration.keycloak.model.KeycloakRole;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "keycloakTestClient",
  url = "#{keycloakProperties.url}",
  configuration = KeycloakFeignClientConfig.class)
public interface KeycloakTestClient {

  @PostMapping("/admin/realms/{realm}/roles/")
  void create(@PathVariable String realm,
    @RequestHeader(AUTHORIZATION) String token,
    @RequestBody KeycloakRole keycloakRole);
}
