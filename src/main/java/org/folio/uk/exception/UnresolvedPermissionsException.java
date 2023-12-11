package org.folio.uk.exception;

import java.util.Collection;
import java.util.UUID;
import lombok.Getter;
import org.folio.uk.domain.dto.ErrorCode;

@Getter
public class UnresolvedPermissionsException extends RuntimeException {

  private static final String MESSAGE_TEMPLATE =
    "Unable to assign user capabilities, permissions are not resolved: userId = %s, permissions = %s";

  private final ErrorCode errorCode;

  public UnresolvedPermissionsException(UUID userId, Collection<String> permissions) {
    super(String.format(MESSAGE_TEMPLATE, userId, permissions));
    this.errorCode = ErrorCode.NOT_FOUND_ERROR;
  }
}
