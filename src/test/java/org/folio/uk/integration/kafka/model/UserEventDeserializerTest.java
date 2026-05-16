package org.folio.uk.integration.kafka.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.uk.support.TestConstants.TENANT_NAME;
import static org.folio.uk.support.TestConstants.USER_ID;

import java.nio.charset.StandardCharsets;
import org.apache.kafka.common.errors.SerializationException;
import org.folio.integration.kafka.model.ResourceEventType;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

@UnitTest
class UserEventDeserializerTest {

  private static final String EVENT_ID = "event-id-1";
  private static final long TIMESTAMP = 1700000000000L;

  private final UserEventDeserializer deserializer = new UserEventDeserializer(new JsonMapper());

  @Test
  void deserialize_positive_nullData() {
    var result = deserializer.deserialize("topic", null);

    assertThat(result).isNull();
  }

  @Test
  void deserialize_positive_updateEvent() {
    var json = """
      {
        "id": "%s",
        "tenant": "%s",
        "timestamp": %d,
        "type": "UPDATED",
        "data": {
          "new": {"id": "%s", "username": "user1", "active": false},
          "old": {"id": "%s", "username": "user1", "active": true}
        }
      }
      """.formatted(EVENT_ID, TENANT_NAME, TIMESTAMP, USER_ID, USER_ID);

    var result = deserializer.deserialize("topic", bytes(json));

    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(EVENT_ID);
    assertThat(result.getTenant()).isEqualTo(TENANT_NAME);
    assertThat(result.getTimestamp()).isEqualTo(TIMESTAMP);
    assertThat(result.getType()).isEqualTo(ResourceEventType.UPDATE);
    assertThat(result.getNewValue()).isNotNull();
    assertThat(result.getNewValue().getId()).isEqualTo(USER_ID);
    assertThat(result.getNewValue().getActive()).isFalse();
    assertThat(result.getOldValue()).isNotNull();
    assertThat(result.getOldValue().getActive()).isTrue();
  }

  @Test
  void deserialize_positive_createdType() {
    var json = """
      {"id": "%s", "tenant": "%s", "timestamp": %d, "type": "CREATED", "data": {"new": {"id": "%s"}}}
      """.formatted(EVENT_ID, TENANT_NAME, TIMESTAMP, USER_ID);

    var result = deserializer.deserialize("topic", bytes(json));

    assertThat(result).isNotNull();
    assertThat(result.getType()).isEqualTo(ResourceEventType.CREATE);
    assertThat(result.getNewValue()).isNotNull();
    assertThat(result.getOldValue()).isNull();
  }

  @Test
  void deserialize_positive_deletedType() {
    var json = """
      {"id": "%s", "tenant": "%s", "timestamp": %d, "type": "DELETED", "data": {"old": {"id": "%s"}}}
      """.formatted(EVENT_ID, TENANT_NAME, TIMESTAMP, USER_ID);

    var result = deserializer.deserialize("topic", bytes(json));

    assertThat(result).isNotNull();
    assertThat(result.getType()).isEqualTo(ResourceEventType.DELETE);
    assertThat(result.getNewValue()).isNull();
    assertThat(result.getOldValue()).isNotNull();
  }

  @Test
  void deserialize_positive_noDataField() {
    var json = """
      {"id": "%s", "tenant": "%s", "timestamp": %d, "type": "UPDATED"}
      """.formatted(EVENT_ID, TENANT_NAME, TIMESTAMP);

    var result = deserializer.deserialize("topic", bytes(json));

    assertThat(result).isNotNull();
    assertThat(result.getNewValue()).isNull();
    assertThat(result.getOldValue()).isNull();
  }

  @Test
  void deserialize_negative_unknownEventType() {
    var json = """
      {"id": "%s", "tenant": "%s", "timestamp": %d, "type": "UNKNOWN"}
      """.formatted(EVENT_ID, TENANT_NAME, TIMESTAMP);

    assertThatThrownBy(() -> deserializer.deserialize("topic", bytes(json)))
      .isInstanceOf(SerializationException.class)
      .hasMessageContaining("Failed to deserialize User Event from message");
  }

  @Test
  void deserialize_negative_malformedJson() {
    assertThatThrownBy(() -> deserializer.deserialize("topic", bytes("not-json{")))
      .isInstanceOf(SerializationException.class)
      .hasMessageContaining("Failed to deserialize User Event from message");
  }

  private static byte[] bytes(String json) {
    return json.getBytes(StandardCharsets.UTF_8);
  }
}
