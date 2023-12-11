package org.folio.uk.integration.kafka.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResourceEvent {

  /**
   * Resource identifier.
   */
  private String id;

  /**
   * Event type.
   */
  private ResourceEventType type;

  /**
   * Tenant identifier (name).
   */
  private String tenant;

  /**
   * Name of resource.
   */
  private String resourceName;

  /**
   * New value (if resource is created or updated).
   */
  @JsonProperty("new")
  private Object newValue;

  /**
   * Previous version value (if resource was updated or deleted).
   */
  @JsonProperty("old")
  private Object oldValue;
}
