package org.folio.uk.integration.policy;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.uk.domain.dto.Policies;
import org.folio.uk.domain.dto.Policy;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class PolicyService {

  public static final int LIMIT = 9999;
  private final PolicyClient client;

  public void removePolicyByUserId(UUID userId) {

    Policies policyResponse = client.find(String.format("name=*%s", userId), LIMIT, 0);
    if (policyResponse.getTotalRecords() == 0) {
      log.warn("Can not delete policy cause not found policies for user: userId = {}", userId);
      return;
    }
    
    List<Policy> policies = policyResponse.getPolicies();
    policies.stream()
      .filter(policy -> policy.getUserPolicy() != null && policy.getUserPolicy().getUsers().contains(userId))
      .forEach(policy -> {
        if (policy.getUserPolicy().getUsers().size() > 1) {
          unassignPolicy(userId, policy);
        } else {
          log.warn("Delete policy: userId = {}, policyId = {}", userId, policy.getId());
          client.delete(policy.getId());
        }
      });
  }

  private void unassignPolicy(UUID userId, Policy policy) {
    policy.getUserPolicy().getUsers()
      .remove(userId);
    log.warn("Unassign policy for user: policyId = {}, userId = {}", policy.getId(), userId);
    client.update(policy.getId(), policy);
  }
}
