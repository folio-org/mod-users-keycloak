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

  private final PolicyClient client;

  public void removePolicyByUsername(String username, UUID userId) {

    Policies policyResponse = client.find(String.format("name=*%s", username), 9999, 0);
    if (policyResponse.getTotalRecords() == 0) {
      log.debug("Can not delete policy cause not found policies for user: username = {}", username);
      return;
    }
    
    List<Policy> policies = policyResponse.getPolicies();
    policies.stream()
      .filter(policy -> policy.getUserPolicy() != null && policy.getUserPolicy().getUsers().contains(userId))
      .forEach(policy -> {
        if (policy.getUserPolicy().getUsers().size() > 1) {
          unassignPolicy(userId, policy);
        } else {
          client.delete(policy.getId());
        }
      });
  }

  private void unassignPolicy(UUID userId, Policy policy) {
    policy.getUserPolicy().getUsers()
      .remove(userId);
    client.update(policy.getId(), policy);
  }
}
