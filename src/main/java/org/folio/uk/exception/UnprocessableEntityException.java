package org.folio.uk.exception;

import java.util.Collections;
import java.util.List;
import org.folio.uk.domain.dto.ErrorCode;

/**
 * Exception indicating that request can not be performed due to invalid request entity.
 */
public class UnprocessableEntityException extends RuntimeException {

  private final List<UnprocessableEntityMessage> errors;

  public UnprocessableEntityException(List<UnprocessableEntityMessage> errors) {
    this.errors = errors;
  }

  public UnprocessableEntityException(String message, ErrorCode errorCode) {
    this.errors = Collections.singletonList(new UnprocessableEntityMessage(errorCode, message));
  }

  public UnprocessableEntityException(String message, String errorCode) {
    this.errors = Collections.singletonList(new UnprocessableEntityMessage(errorCode, message));
  }

  public List<UnprocessableEntityMessage> getErrors() {
    return errors;
  }
}
