package org.folio.uk.integration.permission;

import static java.util.Objects.isNull;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.uk.integration.permission.model.PermissionUser;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class PermissionService {

  private static final Integer ALL_RECORDS = 99999;

  private final PermissionClient client;

  public List<String> findUsersIdsWithPermissions() {
    var response = client.findByQuery("id=* NOT permissions==[]", ALL_RECORDS, null);
    if (isNull(response) || isEmpty(response.getPermissionUsers())) {
      return List.of();
    }
    return response.getPermissionUsers().stream()
      .map(PermissionUser::getUserId)
      .collect(Collectors.toList());
  }
}
