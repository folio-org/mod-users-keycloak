package org.folio.uk.configuration;

import org.folio.uk.integration.configuration.ConfigurationClient;
import org.folio.uk.integration.inventory.ServicePointsClient;
import org.folio.uk.integration.inventory.ServicePointsUserClient;
import org.folio.uk.integration.login.LoginClient;
import org.folio.uk.integration.notify.NotificationClient;
import org.folio.uk.integration.password.PasswordValidatorClient;
import org.folio.uk.integration.permission.PermissionClient;
import org.folio.uk.integration.policy.PolicyClient;
import org.folio.uk.integration.roles.CapabilitiesClient;
import org.folio.uk.integration.roles.UserCapabilitiesClient;
import org.folio.uk.integration.roles.UserCapabilitySetClient;
import org.folio.uk.integration.roles.UserPermissionsClient;
import org.folio.uk.integration.roles.UserRolesClient;
import org.folio.uk.integration.roles.dafaultrole.DefaultRolesClient;
import org.folio.uk.integration.settings.SettingsClient;
import org.folio.uk.integration.users.UserTenantsClient;
import org.folio.uk.integration.users.UsersClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class HttpClientConfiguration {

  @Bean
  public ConfigurationClient configurationClient(HttpServiceProxyFactory factory) {
    return factory.createClient(ConfigurationClient.class);
  }

  @Bean
  public ServicePointsClient servicePointsClient(HttpServiceProxyFactory factory) {
    return factory.createClient(ServicePointsClient.class);
  }

  @Bean
  public ServicePointsUserClient servicePointsUserClient(HttpServiceProxyFactory factory) {
    return factory.createClient(ServicePointsUserClient.class);
  }

  @Bean
  public LoginClient loginClient(HttpServiceProxyFactory factory) {
    return factory.createClient(LoginClient.class);
  }

  @Bean
  public NotificationClient notificationClient(HttpServiceProxyFactory factory) {
    return factory.createClient(NotificationClient.class);
  }

  @Bean
  public PasswordValidatorClient passwordValidatorClient(HttpServiceProxyFactory factory) {
    return factory.createClient(PasswordValidatorClient.class);
  }

  @Bean
  public PermissionClient permissionClient(HttpServiceProxyFactory factory) {
    return factory.createClient(PermissionClient.class);
  }

  @Bean
  public PolicyClient policyClient(HttpServiceProxyFactory factory) {
    return factory.createClient(PolicyClient.class);
  }

  @Bean
  public CapabilitiesClient capabilitiesClient(HttpServiceProxyFactory factory) {
    return factory.createClient(CapabilitiesClient.class);
  }

  @Bean
  public UserCapabilitiesClient userCapabilitiesClient(HttpServiceProxyFactory factory) {
    return factory.createClient(UserCapabilitiesClient.class);
  }

  @Bean
  public UserCapabilitySetClient userCapabilitySetClient(HttpServiceProxyFactory factory) {
    return factory.createClient(UserCapabilitySetClient.class);
  }

  @Bean
  public UserPermissionsClient userPermissionsClient(HttpServiceProxyFactory factory) {
    return factory.createClient(UserPermissionsClient.class);
  }

  @Bean
  public UserRolesClient userRolesClient(HttpServiceProxyFactory factory) {
    return factory.createClient(UserRolesClient.class);
  }

  @Bean
  public SettingsClient settingsClient(HttpServiceProxyFactory factory) {
    return factory.createClient(SettingsClient.class);
  }

  @Bean
  public DefaultRolesClient defaultRolesClient(HttpServiceProxyFactory factory) {
    return factory.createClient(DefaultRolesClient.class);
  }

  @Bean
  public UserTenantsClient userTenantsClient(HttpServiceProxyFactory factory) {
    return factory.createClient(UserTenantsClient.class);
  }

  @Bean
  public UsersClient usersClient(HttpServiceProxyFactory factory) {
    return factory.createClient(UsersClient.class);
  }
}
