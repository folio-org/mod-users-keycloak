package org.folio.uk.exception;

import java.io.Serializable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.folio.uk.domain.dto.ErrorCode;

/**
 * Model describing reason of {@link UnprocessableEntityException}.
 */
@Getter
@EqualsAndHashCode
public class UnprocessableEntityMessage implements Serializable {

  private String code;
  private String message;

  public UnprocessableEntityMessage(ErrorCode code, String message) {
    this.code = code.getValue();
    this.message = message;
  }

  public UnprocessableEntityMessage(String code, String message) {
    this.code = code;
    this.message = message;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
      .append("code", code)
      .append("message", message)
      .toString();
  }
}
