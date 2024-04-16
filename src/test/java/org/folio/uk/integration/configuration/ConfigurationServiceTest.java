package org.folio.uk.integration.configuration;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.folio.test.types.UnitTest;
import org.folio.uk.domain.dto.ErrorCode;
import org.folio.uk.exception.UnprocessableEntityException;
import org.folio.uk.exception.UnprocessableEntityMessage;
import org.folio.uk.integration.configuration.model.Config;
import org.folio.uk.integration.configuration.model.Configurations;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class ConfigurationServiceTest {

  private static final String MODULE_NAME = "TESTMODULE";

  @InjectMocks private ConfigurationService configurationService;
  @Mock private ConfigurationClient configurationClient;

  @Test
  void getAllModuleConfigsValidated_positive() {
    var config = config("TEST_CODE", "test-value");
    when(configurationClient.lookupConfigByModuleName(MODULE_NAME)).thenReturn(configurations(config));

    var result = configurationService.getAllModuleConfigsValidated(MODULE_NAME, emptyList());

    assertThat(result).isEqualTo(Map.of("TEST_CODE", "test-value"));
  }

  @Test
  void getAllModuleConfigsValidated_positive_codeIsNull() {
    var config = config(null, "test-value");
    when(configurationClient.lookupConfigByModuleName(MODULE_NAME)).thenReturn(configurations(config));

    var result = configurationService.getAllModuleConfigsValidated(MODULE_NAME, emptyList());

    assertThat(result).isEmpty();
  }

  @Test
  void getAllModuleConfigsValidated_positive_valueIsNull() {
    var config = config("TEST_CODE", null);
    when(configurationClient.lookupConfigByModuleName(MODULE_NAME)).thenReturn(configurations(config));

    var result = configurationService.getAllModuleConfigsValidated(MODULE_NAME, emptyList());

    assertThat(result).isEmpty();
  }

  @Test
  void getAllModuleConfigsValidated_negative_requiredCodeIsMissing() {
    var config = config("TEST_CODE1", null);
    when(configurationClient.lookupConfigByModuleName(MODULE_NAME)).thenReturn(configurations(config));

    var requiredCodes = List.of("TEST_CODE");
    assertThatThrownBy(() -> configurationService.getAllModuleConfigsValidated(MODULE_NAME, requiredCodes))
      .isInstanceOf(UnprocessableEntityException.class)
      .extracting(error -> ((UnprocessableEntityException) error).getErrors())
      .satisfies(errors -> assertThat(errors).containsExactly(
        new UnprocessableEntityMessage(ErrorCode.NOT_FOUND_ERROR,
          format("Configuration for module %s does not contain all required codes:%s", MODULE_NAME, requiredCodes))
      ));
  }

  @Test
  void getAllModuleConfigsValidated_negative_configIsDisabled() {
    var config = config("TEST_CODE", null).withEnabled(false);
    when(configurationClient.lookupConfigByModuleName(MODULE_NAME)).thenReturn(configurations(config));

    var requiredCodes = List.of("TEST_CODE");
    assertThatThrownBy(() -> configurationService.getAllModuleConfigsValidated(MODULE_NAME, requiredCodes))
      .isInstanceOf(UnprocessableEntityException.class)
      .extracting(error -> ((UnprocessableEntityException) error).getErrors())
      .satisfies(errors -> assertThat(errors).containsExactly(
        new UnprocessableEntityMessage(ErrorCode.NOT_FOUND_ERROR,
          format("Configuration for module %s does not contain all required codes:%s", MODULE_NAME, requiredCodes))
      ));
  }

  private static Configurations configurations(Config... configs) {
    return new Configurations().withConfigs(Arrays.asList(configs)).withTotalRecords(configs.length);
  }

  private static Config config(String code, String value) {
    return new Config()
      .withCode(code)
      .withEnabled(true)
      .withId(UUID.randomUUID().toString())
      .withConfigName("validation_rules")
      .withValue(value);
  }
}
