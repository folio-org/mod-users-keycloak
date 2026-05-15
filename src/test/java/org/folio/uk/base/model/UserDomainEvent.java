package org.folio.uk.base.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.folio.uk.domain.dto.User;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDomainEvent  {

  private UUID id;
  private DomainEventType type;
  private String tenant;
  private long timestamp;
  private UserEventData data;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor(staticName = "of")
  public static class UserEventData {

    @JsonProperty("old")
    private User oldValue;
    @JsonProperty("new")
    private User newValue;
  }
}
