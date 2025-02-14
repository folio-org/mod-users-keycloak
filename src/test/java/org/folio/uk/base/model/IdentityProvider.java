package org.folio.uk.base.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdentityProvider {
  private String alias;
  private String displayName;
  private String internalId;
  private String providerId;
  private boolean enabled;
  private String updateProfileFirstLoginMode;
  private boolean trustEmail;
  private boolean storeToken;
  private boolean addReadTokenRoleOnCreate;
  private boolean authenticateByDefault;
  private boolean linkOnly;
  private boolean hideOnLogin;
  private IdentityProviderConfig config;
}
