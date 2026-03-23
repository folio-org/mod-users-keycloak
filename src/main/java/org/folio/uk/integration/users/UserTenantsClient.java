package org.folio.uk.integration.users;

import java.util.UUID;
import org.folio.uk.domain.dto.UserTenantCollection;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "user-tenants")
public interface UserTenantsClient {

  default UserTenantCollection lookupByUserId(UUID userId) {
    return lookupByUserIdRequest(userId);
  }

  default UserTenantCollection getOne() {
    return getOneRequest(1);
  }

  default UserTenantCollection getUserTenants(Integer limit, String userName, String email, String phoneNumber,
    String mobilePhoneNumber) {
    return getUserTenantsRequest("or", limit, userName, email, phoneNumber, mobilePhoneNumber);
  }

  default UserTenantCollection lookupByTenantId(String tenantId) {
    return lookupByTenantIdRequest(tenantId);
  }

  @GetExchange
  UserTenantCollection lookupByUserIdRequest(@RequestParam("userId") UUID userId);

  @GetExchange
  UserTenantCollection getOneRequest(@RequestParam("limit") Integer limit);

  @GetExchange
  UserTenantCollection getUserTenantsRequest(@RequestParam("queryOp") String queryOp,
    @RequestParam("limit") Integer limit,
    @RequestParam(value = "userName", required = false) String userName,
    @RequestParam(value = "email", required = false) String email,
    @RequestParam(value = "phoneNumber", required = false)
    String phoneNumber,
    @RequestParam(value = "mobilePhoneNumber", required = false)
    String mobilePhoneNumber);

  @GetExchange
  UserTenantCollection lookupByTenantIdRequest(@RequestParam("tenantId") String tenantId);
}
