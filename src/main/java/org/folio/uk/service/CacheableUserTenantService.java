package org.folio.uk.service;

import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.folio.uk.integration.users.UserTenantsClient;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CacheableUserTenantService {

  private final UserTenantsClient userTenantsClient;

  @Cacheable(value = "userTenantCache", key = "#userId")
  public Optional<org.folio.uk.domain.dto.UserTenant> getUserTenant(UUID userId) {
    return userTenantsClient.lookupByUserId(userId, 1).getUserTenants().stream().findFirst();
  }
}
