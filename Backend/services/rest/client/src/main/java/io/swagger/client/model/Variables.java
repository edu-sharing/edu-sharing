/*
 * edu-sharing Repository REST API
 * The public restful API of the edu-sharing repository.
 *
 * OpenAPI spec version: 1.1
 * 
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */


package io.swagger.client.model;

import java.util.Objects;
import java.util.Arrays;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Variables
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2020-03-20T14:32:44.166+01:00")
public class Variables {
  @SerializedName("global")
  private Map<String, String> global = null;

  @SerializedName("current")
  private Map<String, String> current = null;

  public Variables global(Map<String, String> global) {
    this.global = global;
    return this;
  }

  public Variables putGlobalItem(String key, String globalItem) {
    if (this.global == null) {
      this.global = new HashMap<String, String>();
    }
    this.global.put(key, globalItem);
    return this;
  }

   /**
   * Get global
   * @return global
  **/
  @ApiModelProperty(value = "")
  public Map<String, String> getGlobal() {
    return global;
  }

  public void setGlobal(Map<String, String> global) {
    this.global = global;
  }

  public Variables current(Map<String, String> current) {
    this.current = current;
    return this;
  }

  public Variables putCurrentItem(String key, String currentItem) {
    if (this.current == null) {
      this.current = new HashMap<String, String>();
    }
    this.current.put(key, currentItem);
    return this;
  }

   /**
   * Get current
   * @return current
  **/
  @ApiModelProperty(value = "")
  public Map<String, String> getCurrent() {
    return current;
  }

  public void setCurrent(Map<String, String> current) {
    this.current = current;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Variables variables = (Variables) o;
    return Objects.equals(this.global, variables.global) &&
        Objects.equals(this.current, variables.current);
  }

  @Override
  public int hashCode() {
    return Objects.hash(global, current);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Variables {\n");
    
    sb.append("    global: ").append(toIndentedString(global)).append("\n");
    sb.append("    current: ").append(toIndentedString(current)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }

}
