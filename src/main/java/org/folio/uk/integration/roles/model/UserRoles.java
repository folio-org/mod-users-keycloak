package org.folio.uk.integration.roles.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Data;

@Data
public class UserRoles {

  private UUID userId;
  private List<UUID> roleIds = new ArrayList<>();
}
