package org.folio.uk.integration.keycloak.model;

import static java.util.Collections.emptyList;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class KeycloakUser implements Serializable {

  public static final String USER_ID_ATTR = "user_id";
  public static final String USER_TENANT_ATTR = "tenant_name";
  public static final String USER_EXTERNAL_SYSTEM_ID_ATTR = "external_system_id";
  public static final String USER_BARCODE_ATTR = "barcode";

  @Serial
  private static final long serialVersionUID = 3279142822765797425L;

  private String id;
  @JsonProperty("username")
  private String userName;
  private String firstName;
  private String lastName;
  private String email;
  private Boolean emailVerified;
  private Long createdTimestamp;
  private Boolean enabled;
  private List<Credential> credentials;
  private Map<String, List<String>> attributes = new HashMap<>();

  @JsonIgnore
  public void setUserIdAttr(UUID userId) {
    if (userId == null) {
      attributes.remove(USER_ID_ATTR);
    } else {
      attributes.put(USER_ID_ATTR, List.of(userId.toString()));
    }
  }

  @JsonIgnore
  public void setUserTenantAttr(List<String> userTenants) {
    if (isEmpty(userTenants)) {
      attributes.remove(USER_TENANT_ATTR);
    } else {
      attributes.put(USER_TENANT_ATTR, userTenants);
    }
  }

  @JsonIgnore
  public void setUserExternalSystemIdAttr(String externalSystemId) {
    if (externalSystemId == null) {
      attributes.remove(USER_EXTERNAL_SYSTEM_ID_ATTR);
    } else {
      attributes.put(USER_EXTERNAL_SYSTEM_ID_ATTR, List.of(externalSystemId));
    }
  }

  @JsonIgnore
  public void setUserBarcodeAttr(String barcode) {
    if (barcode == null) {
      attributes.remove(USER_BARCODE_ATTR);
    } else {
      attributes.put(USER_BARCODE_ATTR, List.of(barcode));
    }
  }

  @JsonIgnore
  public Optional<String> getUserIdAttr() {
    var values = attributes.getOrDefault(USER_ID_ATTR, emptyList());
    return values.size() == 1 ? Optional.of(values.get(0)) : Optional.empty();
  }
}
