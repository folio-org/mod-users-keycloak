package org.folio.uk.integration.configuration;

import org.folio.uk.integration.configuration.model.Configurations;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * Client for mod-configuration module.
 */
@HttpExchange(url = "configurations")
public interface ConfigurationClient {

  /**
   * Searches for configuration by module name.
   *
   * @param moduleName module name
   * @return map containing found configuration
   */
  default Configurations lookupConfigByModuleName(String moduleName) {
    return queryEntries("module==" + moduleName, null);
  }

  /**
   * Searches for configuration by module name and query.
   *
   * @param moduleName module name
   * @param query      configuration query
   * @return map containing found configuration
   */
  default Configurations lookupConfigByModuleNameAndQuery(String moduleName, String query, int limit) {
    return queryEntries("module==" + moduleName + " AND " + query, limit);
  }

  @GetExchange("entries")
  Configurations queryEntries(@RequestParam("query") String query,
    @RequestParam(value = "limit", required = false) Integer limit);
}
