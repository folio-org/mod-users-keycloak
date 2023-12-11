package org.folio.uk.integration.keycloak.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScopePermission {
  private String id;
  private String name;
  private List<String> scopes;
  private List<String> policies;
  private List<String> resources;
  private final String type = "scope";
}
