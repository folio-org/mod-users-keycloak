package org.folio.uk.integration.roles.model;

import java.util.List;
import java.util.UUID;
import lombok.Data;

@Data
public class UserRolesResponse {

  private List<UserRole> userRoles;
  private Integer totalRecords;

  record UserRole(UUID userId, UUID roleId) {
  }
}
