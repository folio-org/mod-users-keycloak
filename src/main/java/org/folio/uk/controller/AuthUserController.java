package org.folio.uk.controller;

import static org.folio.uk.domain.dto.ErrorCode.USER_ABSENT_USERNAME;

import jakarta.persistence.EntityNotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.folio.uk.domain.dto.User;
import org.folio.uk.exception.RequestValidationException;
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
    var userExists = keycloakService.findKeycloakUserWithUserIdAttr(userId).isPresent();
    if (userExists) {
      return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    return userService.getUser(userId).map(this::createKeycloakUser)
      .orElseThrow(() -> new EntityNotFoundException("Not Found"));
  }

  private ResponseEntity<Void> createKeycloakUser(User user) {
    if (StringUtils.isBlank(user.getUsername())) {
      throw new RequestValidationException("User without username cannot be created in Keycloak",
        USER_ABSENT_USERNAME);
    }
    userService.createUser(user, true);
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  @Override
  public ResponseEntity<String> checkIfExistsAuthUserById(UUID userId) {
    return keycloakService.findKeycloakUserWithUserIdAttr(userId)
      .map(unused -> ResponseEntity.noContent().<String>build())
      .orElseThrow(() -> new EntityNotFoundException("Not Found"));
  }
}
