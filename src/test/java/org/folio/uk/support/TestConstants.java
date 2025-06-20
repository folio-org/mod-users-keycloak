package org.folio.uk.support;

import static java.time.OffsetDateTime.parse;
import static org.folio.uk.domain.dto.User.PreferredEmailCommunicationEnum.SUPPORT;
import static org.folio.uk.utils.UserUtils.ORIGINAL_TENANT_ID_CUSTOM_FIELD;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.folio.uk.domain.dto.Personal;
import org.folio.uk.domain.dto.User;
import org.folio.uk.domain.model.UserType;
import org.folio.uk.integration.kafka.model.ResourceEvent;
import org.folio.uk.integration.kafka.model.SystemUserEvent;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TestConstants {

  public static final String OKAPI_AUTH_TOKEN = "X-Okapi-Token test value";

  public static final UUID USER_ID = UUID.fromString("d3958402-2f80-421b-a527-9933245a3556");
  public static final String USER_NAME = "ZakirBailey";
  public static final UUID USER_PATRON_GROUP_ID = UUID.fromString("503a81cd-6c26-400f-b620-14c08943697c");
  public static final Date USER_ENROLLMENT_DATE = Date.from(parse("2020-10-07T04:00:00.000+00:00").toInstant());
  public static final Date USER_EXPIRATION_DATE = Date.from(parse("2023-02-28T23:59:59.000+00:00").toInstant());

  public static final String CENTRAL_TENANT_NAME = "centraltenant";
  public static final String TENANT_NAME = "testtenant";
  public static final String TOKEN_CACHE = "token";
  public static final String TOKEN_CACHE_KEY = "admin-cli-token";

  public static final String MODULE_NAME = "mod-users-keycloak";

  public static User user() {
    return user(USER_ID.toString(), USER_NAME, "new9@new.com", "newUser@folio.org");
  }

  public static User user(String id, String username, String email, String externalSystemId) {
    return new User().id(UUID.fromString(id))
      .username(username)
      .barcode("12359")
      .active(true)
      .externalSystemId(externalSystemId)
      .patronGroup(USER_PATRON_GROUP_ID)
      .enrollmentDate(USER_ENROLLMENT_DATE)
      .expirationDate(USER_EXPIRATION_DATE)
      .personal(person(email))
      .preferredEmailCommunication(Set.of(SUPPORT));
  }

  public static User shadowUser() {
    return new User().id(UUID.fromString(USER_ID.toString()))
      .username(USER_NAME + "_12345")
      .type(UserType.SHADOW.getValue())
      .barcode("12359")
      .active(true)
      .externalSystemId("newUser@folio.org")
      .patronGroup(USER_PATRON_GROUP_ID)
      .enrollmentDate(USER_ENROLLMENT_DATE)
      .expirationDate(USER_EXPIRATION_DATE)
      .personal(person("new9@new.com"))
      .customFields(Map.of(ORIGINAL_TENANT_ID_CUSTOM_FIELD, TENANT_NAME));
  }

  public static Personal person(String email) {
    return new Personal()
      .firstName("Bailey")
      .lastName("Zakir")
      .email(email)
      .pronouns("he/him");
  }

  public static SystemUserEvent systemUserEvent(Set<String> permissions) {
    return SystemUserEvent.of("mod-foo", "module", permissions);
  }

  public static ResourceEvent systemUserResourceEvent() {
    return TestValues.readValue("json/kafka/system-user-event.json", ResourceEvent.class);
  }

  public static ResourceEvent systemUserResourceDeleteEvent() {
    return TestValues.readValue("json/kafka/system-user-delete-event.json", ResourceEvent.class);
  }

  public static ResourceEvent systemUserResourceUpdateEvent() {
    return TestValues.readValue("json/kafka/system-user-update-event.json", ResourceEvent.class);
  }

  public static List<String> systemUserPermissions() {
    return TestValues.readValue("json/capability/permissions.json", new TypeReference<>() {
    });
  }
}
