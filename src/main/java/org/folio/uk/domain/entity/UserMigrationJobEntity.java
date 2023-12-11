package org.folio.uk.domain.entity;

import io.hypersistence.utils.hibernate.type.basic.PostgreSQLEnumType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Data;
import org.folio.uk.domain.dto.UserMigrationJobStatus;
import org.hibernate.annotations.Type;

@Data
@Entity
@Table(name = "user_migration_job")
public class UserMigrationJobEntity {

  /**
   * An entity identifier.
   */
  @Id
  private UUID id;

  @Column(name = "total_records")
  private Integer totalRecords;

  /**
   * An user migration job status.
   */
  @Type(PostgreSQLEnumType.class)
  @Enumerated(EnumType.STRING)
  @Column(name = "status", columnDefinition = "user_migration_job_status_type")
  private UserMigrationJobStatus status;

  /**
   * An user migration job startup timestamp.
   */
  @Column(name = "started_at")
  private Instant startedAt;

  /**
   * A user migration job finishing timestamp.
   */
  @Column(name = "finished_at")
  private Instant finishedAt;
}
