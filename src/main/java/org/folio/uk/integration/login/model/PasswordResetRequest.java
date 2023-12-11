package org.folio.uk.integration.login.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor(staticName = "of")
public class PasswordResetRequest {
  private String passwordResetActionId;
  private String newPassword;
}
