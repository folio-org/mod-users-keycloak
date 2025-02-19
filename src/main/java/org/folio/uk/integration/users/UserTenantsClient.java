package org.folio.uk.integration.users;

import java.util.UUID;
import org.folio.uk.domain.dto.UserTenantCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-tenants", dismiss404 = true)
public interface UserTenantsClient {

  @GetMapping(value = "?userId={id}&limit={limit}")
  UserTenantCollection lookupByUserId(@PathVariable("id") UUID userId, @PathVariable("limit") Integer limit);
}
