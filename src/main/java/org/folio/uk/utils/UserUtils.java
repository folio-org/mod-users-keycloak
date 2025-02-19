package org.folio.uk.utils;

import java.util.Optional;
import lombok.experimental.UtilityClass;

@UtilityClass
public class UserUtils {

  public static final String ORIGINAL_TENANT_ID_CUSTOM_FIELD = "originaltenantid";

  public static Optional<String> getOriginalTenantIdOptional(org.folio.uk.domain.dto.User user) {
    return user.getCustomFields().entrySet().stream()
      .filter(entry -> entry.getKey().equalsIgnoreCase(ORIGINAL_TENANT_ID_CUSTOM_FIELD))
      .map(entry -> (String) entry.getValue())
      .findFirst();
  }
}
