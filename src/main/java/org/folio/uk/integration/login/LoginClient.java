package org.folio.uk.integration.login;

import java.util.Optional;
import org.folio.uk.integration.login.model.PasswordResetAction;
import org.folio.uk.integration.login.model.PasswordResetActionCreated;
import org.folio.uk.integration.login.model.PasswordResetRequest;
import org.folio.uk.integration.login.model.PasswordResetResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

/**
 * Client for password reset actions API in mod-login-keycloak module.
 */
@HttpExchange(url = "authn")
public interface LoginClient {

  /**
   * Saves given action.
   *
   * @param passwordResetAction entry to save
   * @return password create action response
   */
  @PostExchange("/password-reset-action")
  PasswordResetActionCreated savePasswordResetAction(@RequestBody PasswordResetAction passwordResetAction);

  /**
   * Retrieves password reset action with given id.
   *
   * @param passwordResetActionId password reset action id
   * @return password reset action
   */
  @GetExchange("/password-reset-action/{passwordResetActionId}")
  Optional<PasswordResetAction> getPasswordResetAction(
    @PathVariable("passwordResetActionId") String passwordResetActionId);

  /**
   * Resets password.
   *
   * @param passwordReset password reset payload
   * @return reset action response
   */
  @PostExchange("/reset-password")
  PasswordResetResponse resetPassword(@RequestBody PasswordResetRequest passwordReset);
}
