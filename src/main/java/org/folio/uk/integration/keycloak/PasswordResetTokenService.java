package org.folio.uk.integration.keycloak;

import static org.folio.uk.domain.dto.ErrorCode.LINK_INVALID;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Base64;
import java.util.HashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.spring.FolioExecutionContext;
import org.folio.uk.exception.UnprocessableEntityException;
import org.folio.uk.integration.keycloak.config.KeycloakPasswordResetClientProperties;
import org.folio.uk.integration.keycloak.model.TokenResponse;
import org.springframework.stereotype.Service;

@Service
@Log4j2
@RequiredArgsConstructor
public class PasswordResetTokenService {
  private static final String PASSWORD_RESET_ACTION_ID_CLAIM = "passwordResetActionId";

  private final FolioExecutionContext folioExecutionContext;
  private final KeycloakClient keycloakClient;
  private final ObjectMapper objectMapper;
  private final KeycloakPasswordResetClientProperties passwordResetClientProperties;
  private final RealmConfigurationProvider realmConfigurationProvider;

  public TokenResponse generateResetToken(String passwordResetActionId) {
    var tenant = folioExecutionContext.getTenantId();
    var clientId = passwordResetClientProperties.getClientId();
    var clientConfig = realmConfigurationProvider.getClientConfiguration(tenant, clientId);
    var loginRequest = new HashMap<String, String>();
    loginRequest.put("client_id", clientId);
    loginRequest.put("client_secret", clientConfig.getClientSecret());
    loginRequest.put("grant_type", "client_credentials");
    loginRequest.put(PASSWORD_RESET_ACTION_ID_CLAIM, passwordResetActionId);

    return keycloakClient.login(loginRequest, tenant);
  }

  public String parsePasswordResetActionId() {
    var authToken = folioExecutionContext.getToken();
    if (authToken == null) {
      throw new UnprocessableEntityException("Failed to find auth token in request.", LINK_INVALID);
    }

    var split = authToken.split("\\.");
    if (split.length < 2 || split.length > 3) {
      throw new UnprocessableEntityException("Invalid amount of segments in JWT token.", LINK_INVALID);
    }

    try {
      var payload = objectMapper.readTree(new String(Base64.getDecoder().decode(split[1])));
      return payload.get(PASSWORD_RESET_ACTION_ID_CLAIM).asText();
    } catch (Exception e) {
      log.warn("Failed to parse token", e);
      throw new UnprocessableEntityException("Invalid token.", LINK_INVALID);
    }
  }
}
