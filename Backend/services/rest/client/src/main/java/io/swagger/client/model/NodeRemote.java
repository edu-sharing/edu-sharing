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
import io.swagger.client.model.Node;
import java.io.IOException;

/**
 * NodeRemote
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2020-03-20T14:32:44.166+01:00")
public class NodeRemote {
  @SerializedName("node")
  private Node node = null;

  @SerializedName("remote")
  private Node remote = null;

  public NodeRemote node(Node node) {
    this.node = node;
    return this;
  }

   /**
   * Get node
   * @return node
  **/
  @ApiModelProperty(required = true, value = "")
  public Node getNode() {
    return node;
  }

  public void setNode(Node node) {
    this.node = node;
  }

  public NodeRemote remote(Node remote) {
    this.remote = remote;
    return this;
  }

   /**
   * Get remote
   * @return remote
  **/
  @ApiModelProperty(required = true, value = "")
  public Node getRemote() {
    return remote;
  }

  public void setRemote(Node remote) {
    this.remote = remote;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    NodeRemote nodeRemote = (NodeRemote) o;
    return Objects.equals(this.node, nodeRemote.node) &&
        Objects.equals(this.remote, nodeRemote.remote);
  }

  @Override
  public int hashCode() {
    return Objects.hash(node, remote);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class NodeRemote {\n");
    
    sb.append("    node: ").append(toIndentedString(node)).append("\n");
    sb.append("    remote: ").append(toIndentedString(remote)).append("\n");
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

