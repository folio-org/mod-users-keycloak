package org.folio.uk.integration.inventory.model;

import java.util.List;
import lombok.Data;
import org.folio.uk.domain.dto.ServicePointUser;

@Data
public class ServicePointUserCollection {
  private List<ServicePointUser> servicePointsUsers;
  private int totalRecords;
}
