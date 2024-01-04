
package org.folio.uk.integration.configuration.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.validation.constraints.NotNull;

/**
 * Configuration.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
  "id",
  "module",
  "configName",
  "code",
  "description",
  "default",
  "enabled",
  "value",
  "userId",
  "metadata"
})
public class Config {

  /**
   * id.
   */
  @JsonProperty("id")
  @JsonPropertyDescription("id")
  private String id;
  /**
   * global module name.
   * (Required)
   */
  @JsonProperty("module")
  @JsonPropertyDescription("global module name")
  @NotNull
  private String module;
  /**
   * description.
   * (Required)
   */
  @JsonProperty("configName")
  @JsonPropertyDescription("description")
  @NotNull
  private String configName;
  /**
   * module name.
   */
  @JsonProperty("code")
  @JsonPropertyDescription("module name")
  private String code;
  /**
   * description.
   */
  @JsonProperty("description")
  @JsonPropertyDescription("description")
  private String description;
  /**
   * default value.
   */
  @JsonProperty("default")
  @JsonPropertyDescription("default value")
  private Boolean defaultValue;
  /**
   * configuration availability.
   */
  @JsonProperty("enabled")
  @JsonPropertyDescription("configuration availability")
  private Boolean enabled;
  /**
   * value.
   */
  @JsonProperty("value")
  @JsonPropertyDescription("value")
  private String value;
  /**
   * user id.
   */
  @JsonProperty("userId")
  @JsonPropertyDescription("user id")
  private String userId;
  /**
   * metadata.
   */
  @JsonProperty("metadata")
  @JsonPropertyDescription("metadata")
  private Object metadata;

  /**
   * id.
   */
  @JsonProperty("id")
  public String getId() {
    return id;
  }

  /**
   * id.
   */
  @JsonProperty("id")
  public void setId(String id) {
    this.id = id;
  }

  public Config withId(String id) {
    this.id = id;
    return this;
  }

  /**
   * global module name.
   * (Required)
   */
  @JsonProperty("module")
  public String getModule() {
    return module;
  }

  /**
   * global module name.
   * (Required)
   */
  @JsonProperty("module")
  public void setModule(String module) {
    this.module = module;
  }

  public Config withModule(String module) {
    this.module = module;
    return this;
  }

  /**
   * description.
   * (Required)
   */
  @JsonProperty("configName")
  public String getConfigName() {
    return configName;
  }

  /**
   * description.
   * (Required)
   */
  @JsonProperty("configName")
  public void setConfigName(String configName) {
    this.configName = configName;
  }

  public Config withConfigName(String configName) {
    this.configName = configName;
    return this;
  }

  /**
   * module name.
   */
  @JsonProperty("code")
  public String getCode() {
    return code;
  }

  /**
   * module name.
   */
  @JsonProperty("code")
  public void setCode(String code) {
    this.code = code;
  }

  public Config withCode(String code) {
    this.code = code;
    return this;
  }

  /**
   * description.
   */
  @JsonProperty("description")
  public String getDescription() {
    return description;
  }

  /**
   * description.
   */
  @JsonProperty("description")
  public void setDescription(String description) {
    this.description = description;
  }

  public Config withDescription(String description) {
    this.description = description;
    return this;
  }

  @JsonProperty("default")
  public Boolean getDefault() {
    return defaultValue;
  }

  @JsonProperty("default")
  public void setDefault(Boolean defaultValue) {
    this.defaultValue = defaultValue;
  }

  public Config withDefault(Boolean defaultValue) {
    this.defaultValue = defaultValue;
    return this;
  }

  @JsonProperty("enabled")
  public Boolean getEnabled() {
    return enabled;
  }

  @JsonProperty("enabled")
  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public Config withEnabled(Boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  @JsonProperty("value")
  public String getValue() {
    return value;
  }

  @JsonProperty("value")
  public void setValue(String value) {
    this.value = value;
  }

  public Config withValue(String value) {
    this.value = value;
    return this;
  }

  @JsonProperty("userId")
  public String getUserId() {
    return userId;
  }

  @JsonProperty("userId")
  public void setUserId(String userId) {
    this.userId = userId;
  }

  public Config withUserId(String userId) {
    this.userId = userId;
    return this;
  }

  @JsonProperty("metadata")
  public Object getMetadata() {
    return metadata;
  }

  @JsonProperty("metadata")
  public void setMetadata(Object metadata) {
    this.metadata = metadata;
  }

  public Config withMetadata(Object metadata) {
    this.metadata = metadata;
    return this;
  }
}
