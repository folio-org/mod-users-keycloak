package org.folio.uk.mapper;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;

import java.util.List;
import org.folio.uk.domain.dto.UserMigrationJob;
import org.folio.uk.domain.dto.UserMigrationJobs;
import org.folio.uk.domain.entity.UserMigrationJobEntity;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.springframework.data.domain.Page;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = MappingMethods.class)
public interface UserMigrationMapper {

  UserMigrationJobEntity toEntity(UserMigrationJob dto);

  UserMigrationJob toDto(UserMigrationJobEntity entity);

  List<UserMigrationJob> toDtos(Iterable<UserMigrationJobEntity> entity);

  default UserMigrationJobs toDtoCollection(Page<UserMigrationJobEntity> pageable) {
    List<UserMigrationJob> dtos = emptyIfNull(toDtos(pageable));

    return new UserMigrationJobs()
      .migrations(dtos)
      .totalRecords((int) pageable.getTotalElements());
  }
}
