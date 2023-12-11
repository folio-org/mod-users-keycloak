package org.folio.uk.integration.keycloak.model;

import java.io.Serial;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Credential implements Serializable {

  @Serial
  private static final long serialVersionUID = 2570049338890249201L;

  private String type;
  private String value;
  private Boolean temporary;
}
