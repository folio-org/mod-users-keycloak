package org.folio.uk.integration.users;

import java.util.UUID;
import org.folio.uk.domain.dto.UserTenantCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "user-tenants", dismiss404 = true)
public interface UserTenantsClient {

  @GetMapping(value = "?userId={id}")
  UserTenantCollection lookupByUserId(@PathVariable("id") UUID userId);

  @GetMapping(value = "?limit=1")
  UserTenantCollection getOne();

  @GetMapping(value = "?queryOp=or")
  UserTenantCollection getUserTenants(@RequestParam Integer limit, @RequestParam String userName,
    @RequestParam String email, @RequestParam String phoneNumber);

  @GetMapping(value = "?tenantId={id}")
  UserTenantCollection lookupByTenantId(@PathVariable("id") String tenantId);
}
