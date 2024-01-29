package org.folio.uk.controller;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.uk.domain.dto.UserMigrationJob;
import org.folio.uk.domain.dto.UserMigrationJobs;
import org.folio.uk.migration.UserMigrationService;
import org.folio.uk.rest.resource.MigrationApi;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@Log4j2
@RestController
@RequiredArgsConstructor
public class MigrationController implements MigrationApi {

  private final UserMigrationService service;

  @Override
  public ResponseEntity<UserMigrationJob> migrateUsers() {
    var userMigrationjob = service.createMigration();
    return ResponseEntity.status(CREATED).body(userMigrationjob);
  }

  @Override
  public ResponseEntity<String> deleteMigration(UUID id) {
    service.deleteMigrationById(id);
    return ResponseEntity.status(NO_CONTENT).build();
  }

  @Override
  public ResponseEntity<UserMigrationJob> getMigration(UUID id) {
    var migration = service.getMigrationById(id);
    return ResponseEntity.ok(migration);
  }

  @Override
  public ResponseEntity<UserMigrationJobs> getMigrations(String query, Integer offset, Integer limit) {
    var migrations = service.getMigrationsByQuery(query, offset, limit);
    return ResponseEntity.ok(migrations);
  }
}
