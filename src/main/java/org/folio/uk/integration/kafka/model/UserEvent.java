package org.folio.uk.integration.kafka.model;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.folio.integration.kafka.model.ResourceEvent;
import org.folio.integration.kafka.model.ResourceEventType;
import org.folio.uk.domain.dto.User;
import org.jspecify.annotations.Nullable;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class UserEvent extends ResourceEvent<User> {

  private static final String RESOURCE_NAME = "User";

  private long timestamp;

  public UserEvent() {
    setResourceName(RESOURCE_NAME);
  }

  @Builder
  public UserEvent(String id, ResourceEventType type, String tenant, @Nullable User newValue,
    @Nullable User oldValue, long timestamp) {
    super(id, type, tenant, RESOURCE_NAME, newValue, oldValue);
    this.timestamp = timestamp;
  }
}

