package org.folio.uk.integration.kafka.model;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.folio.integration.kafka.model.ResourceEvent;
import org.folio.integration.kafka.model.ResourceEventType;
import org.folio.uk.domain.dto.User;
import org.jspecify.annotations.Nullable;

/**
 * Kafka event envelope for FOLIO user domain events, extending {@link ResourceEvent} typed to {@link User}.
 *
 * <p>Used by the user topic listener to react to user create, update, and delete operations.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class UserEvent extends ResourceEvent<User> {

  private static final String RESOURCE_NAME = "User";

  private long timestamp;

  /**
   * Creates a user event and sets the resource name to {@value RESOURCE_NAME}.
   */
  public UserEvent() {
    setResourceName(RESOURCE_NAME);
  }

  /**
   * Creates a fully initialised user event.
   *
   * @param id        event identifier
   * @param type      event type (CREATE, UPDATE, DELETE)
   * @param tenant    tenant the event belongs to
   * @param newValue  new user state, or {@code null} for DELETE events
   * @param oldValue  previous user state, or {@code null} for CREATE events
   * @param timestamp epoch-millis timestamp of when the event was produced
   */
  @Builder
  public UserEvent(String id, ResourceEventType type, String tenant, @Nullable User newValue,
    @Nullable User oldValue, long timestamp) {
    super(id, type, tenant, RESOURCE_NAME, newValue, oldValue);
    this.timestamp = timestamp;
  }
}

