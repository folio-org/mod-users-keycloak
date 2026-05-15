package org.folio.uk.integration.kafka.model;

import static java.util.function.Function.identity;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import org.folio.integration.kafka.model.ResourceEventType;
import org.folio.uk.domain.dto.User;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@RequiredArgsConstructor
public class UserEventDeserializer implements Deserializer<UserEvent> {

  private final JsonMapper jsonMapper;

  @Override
  public UserEvent deserialize(String topic, byte[] data) {
    if (data == null) {
      return null;
    }

    try {
      var root = jsonMapper.readTree(data);

      var builder = UserEvent.builder();

      readAndSet(root, "id", String.class, builder::id);
      readAndSet(root, "tenant", String.class, builder::tenant);
      readAndSet(root, "timestamp", Long.class, builder::timestamp);
      readAndSet(root, "type", String.class, UserEventDeserializer::toResourceEventType, builder::type);

      deserializeUserData(root, builder);

      return builder.build();
    } catch (Exception e) {
      throw new SerializationException("Failed to deserialize User Event from message: payload = "
        + Arrays.toString(data), e);
    }
  }

  private void deserializeUserData(JsonNode root, UserEvent.UserEventBuilder builder) {
    if (root.has("data")) {
      var eventData = root.get("data");

      readAndSet(eventData, "old", User.class, builder::oldValue);
      readAndSet(eventData, "new", User.class, builder::newValue);
    }
  }

  private <T> void readAndSet(JsonNode node, String fieldName, Class<T> fieldClass, Consumer<T> consumer) {
    readAndSet(node, fieldName, fieldClass, identity(), consumer);
  }

  private <T, E> void readAndSet(JsonNode node, String fieldName, Class<T> fieldClass,
    Function<T, E> mapper, Consumer<E> setter) {
    if (node.has(fieldName)) {
      var fieldNode = node.get(fieldName);

      Object value = null;
      if (!fieldNode.isNull()) {
        if (fieldClass.equals(String.class)) {
          value = fieldNode.asString();
        } else if (fieldClass.equals(Long.class)) {
          value = fieldNode.asLong();
        } else if (fieldClass.equals(Integer.class)) {
          value = fieldNode.asInt();
        } else if (fieldClass.equals(Boolean.class)) {
          value = fieldNode.asBoolean();
        } else {
          value = jsonMapper.treeToValue(fieldNode, fieldClass);
        }
      }

      setter.accept(mapper.apply(fieldClass.cast(value)));
    }
  }

  private static ResourceEventType toResourceEventType(String value) {
    return switch (value) {
      case "CREATED" -> ResourceEventType.CREATE;
      case "UPDATED" -> ResourceEventType.UPDATE;
      case "DELETED" -> ResourceEventType.DELETE;
      default -> throw new IllegalArgumentException("Unknown event type: " + value);
    };
  }
}
