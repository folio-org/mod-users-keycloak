package org.folio.uk.integration.login.model;

import lombok.Data;

/**
 * Response entity to create a new password change action.
 */
@Data
public class PasswordResetActionCreated {
  /**
   * Check if the user has an existing password or credential.
   */
  private Boolean passwordExists;
}
