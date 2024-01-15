package org.folio.uk.integration.roles.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CollectionResponse {
  @JsonProperty("totalRecords")
  private Integer totalRecords;
}
