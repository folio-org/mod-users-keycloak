
package org.folio.uk.integration.notify.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.UUID;
import javax.validation.Valid;
import lombok.Data;

/**
 * Notification schema for mod-notify.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
  "eventConfigName",
  "recipientId",
  "text",
  "lang",
  "context"
})
@Data
public class Notification {

  /**
   * Unique event config name.
   */
  @JsonProperty("eventConfigName")
  @JsonPropertyDescription("Unique event config name")
  private String eventConfigName;
  /**
   * The UUID of the receiving user.
   */
  @JsonProperty("recipientId")
  @JsonPropertyDescription("The UUID of the receiving user")
  private UUID recipientId;
  /**
   * The text of this notification.
   */
  @JsonProperty("text")
  @JsonPropertyDescription("The text of this notification")
  private String text;
  /**
   * Notification language.
   */
  @JsonProperty("lang")
  @JsonPropertyDescription("Notification language")
  private String lang;
  /**
   * Context object.
   */
  @JsonProperty("context")
  @JsonPropertyDescription("Context object")
  @Valid
  private Context context;

  public Notification withEventConfigName(String eventConfigName) {
    this.eventConfigName = eventConfigName;
    return this;
  }

  public Notification withRecipientId(UUID recipientId) {
    this.recipientId = recipientId;
    return this;
  }

  public Notification withText(String text) {
    this.text = text;
    return this;
  }

  public Notification withLang(String lang) {
    this.lang = lang;
    return this;
  }

  public Notification withContext(Context context) {
    this.context = context;
    return this;
  }
}
