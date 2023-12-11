package org.folio.uk.integration.password.model;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor(staticName = "of")
public class PasswordValidationRequest {
  private String password;
  private UUID userId;
}
