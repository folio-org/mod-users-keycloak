package org.folio.uk.integration.roles.model;

import static org.folio.common.utils.CollectionUtils.mapItems;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Data;

@Data
public class LoadableRole {

  private UUID id;
  private String name;
  private String description;
  private List<LoadablePermission> permissions = new ArrayList<>();

  @Override
  public String toString() {
    return "LoadableRole {"
      + "id=" + id
      + ", name='" + name + '\''
      + ", permissions=" + mapItems(permissions, LoadablePermission::getPermissionName)
      + '}';
  }
}
