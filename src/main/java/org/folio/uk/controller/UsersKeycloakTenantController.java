package org.folio.uk.controller;

import org.folio.spring.controller.TenantController;
import org.folio.uk.service.UsersTenantService;
import org.springframework.web.bind.annotation.RestController;

@RestController("folioTenantController")
public class UsersKeycloakTenantController extends TenantController {

  public UsersKeycloakTenantController(UsersTenantService tenantService) {
    super(tenantService);
  }
}
