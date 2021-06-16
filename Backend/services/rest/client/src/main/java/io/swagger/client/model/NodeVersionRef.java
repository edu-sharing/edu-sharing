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
import io.swagger.client.model.NodeRef;
import java.io.IOException;

/**
 * NodeVersionRef
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2020-03-20T14:32:44.166+01:00")
public class NodeVersionRef {
  @SerializedName("node")
  private NodeRef node = null;

  @SerializedName("major")
  private Integer major = null;

  @SerializedName("minor")
  private Integer minor = null;

  public NodeVersionRef node(NodeRef node) {
    this.node = node;
    return this;
  }

   /**
   * Get node
   * @return node
  **/
  @ApiModelProperty(required = true, value = "")
  public NodeRef getNode() {
    return node;
  }

  public void setNode(NodeRef node) {
    this.node = node;
  }

  public NodeVersionRef major(Integer major) {
    this.major = major;
    return this;
  }

   /**
   * Get major
   * @return major
  **/
  @ApiModelProperty(required = true, value = "")
  public Integer getMajor() {
    return major;
  }

  public void setMajor(Integer major) {
    this.major = major;
  }

  public NodeVersionRef minor(Integer minor) {
    this.minor = minor;
    return this;
  }

   /**
   * Get minor
   * @return minor
  **/
  @ApiModelProperty(required = true, value = "")
  public Integer getMinor() {
    return minor;
  }

  public void setMinor(Integer minor) {
    this.minor = minor;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    NodeVersionRef nodeVersionRef = (NodeVersionRef) o;
    return Objects.equals(this.node, nodeVersionRef.node) &&
        Objects.equals(this.major, nodeVersionRef.major) &&
        Objects.equals(this.minor, nodeVersionRef.minor);
  }

  @Override
  public int hashCode() {
    return Objects.hash(node, major, minor);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class NodeVersionRef {\n");
    
    sb.append("    node: ").append(toIndentedString(node)).append("\n");
    sb.append("    major: ").append(toIndentedString(major)).append("\n");
    sb.append("    minor: ").append(toIndentedString(minor)).append("\n");
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
