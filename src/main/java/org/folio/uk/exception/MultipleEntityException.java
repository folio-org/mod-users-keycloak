package org.folio.uk.exception;

/**
 * Exception indicating that request can not be performed due to multiple entities for same Identifier.
 */
public class MultipleEntityException extends UnprocessableEntityException {

  public MultipleEntityException(String message, String errorCode) {
    super(message, errorCode);
  }
}
