package org.folio.uk.integration.roles.dafaultrole;

import org.folio.uk.integration.roles.model.LoadableRole;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "loadable-roles")
public interface DefaultRolesClient {

  @PostMapping
  LoadableRole createDefaultRole(@RequestBody LoadableRole role);
}
