package org.folio.uk.domain;

import java.util.UUID;
import org.folio.spring.cql.JpaCqlRepository;
import org.folio.uk.domain.dto.UserMigrationJobStatus;
import org.folio.uk.domain.entity.UserMigrationJobEntity;
import org.springframework.stereotype.Repository;

@Repository
public interface UserMigrationJobRepository extends JpaCqlRepository<UserMigrationJobEntity, UUID> {

  boolean existsByStatus(UserMigrationJobStatus status);
}
