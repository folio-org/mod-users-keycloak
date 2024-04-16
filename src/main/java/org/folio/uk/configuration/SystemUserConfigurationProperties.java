package org.folio.uk.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties("application.system.user")
public class SystemUserConfigurationProperties {

  /**
   * System username template.
   *
   * <p>
   * Allowed placeholders:
   * <ul>
   *   <li>{tenantId} - tenant identifier as name from {@link org.folio.spring.FolioExecutionContext}</li>
   * </ul>
   * </p>
   */
  private String usernameTemplate = "{tenantId}-system-user";

  /**
   * System username template.
   *
   * <p>
   * Allowed placeholders:
   * <ul>
   *   <li>{tenantId} - tenant identifier as name from {@link org.folio.spring.FolioExecutionContext}</li>
   * </ul>
   * </p>
   */
  private String emailTemplate = "{tenantId}-system-user@folio.org";

  /**
   * System user role name.
   */
  private String systemUserRole = "System";

  /**
   * System user password length used by password generator.
   */
  private int passwordLength = 32;

  /**
   * Retry delay for system user creation.
   *
   * <p>This property is required to resolve installation request for mod-users</p>
   */
  private int retryDelay = 250;

  /**
   * A number for Retry attempts for system user creation.
   *
   * <p>This property is required to resolve installation request for mod-users</p>
   */
  private int retryAttempts = 10;
}
