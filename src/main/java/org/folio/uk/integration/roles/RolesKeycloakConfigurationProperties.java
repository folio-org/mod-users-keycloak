package org.folio.uk.integration.roles;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "application.mod-roles-keycloak")
public class RolesKeycloakConfigurationProperties {

  private boolean includeOnlyVisiblePermissions = true;
}
