package org.folio.uk.integration.login;

import static org.folio.uk.domain.dto.ErrorCode.LINK_USED;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.uk.exception.UnprocessableEntityException;
import org.folio.uk.integration.login.model.PasswordResetAction;
import org.folio.uk.integration.login.model.PasswordResetActionCreated;
import org.folio.uk.integration.login.model.PasswordResetRequest;
import org.folio.uk.integration.login.model.PasswordResetResponse;
import org.folio.uk.integration.password.PasswordValidationService;
import org.springframework.stereotype.Service;

@Service
@Log4j2
@RequiredArgsConstructor
public class LoginService {
  private final PasswordValidationService passwordValidationService;
  private final LoginClient loginClient;

  public PasswordResetResponse resetPassword(String newPassword, UUID userId, String passwordResetActionId) {
    passwordValidationService.validatePassword(userId, newPassword);
    return loginClient.resetPassword(PasswordResetRequest.of(passwordResetActionId, newPassword));
  }

  public PasswordResetActionCreated createPasswordResetAction(UUID userId, long expirationTime,
    String passwordResetActionId) {
    var actionToCreate = new PasswordResetAction()
      .withId(passwordResetActionId)
      .withUserId(userId)
      .withExpirationTime(new Date(
        Instant.now()
          .plusMillis(expirationTime)
          .toEpochMilli()));

    return loginClient.savePasswordResetAction(actionToCreate);
  }

  public PasswordResetAction getPasswordResetAction(String passwordResetActionId) {
    return loginClient.getPasswordResetAction(passwordResetActionId).orElseThrow(() -> {
      var message = String.format("PasswordResetAction with id = %s is not found", passwordResetActionId);
      return new UnprocessableEntityException(message, LINK_USED);
    });
  }
}
