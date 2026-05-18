package org.folio.uk.integration.kafka.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

/**
 * Represents the system user payload carried inside a {@link SystemUserEvent}.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor(staticName = "of")
public class SystemUser {
  private String name;
  private String type;
  @ToString.Exclude
  private Set<String> permissions;
}
