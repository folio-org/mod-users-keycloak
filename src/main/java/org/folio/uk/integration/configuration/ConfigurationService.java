package org.folio.uk.integration.configuration;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toMap;
import static org.folio.common.utils.CollectionUtils.toStream;
import static org.folio.uk.domain.dto.ErrorCode.NOT_FOUND_ERROR;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.folio.uk.exception.UnprocessableEntityException;
import org.folio.uk.integration.configuration.model.Config;
import org.folio.uk.integration.configuration.model.Configurations;
import org.springframework.stereotype.Service;

@Service
@Log4j2
@RequiredArgsConstructor
public class ConfigurationService {
  private final ConfigurationClient configurationClient;

  public Map<String, String> queryModuleConfigsByCodes(String moduleName, Collection<String> codes) {
    var codesQuery = new StringBuilder("(")
      .append(codes.stream()
        .map(code -> new StringBuilder("code==\"").append(code).append("\"").toString())
        .collect(Collectors.joining(" or ")))
      .append(")");

    var configurations =
      configurationClient.lookupConfigByModuleNameAndQuery(moduleName, codesQuery.toString(), codes.size());

    return convertConfigsToMap(configurations);
  }

  public Map<String, String> getAllModuleConfigsValidated(String moduleName, Collection<String> expectedCodes) {
    var configurations = configurationClient.lookupConfigByModuleName(moduleName);
    if (!containsCodes(configurations, expectedCodes)) {
      var message = String.format("Configuration for module %s does not contain all required codes:%s",
        moduleName, expectedCodes);
      throw new UnprocessableEntityException(message, NOT_FOUND_ERROR);
    }
    return convertConfigsToMap(configurations);
  }

  private static Map<String, String> convertConfigsToMap(Configurations configurations) {
    return toStream(configurations.getConfigs())
      .filter(config -> config.getCode() != null && config.getValue() != null)
      .collect(toMap(Config::getCode, Config::getValue, (o1, o2) -> o2));
  }

  private boolean containsCodes(Configurations configurations, Collection<String> requiredCodes) {
    return configurations.getConfigs().stream()
      .filter(Config::getEnabled)
      .map(Config::getCode)
      .collect(Collectors.toList())
      .containsAll(requiredCodes);
  }
}
