package org.folio.uk.integration.keycloak.model;

import lombok.Data;

@Data
public class KeycloakRealmConfiguration {

  /**
   * Keycloak realm client id.
   */
  private String clientId;

  /**
   * Keycloak realm client secret.
   */
  private String clientSecret;

  /**
   * Sets clientId and returns {@link KeycloakRealmConfiguration} object.
   *
   * @param clientId - keycloak client id.
   * @return {@link KeycloakRealmConfiguration} object
   */
  public KeycloakRealmConfiguration clientId(String clientId) {
    this.clientId = clientId;
    return this;
  }

  /**
   * Sets clientSecret and returns {@link KeycloakRealmConfiguration} object.
   *
   * @param clientSecret - keycloak client secret.
   * @return {@link KeycloakRealmConfiguration} object
   */
  public KeycloakRealmConfiguration clientSecret(String clientSecret) {
    this.clientSecret = clientSecret;
    return this;
  }
}
