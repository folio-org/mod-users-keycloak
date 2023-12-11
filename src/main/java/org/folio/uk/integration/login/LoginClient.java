package org.folio.uk.integration.login;

import java.util.Optional;
import org.folio.uk.integration.login.model.PasswordResetAction;
import org.folio.uk.integration.login.model.PasswordResetActionCreated;
import org.folio.uk.integration.login.model.PasswordResetRequest;
import org.folio.uk.integration.login.model.PasswordResetResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Client for password reset actions API in mod-login-keycloak module.
 */
@FeignClient(name = "authn", dismiss404 = true)
public interface LoginClient {

  /**
   * Saves given action.
   *
   * @param passwordResetAction entry to save
   * @return password create action response
   */
  @PostMapping("/password-reset-action")
  PasswordResetActionCreated savePasswordResetAction(@RequestBody PasswordResetAction passwordResetAction);

  /**
   * Retrieves password reset action with given id.
   *
   * @param passwordResetActionId password reset action id
   * @return password reset action
   */
  @GetMapping("/password-reset-action/{passwordResetActionId}")
  Optional<PasswordResetAction> getPasswordResetAction(
    @PathVariable("passwordResetActionId") String passwordResetActionId);

  /**
   * Resets password.
   *
   * @param passwordReset password reset payload
   * @return reset action response
   */
  @PostMapping("/reset-password")
  PasswordResetResponse resetPassword(@RequestBody PasswordResetRequest passwordReset);
}
