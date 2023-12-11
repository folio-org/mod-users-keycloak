
package org.folio.uk.integration.permission.model;

import java.util.List;
import lombok.Data;

@Data
public class PermissionsUsersResponse {

  private List<PermissionUser> permissionUsers;
}
