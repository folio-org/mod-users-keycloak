package org.folio.uk.migration;

import static feign.Util.isBlank;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.ListUtils.partition;
import static org.folio.common.utils.CollectionUtils.toStream;
import static org.folio.spring.scope.FolioExecutionScopeExecutionContextManager.getRunnableWithCurrentFolioContext;

import feign.FeignException;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.data.OffsetRequest;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.folio.uk.domain.UserMigrationJobRepository;
import org.folio.uk.domain.dto.User;
import org.folio.uk.domain.dto.UserMigrationJob;
import org.folio.uk.domain.dto.UserMigrationJobStatus;
import org.folio.uk.domain.dto.UserMigrationJobs;
import org.folio.uk.domain.dto.UserTenant;
import org.folio.uk.domain.entity.EntityUserMigrationJobStatus;
import org.folio.uk.domain.entity.UserMigrationJobEntity;
import org.folio.uk.exception.RequestValidationException;
import org.folio.uk.integration.keycloak.KeycloakService;
import org.folio.uk.integration.permission.PermissionService;
import org.folio.uk.integration.users.UserTenantsClient;
import org.folio.uk.integration.users.UsersClient;
import org.folio.uk.mapper.UserMigrationMapper;
import org.folio.uk.migration.properties.UserMigrationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Service
@Transactional
@RequiredArgsConstructor
public class UserMigrationService {

  private static final String INVALID_EMAIL_ERROR_MESSAGE = "Invalid email address.";

  private final PermissionService permissionService;
  private final UserMigrationProperties migrationProperties;
  private final UserMigrationJobRepository repository;
  private final KeycloakService keycloakService;
  private final UsersClient usersClient;
  private final UserMigrationMapper mapper;
  private final FolioExecutionContext folioContext;
  private final UserTenantsClient userTenantsClient;

  @Transactional(readOnly = true)
  public UserMigrationJob getMigrationById(UUID id) {
    var entity = repository.findById(id)
      .orElseThrow(() -> new EntityNotFoundException("Migration is not found: id = " + id));
    return mapper.toDto(entity);
  }

  @Transactional(readOnly = true)
  public UserMigrationJobs getMigrationsByQuery(String query, Integer offset, Integer limit) {
    var offsetReq = OffsetRequest.of(offset, limit);

    var page = StringUtils.isBlank(query)
      ? repository.findAll(offsetReq)
      : repository.findByCql(query, offsetReq);

    return mapper.toDtoCollection(page);
  }

  public void deleteMigrationById(UUID id) {
    repository.findById(id).ifPresent(repository::delete);
  }

  public UserMigrationJob createMigration() {

    var migration = buildUserMigrationsEntity();
    var userIds = permissionService.findUsersIdsWithPermissions();

    validateRunningMigrations(userIds);
    migration.setTotalRecords(userIds.size());
    repository.save(migration);
    repository.flush();

    startMigration(userIds, migration);

    return mapper.toDto(migration);
  }

  private void startMigration(List<String> userIds, UserMigrationJobEntity job) {
    var partitions = partition(userIds, migrationProperties.getBatchSize());
    CompletableFuture.allOf(partitions.stream()
        .map(part -> CompletableFuture.runAsync(getRunnableWithCurrentFolioContext(() -> findThenCreateUsers(part))))
        .toArray(CompletableFuture[]::new))
      .whenComplete(migrationCompleteHandler(job, (FolioExecutionContext) folioContext.getInstance()));
  }

  private BiConsumer<Void, ? super Throwable> migrationCompleteHandler(UserMigrationJobEntity job,
                                                                       FolioExecutionContext context) {
    return (result, ex) -> {
      try (var ignored = new FolioExecutionContextSetter(context)) {
        EntityUserMigrationJobStatus status;
        if (ex != null) {
          status = EntityUserMigrationJobStatus.FAILED;
          log.error("User Migration was failed. Id: {}", job.getId(), ex);
        } else {
          status = EntityUserMigrationJobStatus.FINISHED;
          log.info("User Migration was successfully finished. Id: {} Total Records: {}",
            job.getId(), job.getTotalRecords());
        }
        repository.findById(job.getId())
          .ifPresent(entity -> {
            entity.setStatus(status);
            entity.setFinishedAt(Instant.now());
            repository.save(entity);
          });
      }
    };
  }

