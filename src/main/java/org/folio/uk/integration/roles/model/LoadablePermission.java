package org.folio.uk.integration.roles.model;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor(staticName = "of")
public class LoadablePermission {

  @NonNull
  private final String permissionName;
}
