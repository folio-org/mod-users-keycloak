package org.folio.uk.utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections.MapUtils;
import org.folio.spring.DefaultFolioExecutionContext;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.integration.XOkapiHeaders;

@Log4j2
@UtilityClass
public class TenantContextUtils {

  public static FolioExecutionContext prepareContextForTenant(String tenantId, FolioModuleMetadata folioModuleMetadata,
                                                              FolioExecutionContext context) {
    if (MapUtils.isNotEmpty(context.getOkapiHeaders())) {
      var headersCopy = new HashMap<String, Collection<String>>();
      context.getAllHeaders().forEach((key, value) -> headersCopy.put(key, List.copyOf(value)));
      headersCopy.put(XOkapiHeaders.TENANT, List.of(tenantId));
      log.info("FOLIO context initialized with tenant {}", tenantId);
      return new DefaultFolioExecutionContext(folioModuleMetadata, headersCopy);
    }
    throw new IllegalStateException("Okapi headers not provided");
  }
}
