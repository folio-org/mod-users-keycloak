package org.folio.uk.it;

import org.folio.uk.integration.keycloak.model.KeycloakUser;

public record CreateUserVerifyDto(String authToken, KeycloakUser keycloakUser) {
}
