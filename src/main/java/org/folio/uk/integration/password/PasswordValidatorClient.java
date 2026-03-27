package org.folio.uk.integration.password;

import org.folio.uk.integration.password.model.PasswordValidationRequest;
import org.folio.uk.integration.password.model.PasswordValidationResult;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange(url = "password")
public interface PasswordValidatorClient {

  @PostExchange("/validate")
  PasswordValidationResult validateNewPassword(@RequestBody PasswordValidationRequest payload);
}
