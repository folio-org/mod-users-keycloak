package org.folio.uk.integration.kafka.model;

import static java.util.function.Function.identity;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import org.folio.integration.kafka.model.ResourceEventType;
import org.folio.uk.domain.dto.User;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Kafka {@link Deserializer} that converts raw JSON bytes from the {@code users.users} topic
 * into a {@link UserEvent}, mapping wire-format event type strings (CREATED/UPDATED/DELETED)
 * to their {@link org.folio.integration.kafka.model.ResourceEventType} counterparts.
 */
@RequiredArgsConstructor
public class UserEventDeserializer implements Deserializer<UserEvent> {

  private final JsonMapper jsonMapper;

  /**
   * Deserializes a raw Kafka message payload into a {@link UserEvent}.
   *
   * @param topic the Kafka topic the message was received on
   * @param data  the raw bytes of the Kafka message value; {@code null} returns {@code null}
   * @return the deserialized {@link UserEvent}, or {@code null} if {@code data} is {@code null}
   * @throws org.apache.kafka.common.errors.SerializationException if deserialization fails
   */
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
        + new String(data, StandardCharsets.UTF_8), e);
    }
  }

  /**
   * Populates the {@code oldValue} and {@code newValue} fields on the builder from the
   * {@code data} node of the event JSON, if present.
   *
   * @param root    the root {@link JsonNode} of the event JSON
   * @param builder the {@link UserEvent.UserEventBuilder} to populate
   */
  private void deserializeUserData(JsonNode root, UserEvent.UserEventBuilder builder) {
    if (root.has("data")) {
      var eventData = root.get("data");

      readAndSet(eventData, "old", User.class, builder::oldValue);
      readAndSet(eventData, "new", User.class, builder::newValue);
    }
  }

  /**
   * Reads {@code fieldName} from {@code node} as type {@code fieldClass} and passes the value to
   * {@code consumer}; does nothing if the field is absent.
   *
   * @param <T>       the target field type
   * @param node      the JSON node to read from
   * @param fieldName the field name to look up
   * @param fieldClass the expected Java type of the field value
   * @param consumer  the setter that receives the extracted value
   */
  private <T> void readAndSet(JsonNode node, String fieldName, Class<T> fieldClass, Consumer<T> consumer) {
    readAndSet(node, fieldName, fieldClass, identity(), consumer);
  }

  /**
   * Reads {@code fieldName} from {@code node} as type {@code fieldClass}, applies {@code mapper},
   * and passes the result to {@code setter}; does nothing if the field is absent.
   *
   * @param <T>       the raw field type extracted from JSON
   * @param <E>       the mapped type passed to the setter
   * @param node      the JSON node to read from
   * @param fieldName the field name to look up
   * @param fieldClass the expected Java type of the raw field value
   * @param mapper    a conversion function applied to the raw value before passing to the setter
   * @param setter    the setter that receives the mapped value
   */
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

  /**
   * Converts a wire-format event type string to the corresponding {@link ResourceEventType}.
   *
   * @param value the raw event type string ({@code "CREATED"}, {@code "UPDATED"}, or {@code "DELETED"})
   * @return the matching {@link ResourceEventType}
   * @throws IllegalArgumentException if {@code value} is not a recognised event type
   */
  private static ResourceEventType toResourceEventType(String value) {
    return switch (value) {
      case "CREATED" -> ResourceEventType.CREATE;
      case "UPDATED" -> ResourceEventType.UPDATE;
      case "DELETED" -> ResourceEventType.DELETE;
      default -> throw new IllegalArgumentException("Unknown event type: " + value);
    };
  }
}
