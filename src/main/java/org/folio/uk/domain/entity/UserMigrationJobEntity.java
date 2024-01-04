package org.folio.uk.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(name = "status", columnDefinition = "user_migration_job_status_type")
  private EntityUserMigrationJobStatus status;

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
