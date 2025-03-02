package org.folio.uk.integration.keycloak.model;

import java.util.UUID;

public record KeycloakIdentityProviderDto(String tenant, UUID userId, String memberTenant, String providerAlias) {
}
