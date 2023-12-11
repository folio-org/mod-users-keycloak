package org.folio.uk.service;

import static java.util.stream.Collectors.toCollection;
import static org.folio.common.utils.CollectionUtils.mapItems;
import static org.folio.common.utils.PaginationUtils.loadInBatches;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.ListUtils;
import org.folio.common.utils.CqlQuery;
import org.folio.uk.domain.dto.Capability;
import org.folio.uk.domain.dto.ErrorResponse;
import org.folio.uk.domain.dto.User;
import org.folio.uk.domain.dto.UserCapabilitiesRequest;
import org.folio.uk.exception.UnresolvedPermissionsException;
import org.folio.uk.integration.roles.CapabilitiesClient;
import org.folio.uk.integration.roles.UserCapabilitiesClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.RetryContext;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class CapabilitiesService {

  private static final String CAPABILITIES_UP_TO_DATE_ERROR_MSG =
    "Nothing to update, user-capability relations are not changed";
  private static final int CAPABILITY_BATCH_SIZE = 50;
  private static final String PERMISSION_QUERY_FIELD = "permission";

  private final CapabilitiesClient capabilitiesClient;
  private final UserCapabilitiesClient userCapabilitiesClient;
  private final ObjectMapper objectMapper;
  @Qualifier("capabilityRetryTemplate") private final RetryTemplate retryTemplate;

  public void assignCapabilitiesByPermissions(User user, Set<String> permissions) {
    log.info("Assigning capabilities to user: userId = {}", user.getId());
    var capabilities = findCapabilities(user, permissions);
    var ids = mapItems(capabilities, Capability::getId);
    assignCapabilities(user, ids);
  }

  private void assignCapabilities(User user, List<UUID> capabilityIds) {
    var userId = user.getId();
    try {
      var request = new UserCapabilitiesRequest().userId(userId).capabilityIds(capabilityIds);
      userCapabilitiesClient.assignUserCapabilities(userId, request);
      log.info("User capabilities are successfully assigned: userId = {}", userId);
    } catch (FeignException e) {
      if (isNothingToUpdateError(e)) {
        log.info("User capabilities are up to date: userId = {}", userId);
        return;
      }
      throw e;
    }
  }

  private List<Capability> findCapabilities(User user, Set<String> permissions) {
    log.info("Loading capabilities by permissions: userId = {}, number of permissions = {}",
      user.getId(), permissions.size());
    var resolvedCapabilities = findCapabilitiesByPermissions(permissions);

    var unresolvedPermissions = getUnresolvedPermissions(permissions, resolvedCapabilities);
    if (unresolvedPermissions.isEmpty()) {
      return resolvedCapabilities;
    }

    return ListUtils.union(resolvedCapabilities, findUnresolvedCapabilities(user, unresolvedPermissions));
  }

  private List<Capability> findUnresolvedCapabilities(User user, Set<String> permissions) {
    var userId = user.getId();
    log.info("Reloading capabilities by unresolved permissions: userId = {}, number of permissions = {}",
      userId, permissions.size());
    return retryTemplate.execute(ctx -> {
        ctx.setAttribute(RetryContext.NAME, "findUnresolvedCapabilities");

        var foundCapabilities = findCapabilitiesByPermissions(permissions);
        var unresolvedPermissions = getUnresolvedPermissions(permissions, foundCapabilities);
        if (!unresolvedPermissions.isEmpty()) {
          throw new UnresolvedPermissionsException(userId, unresolvedPermissions);
        }
        return foundCapabilities;
      }
    );
  }

  private static Set<String> getUnresolvedPermissions(Set<String> permissions, List<Capability> capabilities) {
    var capabilityPermissions = capabilities.stream()
      .map(Capability::getPermission)
      .collect(Collectors.toSet());

    return permissions.stream().filter(perm -> !capabilityPermissions.contains(perm))
      .collect(toCollection(LinkedHashSet::new));
  }

  /**
   * Queries capabilities in batches by permissions.
   *
   * @param permissions permissions
   * @return List of capabilities
   */
  private List<Capability> findCapabilitiesByPermissions(Set<String> permissions) {
    return loadInBatches(new ArrayList<>(permissions),
      permissionsBatch -> queryCapabilities(permissionsBatch), CAPABILITY_BATCH_SIZE
    );
  }

  private List<Capability> queryCapabilities(List<String> permissions) {
    var query = CqlQuery.exactMatchAny(PERMISSION_QUERY_FIELD, permissions).toString();
    return capabilitiesClient.queryCapabilities(query, CAPABILITY_BATCH_SIZE).getCapabilities();
  }

  private boolean isNothingToUpdateError(FeignException feignException) {
    var content = feignException.contentUTF8();
    try {
      var response = objectMapper.readValue(content, ErrorResponse.class);
      if (response.getTotalRecords() == 1) {
        var error = response.getErrors().get(0);
        return CAPABILITIES_UP_TO_DATE_ERROR_MSG.equals(error.getMessage());
      }
    } catch (Exception e) {
      log.warn("Unable to parse error: '{}'", content, e);
    }
    return false;
  }
}
