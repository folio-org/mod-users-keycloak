package org.folio.uk.controller;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.folio.uk.domain.dto.CompositeUser;
import org.folio.uk.domain.dto.IncludedField;
import org.folio.uk.domain.dto.PermissionsContainer;
import org.folio.uk.domain.dto.User;
import org.folio.uk.rest.resource.UsersApi;
import org.folio.uk.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserController implements UsersApi {

  private final UserService service;

  @Override
  public ResponseEntity<User> createUser(User user, Boolean keycloakOnly) {
    return ResponseEntity.status(CREATED).body(service.createUser(user, keycloakOnly));
  }

  @Override
  public ResponseEntity<User> getUser(UUID id) {
    return ResponseEntity.ok(service.getUser(id).orElseThrow(() -> new EntityNotFoundException("Not Found")));
  }

  @Override
  public ResponseEntity<CompositeUser> getUserBySelfReference(List<IncludedField> include, Boolean expandPermissions) {
    return ResponseEntity.ok(service.getUserBySelfReference(include, expandPermissions));
  }

  @Override
  public ResponseEntity<String> updateUser(UUID id, User user) {
    service.updateUser(id, user);
    return ResponseEntity.status(NO_CONTENT).build();
  }

  @Override
  public ResponseEntity<String> deleteUser(UUID id) {
    service.deleteUser(id);
    return ResponseEntity.status(NO_CONTENT).build();
  }

  @Override
  public ResponseEntity<PermissionsContainer> findPermissions(UUID userId, List<String> permissions) {
    var resolvedPermissions = service.resolvePermissions(userId, permissions);
    var result = new PermissionsContainer().permissions(resolvedPermissions);
    return ResponseEntity.ok(result);
  }
}
