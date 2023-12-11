package org.folio.uk.integration.configuration;

import org.folio.uk.integration.configuration.model.Configurations;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Client for mod-configuration module.
 */
@FeignClient(name = "configurations")
public interface ConfigurationClient {

  /**
   * Searches for configuration by module name.
   *
   * @param moduleName module name
   * @return map containing found configuration
   */
  @GetMapping("${config.client.path:/entries}?query=module=={moduleName}")
  Configurations lookupConfigByModuleName(@PathVariable("moduleName") String moduleName);

  /**
   * Searches for configuration by module name and query.
   *
   * @param moduleName module name
   * @param query      configuration query
   * @return map containing found configuration
   */
  @GetMapping("${config.client.path:/entries}?query=module=={moduleName} AND {query}")
  Configurations lookupConfigByModuleNameAndQuery(@PathVariable("moduleName") String moduleName,
    @PathVariable("query") String query, @RequestParam("limit") int limit);
}
