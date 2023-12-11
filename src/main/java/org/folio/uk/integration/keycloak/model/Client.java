package org.folio.uk.integration.keycloak.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Client implements Serializable {
  @Serial
  private static final long serialVersionUID = -5450019006221767712L;

  private UUID id;
  private String clientId;
}
