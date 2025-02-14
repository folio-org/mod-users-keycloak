package org.folio.uk.integration.keycloak.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FederatedIdentity {
  @JsonProperty("identityProvider")
  private String providerAlias;
  private String userId;
  private String userName;

  @Override
  public String toString() {
    if (StringUtils.isEmpty(providerAlias)) {
      return "FederatedIdentity{"
        + ", userId='" + userId + '\''
        + ", userName='" + userName + '\''
        + '}';
    }
    return "FederatedIdentity{"
      + "providerAlias='" + providerAlias + '\''
      + ", userId='" + userId + '\''
      + ", userName='" + userName + '\''
      + '}';
  }
}
