package org.folio.uk.integration.roles;

import java.util.UUID;
import org.folio.uk.integration.roles.model.UserPermissions;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "permissions")
public interface UserPermissionsClient {

  @GetMapping("/users/{id}")
  UserPermissions getPermissionsForUser(@PathVariable("id") UUID id, @RequestParam("onlyVisible") Boolean onlyVisible);
}
