package org.folio.uk.integration.password.model;

import java.util.List;
import lombok.Data;

@Data
public class PasswordValidationResult {
  private String result;
  private List<String> messages;
}
