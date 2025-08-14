package org.folio.uk.service;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.uk.integration.policy.PolicyService;
import org.folio.uk.integration.roles.UserCapabilitiesClient;
import org.folio.uk.integration.roles.UserCapabilitySetClient;
import org.folio.uk.integration.roles.UserRolesClient;
import org.folio.uk.integration.roles.model.CollectionResponse;
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
    deleteIfFound(
      () -> userCapabilitySetClient.findUserCapabilitySet(userId),
      () -> userCapabilitySetClient.deleteUserCapabilitySet(userId));

    log.info("Trying to unassign capabilities from the user: userId = {}", userId);
    deleteIfFound(
      () -> userCapabilitiesClient.findUserCapabilities(userId),
      () -> userCapabilitiesClient.deleteUserCapabilities(userId));
    
    log.info("Trying to unassign user from the role: userId = {}", userId);
    deleteIfFound(
      () -> userRolesClient.findUserRoles(userId),
      () -> userRolesClient.deleteUserRoles(userId));

    log.info("Trying to delete policies related to the user: userId = {}", userId);
    policyService.removePolicyByUserId(userId);
  }

  private static void deleteIfFound(Supplier<Optional<CollectionResponse>> finder, Action deleteAction) {
    finder.get()
      .filter(collectionResponse -> collectionResponse.getTotalRecords() > 0)
      .ifPresent(unused -> deleteAction.execute());
  }

  private interface Action {
    void execute();
  }
}
