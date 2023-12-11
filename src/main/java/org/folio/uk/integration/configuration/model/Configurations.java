
package org.folio.uk.integration.configuration.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.ArrayList;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * Configuration List.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
  "configs",
  "totalRecords",
  "resultInfo"
})
public class Configurations {

  /**
   * configurations.
   * (Required)
   */
  @JsonProperty("configs")
  @JsonPropertyDescription("configurations")
  @Valid
  @NotNull
  private List<Config> configs = new ArrayList<Config>();
  /**
   * total records.
   * (Required)
   */
  @JsonProperty("totalRecords")
  @JsonPropertyDescription("total records")
  @NotNull
  private Integer totalRecords;
  /**
   * info.
   */
  @JsonProperty("resultInfo")
  @JsonPropertyDescription("info")
  private Object resultInfo;

  /**
   * configurations.
   * (Required)
   */
  @JsonProperty("configs")
  public List<Config> getConfigs() {
    return configs;
  }

  /**
   * configurations.
   * (Required)
   */
  @JsonProperty("configs")
  public void setConfigs(List<Config> configs) {
    this.configs = configs;
  }

  public Configurations withConfigs(List<Config> configs) {
    this.configs = configs;
    return this;
  }

  /**
   * total records.
   * (Required)
   */
  @JsonProperty("totalRecords")
  public Integer getTotalRecords() {
    return totalRecords;
  }

  /**
   * total records.
   * (Required)
   */
  @JsonProperty("totalRecords")
  public void setTotalRecords(Integer totalRecords) {
    this.totalRecords = totalRecords;
  }

  public Configurations withTotalRecords(Integer totalRecords) {
    this.totalRecords = totalRecords;
    return this;
  }

  /**
   * info.
   */
  @JsonProperty("resultInfo")
  public Object getResultInfo() {
    return resultInfo;
  }

  /**
   * info.
   */
  @JsonProperty("resultInfo")
  public void setResultInfo(Object resultInfo) {
    this.resultInfo = resultInfo;
  }

  public Configurations withResultInfo(Object resultInfo) {
    this.resultInfo = resultInfo;
    return this;
  }
}
