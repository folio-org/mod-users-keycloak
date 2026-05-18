package org.folio.uk.integration.kafka.model;

import lombok.Builder;
import lombok.NoArgsConstructor;
import org.folio.integration.kafka.model.ResourceEvent;
import org.folio.integration.kafka.model.ResourceEventType;
import org.jspecify.annotations.Nullable;

/**
 * Concrete event class for system user events.
 */
@NoArgsConstructor
public class SystemUserEvent extends ResourceEvent<SystemUser> {

  @Builder
  public SystemUserEvent(String id, ResourceEventType type, String tenant, String resourceName,
    @Nullable SystemUser newValue, @Nullable SystemUser oldValue) {
    super(id, type, tenant, resourceName, newValue, oldValue);
  }
}
