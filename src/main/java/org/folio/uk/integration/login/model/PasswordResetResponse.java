package org.folio.uk.integration.login.model;

import lombok.Data;

/**
 * Response entity to reset the password.
 */
@Data
public class PasswordResetResponse {
  /**
   * Indicates the presence of a new password for the user.
   */
  private Boolean isNewPassword;
}
