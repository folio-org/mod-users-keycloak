package org.folio.uk.controller;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.folio.uk.domain.dto.User;
import org.folio.uk.integration.keycloak.KeycloakService;
import org.folio.uk.rest.resource.AuthUserApi;
import org.folio.uk.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthUserController implements AuthUserApi {

  private final KeycloakService keycloakService;
  private final UserService userService;

  @Override
  public ResponseEntity<Void> createAuthUser(UUID userId) {
    boolean userExists = keycloakService.findKeycloakUserWithUserIdAttr(userId).isPresent();
    if (userExists) {
      return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    return userService.getUser(userId).map(this::createKeycloakUser)
      .orElseGet(() -> ResponseEntity.notFound().build());
  }

  private ResponseEntity<Void> createKeycloakUser(User user) {
    if (StringUtils.isBlank(user.getUsername())) {
      return ResponseEntity.badRequest().build();
    }
    userService.createUser(user, true);
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  @Override
  public ResponseEntity<String> checkIfExistsAuthUserById(UUID userId) {
    boolean userExists = keycloakService.findKeycloakUserWithUserIdAttr(userId).isPresent();
    return userExists ? ResponseEntity.status(HttpStatus.NO_CONTENT).build()
      : ResponseEntity.status(HttpStatus.NOT_FOUND).build();
  }
}
