package org.folio.uk.integration.keycloak.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdentityProvider {
  @JsonProperty("identityProvider")
  private String providerAlias;
  private String userId;
  private String userName;
}
