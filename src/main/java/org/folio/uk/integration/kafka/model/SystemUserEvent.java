package org.folio.uk.integration.kafka.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor(staticName = "of")
public class SystemUserEvent {
  private String name;
  private String type;
  @ToString.Exclude
  private Set<String> permissions;
}
