package org.folio.uk.integration.permission;

import org.folio.uk.integration.permission.model.PermissionsUsersResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "perms")
public interface PermissionClient {

  @GetMapping("/users")
  PermissionsUsersResponse findByQuery(@RequestParam String query, @RequestParam Integer limit,
                                       @RequestParam Integer offset);
}
