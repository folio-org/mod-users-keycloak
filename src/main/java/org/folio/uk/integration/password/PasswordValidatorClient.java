package org.folio.uk.integration.password;

import org.folio.uk.integration.password.model.PasswordValidationRequest;
import org.folio.uk.integration.password.model.PasswordValidationResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "password")
public interface PasswordValidatorClient {

  @PostMapping("/validate")
  PasswordValidationResult validateNewPassword(@RequestBody PasswordValidationRequest payload);
}
