
package org.folio.uk.integration.login.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.validation.constraints.NotNull;
import java.util.Date;
import java.util.UUID;

/**
 * Reset password action entity.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
  "id",
  "userId",
  "expirationTime"
})
public class PasswordResetAction {

  /**
   * ID of the password reset action.
   * (Required)
   */
  @JsonProperty("id")
  @JsonPropertyDescription("ID of the password reset action")
  @NotNull
  private String id;

  /**
   * User ID to register password reset action.
   * (Required)
   */
  @JsonProperty("userId")
  @JsonPropertyDescription("User ID to register password reset action")
  @NotNull
  private UUID userId;

  /**
   * Action expiration time.
   * (Required)
   */
  @JsonProperty("expirationTime")
  @JsonPropertyDescription("Action expiration time")
  @NotNull
  private Date expirationTime;

  /**
   * ID of the password reset action.
   * (Required)
   */
  @JsonProperty("id")
  public String getId() {
    return id;
  }

  /**
   * ID of the password reset action.
   * (Required)
   */
  @JsonProperty("id")
  public void setId(String id) {
    this.id = id;
  }

  public PasswordResetAction withId(String id) {
    this.id = id;
    return this;
  }

  /**
   * User ID to register password reset action.
   * (Required)
   */
  @JsonProperty("userId")
  public UUID getUserId() {
    return userId;
  }

  /**
   * User ID to register password reset action.
   * (Required)
   */
  @JsonProperty("userId")
  public void setUserId(UUID userId) {
    this.userId = userId;
  }

  public PasswordResetAction withUserId(UUID userId) {
    this.userId = userId;
    return this;
  }

  /**
   * Action expiration time.
   * (Required)
   */
  @JsonProperty("expirationTime")
  public Date getExpirationTime() {
    return expirationTime;
  }

  /**
   * Action expiration time.
   * (Required)
   */
  @JsonProperty("expirationTime")
  public void setExpirationTime(Date expirationTime) {
    this.expirationTime = expirationTime;
  }

  public PasswordResetAction withExpirationTime(Date expirationTime) {
    this.expirationTime = expirationTime;
    return this;
  }
}