  private UserMigrationJobEntity buildUserMigrationsEntity() {
    var migration = new UserMigrationJobEntity();
    migration.setId(UUID.randomUUID());
    migration.setStatus(EntityUserMigrationJobStatus.IN_PROGRESS);
    migration.setStartedAt(Instant.now());
    return migration;
  }

  private void validateRunningMigrations(List<String> userIds) {
    if (repository.existsByStatus(EntityUserMigrationJobStatus.IN_PROGRESS)) {
      throw new RequestValidationException("There is already exists active migration job", "status",
        UserMigrationJobStatus.IN_PROGRESS);
    }
    if (isEmpty(userIds)) {
      throw new RequestValidationException("Nothing to migrate, there are no users");
    }
  }

  private void findThenCreateUsers(List<String> userIds) {
    var query = createSearchingUsersByIdsQuery(userIds);
    var users = usersClient.query(query, userIds.size());
    if (isNull(users) || isEmpty(users.getUsers())) {
      return;
    }
    users.getUsers().removeIf(this::isNotValidUser);
    users.getUsers().forEach(user -> createUserInKeycloakSafe(user, true));
  }

  private boolean isNotValidUser(User user) {
    if (isBlank(user.getUsername())) {
      log.info("User has been filtered by blank username: userId = {}", user.getId());
      return true;
    } else {
      return false;
    }
  }

  /**
   * Creates a user in Keycloak, with the given user object and sets the user's password to their username
   * if migration.default-passwords-on-migration property is set to true.
   * If the creation fails, the method will log a warning and return an empty Optional.
   * If the creation fails due to an invalid email, the method will try to create the user without an email and retry
   * once if "retryIfEmailNotValid" is set to true.
   *
   * @param user                 the {@link User} object to be created in Keycloak
   * @param retryIfEmailNotValid a boolean value indicating whether to retry creating the user without an email
   *                             if the email is invalid
   * @return an Optional of the created user object if the creation is successful, otherwise an empty Optional
   */
  private Optional<User> createUserInKeycloakSafe(User user, boolean retryIfEmailNotValid) {
    var password = migrationProperties.isDefaultPasswordsOnMigration() ? user.getUsername() : null;
    try {
      keycloakService.createUserForMigration(user, password, fetchUserTenants(user.getId()));
      return of(user);
    } catch (Exception e) {
      var message = e.getCause().getMessage();
      if (retryIfEmailNotValid && isEmailNotValidError(message)) {
        if (nonNull(user.getPersonal())) {
          log.warn("User email is not valid: username = {}, email = {}. Try to save user without email...",
            user.getUsername(), user.getPersonal().getEmail());
          user.getPersonal().setEmail(null);
          return createUserInKeycloakSafe(user, false);
        }
      } else {
        log.warn("Cannot create user in Keycloak: username = {}, userId = {}, cause = {}", user.getUsername(),
          user.getId(), message);
      }
      return empty();
    }
  }

  private List<String> fetchUserTenants(UUID userId) {
    try {
      var userTenantsList = userTenantsClient.lookupByUserId(userId);

      return toStream(userTenantsList.getUserTenants())
        .map(UserTenant::getTenantId)
        .collect(toList());
    } catch (FeignException e) {
      log.warn("Cannot fetch user tenants: userId = {}", userId, e);
      return List.of(folioContext.getTenantId());
    }
  }

  private boolean isEmailNotValidError(String message) {
    return StringUtils.contains(message, INVALID_EMAIL_ERROR_MESSAGE);
  }

  private String createSearchingUsersByIdsQuery(List<String> userIds) {
    var stringBuilder = new StringBuilder("id=(");
    for (var i = 0; i < userIds.size(); i++) {
      stringBuilder.append(userIds.get(i));
      if (i < userIds.size() - 1) {
        stringBuilder.append(" or ");
      } else {
        stringBuilder.append(")");
      }
    }
    return stringBuilder.toString();
  }
}
