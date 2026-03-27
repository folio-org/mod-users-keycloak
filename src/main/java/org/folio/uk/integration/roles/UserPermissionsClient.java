package org.folio.uk.integration.roles;

import java.util.List;
import java.util.UUID;
import org.folio.uk.integration.roles.model.UserPermissions;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "permissions")
public interface UserPermissionsClient {

  @GetExchange("/users/{id}")
  UserPermissions getPermissionsForUser(@PathVariable("id") UUID id, @RequestParam("onlyVisible") Boolean onlyVisible,
    @RequestParam(value = "desiredPermissions", required = false) List<String> desiredPermissions);
}
