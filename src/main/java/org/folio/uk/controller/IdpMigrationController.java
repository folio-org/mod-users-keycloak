package org.folio.uk.controller;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NO_CONTENT;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.uk.domain.dto.UsersIdp;
import org.folio.uk.migration.IdpMigrationService;
import org.folio.uk.rest.resource.IdpMigrationApi;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@Log4j2
@RestController
@RequiredArgsConstructor
public class IdpMigrationController implements IdpMigrationApi {

  private final IdpMigrationService idpMigrationService;

  @Override
  public ResponseEntity<String> linkUserIdpMigration(UsersIdp usersIdp) {
    try {
      idpMigrationService.linkUserIdpMigration(usersIdp);
      return ResponseEntity.status(NO_CONTENT).build();
    } catch (IllegalStateException e) {
      log.error("Caught a validation exception: ", e);
      return ResponseEntity.status(BAD_REQUEST).body(e.getMessage());
    }
  }

  @Override
  public ResponseEntity<String> unlinkUserIdpMigration(UsersIdp usersIdp) {
    try {
      idpMigrationService.unlinkUserIdpMigration(usersIdp);
      return ResponseEntity.status(NO_CONTENT).build();
    } catch (IllegalStateException e) {
      log.error("Caught a validation exception: ", e);
      return ResponseEntity.status(BAD_REQUEST).body(e.getMessage());
    }
  }
}
