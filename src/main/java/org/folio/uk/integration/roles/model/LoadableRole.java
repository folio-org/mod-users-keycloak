package org.folio.uk.integration.roles.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Data;

@Data
public class LoadableRole {

  private List<LoadablePermission> permissions = new ArrayList<>();
  private UUID id;
  private String name;
  private String description;
}
