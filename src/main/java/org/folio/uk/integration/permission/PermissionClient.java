package org.folio.uk.integration.permission;

import org.folio.uk.integration.permission.model.PermissionsUsersResponse;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "perms")
public interface PermissionClient {

  @GetExchange("/users")
  PermissionsUsersResponse findByQuery(@RequestParam String query, @RequestParam Integer limit,
    @RequestParam(required = false) Integer offset);
}
