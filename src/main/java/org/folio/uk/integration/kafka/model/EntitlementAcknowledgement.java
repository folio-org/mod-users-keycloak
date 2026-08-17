package org.folio.uk.integration.kafka.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * Reports the result of processing an entitlement-related Kafka event.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
public class EntitlementAcknowledgement {

  public static final String SYSTEM_USER_TYPE = "system-user";
  public static final String SUCCESS_STATUS = "success";
  public static final String ERROR_STATUS = "error";

  private String tenant;
  private String type;
  private String moduleId;
  private String status;
  private String details;
}
