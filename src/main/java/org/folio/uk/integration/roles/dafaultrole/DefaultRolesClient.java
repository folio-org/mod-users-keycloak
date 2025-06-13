package org.folio.uk.integration.roles.dafaultrole;

import org.folio.uk.integration.roles.model.LoadableRole;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "loadable-roles")
public interface DefaultRolesClient {

  @PutMapping
  LoadableRole createDefaultLoadableRole(@RequestBody LoadableRole role);
}
