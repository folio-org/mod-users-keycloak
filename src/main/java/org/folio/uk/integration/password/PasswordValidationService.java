package org.folio.uk.integration.password;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.folio.uk.domain.dto.Error;
import org.folio.uk.domain.dto.Errors;
import org.folio.uk.exception.UnprocessableEntityException;
import org.folio.uk.exception.UnprocessableEntityMessage;
import org.folio.uk.integration.password.model.PasswordValidationRequest;
import org.springframework.stereotype.Service;

@Service
@Log4j2
@RequiredArgsConstructor
public class PasswordValidationService {
  private final PasswordValidatorClient passwordValidatorClient;

  public void validatePassword(UUID userId, String newPassword) {
    var validationResult =
      passwordValidatorClient.validateNewPassword(PasswordValidationRequest.of(newPassword, userId));
    var messages = validationResult.getMessages();
    var errors = mapErrors(messages);

    if (errors.getTotalRecords() != 0) {
      throw new UnprocessableEntityException(errors.getErrors().stream()
        .map(error -> new UnprocessableEntityMessage(error.getCode(), error.getMessage()))
        .collect(Collectors.toList()));
    }
  }

  private static Errors mapErrors(List<String> messages) {
    var errors = new Errors();
    errors.setTotalRecords(0);
    if (CollectionUtils.isNotEmpty(messages)) {
      errors.setTotalRecords(messages.size());
      var errorList = new ArrayList<Error>();
      for (var message : messages) {
        var error = new Error();
        error.setMessage(message);
        error.setCode(message);
        errorList.add(error);
      }
      errors.setErrors(errorList);
    }
    return errors;
  }
}
