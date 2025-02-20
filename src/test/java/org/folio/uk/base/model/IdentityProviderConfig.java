package org.folio.uk.base.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdentityProviderConfig {
  private String tokenUrl;
  private String acceptsPromptNoneForwardFromClient;
  private String jwksUrl;
  @JsonProperty("isAccessTokenJWT")
  private String isAccessTokenJwt;
  private String filteredByClaim;
  private String backchannelSupported;
  private String caseSensitiveOriginalUsername;
  private String issuer;
  private String loginHint;
  private String clientAuthMethod;
  private String syncMode;
  private String clientSecret;
  private String allowedClockSkew;
  private String userInfoUrl;
  private String validateSignature;
  private String clientId;
  private String uiLocales;
  private String disableNonce;
  private String useJwksUrl;
  private String sendClientIdOnLogout;
  private String metadataDescriptorUrl;
  private String pkceEnabled;
  private String authorizationUrl;
  private String disableUserInfo;
  private String logoutUrl;
  private String sendIdTokenOnLogout;
  private String passMaxAge;
}
