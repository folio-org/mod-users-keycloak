package org.folio.uk.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.uk.integration.policy.PolicyService;
import org.folio.uk.integration.roles.UserCapabilitiesClient;
import org.folio.uk.integration.roles.UserCapabilitySetClient;
import org.folio.uk.integration.roles.UserRolesClient;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class CapabilitiesService {

  private final UserCapabilitiesClient userCapabilitiesClient;
  private final UserCapabilitySetClient userCapabilitySetClient;
  private final UserRolesClient userRolesClient;
  private final PolicyService policyService;

  public void unassignAll(UUID userId) {
    log.info("Trying to unassign capability sets from the user: userId = {}", userId);
    userCapabilitySetClient.findUserCapabilitySet(userId)
      .ifPresent(userCapabilitySet -> {
        if (userCapabilitySet.getTotalRecords() > 0) {
          userCapabilitySetClient.deleteUserCapabilitySet(userId);
        }
      });

    log.info("Trying to unassign capabilities from the user: userId = {}", userId);
    userCapabilitiesClient.findUserCapabilities(userId)
      .ifPresent(userCapabilities -> {
        if (userCapabilities.getTotalRecords() > 0) {
          userCapabilitiesClient.deleteUserCapabilities(userId);
        }
      });

    log.info("Trying to unassign user from the role: userId = {}", userId);
    userRolesClient.findUserRoles(userId)
      .ifPresent(roles -> {
        if (roles.getTotalRecords() > 0) {
          userRolesClient.deleteUserRoles(userId);
        }
      });

    log.info("Trying to delete policies related to the user: userId = {}", userId);
    policyService.removePolicyByUserId(userId);
  }
}
