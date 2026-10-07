package org.folio.uk.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

@UnitTest
class SystemUserConfigurationPropertiesTest {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
    .withInitializer(new ConfigDataApplicationContextInitializer())
    .withUserConfiguration(TestConfiguration.class);

  @Test
  void retryAttempts_positive_boundFromEnvironmentVariable() {
    contextRunner
      .withSystemProperties("SYSTEM_USER_RETRY_COUNT=60")
      .run(context -> assertThat(context.getBean(SystemUserConfigurationProperties.class).getRetryAttempts())
        .isEqualTo(60));
  }

  @Test
  void retryDelay_positive_boundFromEnvironmentVariable() {
    contextRunner
      .withSystemProperties("SYSTEM_USER_RETRY_DELAY=1000")
      .run(context -> assertThat(context.getBean(SystemUserConfigurationProperties.class).getRetryDelay())
        .isEqualTo(1000));
  }

  @Test
  void usernameTemplate_positive_boundFromEnvironmentVariable() {
    contextRunner
      .withSystemProperties("SYSTEM_USER_USERNAME_TEMPLATE=custom-{tenantId}-user")
      .run(context -> assertThat(context.getBean(SystemUserConfigurationProperties.class).getUsernameTemplate())
        .isEqualTo("custom-{tenantId}-user"));
  }

  @Test
  void emailTemplate_positive_boundFromEnvironmentVariable() {
    contextRunner
      .withSystemProperties("SYSTEM_USER_EMAIL_TEMPLATE=custom-{tenantId}@example.org")
      .run(context -> assertThat(context.getBean(SystemUserConfigurationProperties.class).getEmailTemplate())
        .isEqualTo("custom-{tenantId}@example.org"));
  }

  @Test
  void systemUserRole_positive_boundFromEnvironmentVariable() {
    contextRunner
      .withSystemProperties("SYSTEM_USER_ROLE=CustomRole")
      .run(context -> assertThat(context.getBean(SystemUserConfigurationProperties.class).getSystemUserRole())
        .isEqualTo("CustomRole"));
  }

  @Test
  void passwordLength_positive_boundFromEnvironmentVariable() {
    contextRunner
      .withSystemProperties("SYSTEM_USER_PASSWORD_LENGTH=48")
      .run(context -> assertThat(context.getBean(SystemUserConfigurationProperties.class).getPasswordLength())
        .isEqualTo(48));
  }

  @Configuration
  @EnableConfigurationProperties(SystemUserConfigurationProperties.class)
  static class TestConfiguration {}
}
