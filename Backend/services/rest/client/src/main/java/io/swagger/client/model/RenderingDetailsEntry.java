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
 * RenderingDetailsEntry
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2020-03-20T14:32:44.166+01:00")
public class RenderingDetailsEntry {
  @SerializedName("detailsSnippet")
  private String detailsSnippet = null;

  @SerializedName("mimeType")
  private String mimeType = null;

  @SerializedName("node")
  private Node node = null;

  public RenderingDetailsEntry detailsSnippet(String detailsSnippet) {
    this.detailsSnippet = detailsSnippet;
    return this;
  }

   /**
   * Get detailsSnippet
   * @return detailsSnippet
  **/
  @ApiModelProperty(required = true, value = "")
  public String getDetailsSnippet() {
    return detailsSnippet;
  }

  public void setDetailsSnippet(String detailsSnippet) {
    this.detailsSnippet = detailsSnippet;
  }

  public RenderingDetailsEntry mimeType(String mimeType) {
    this.mimeType = mimeType;
    return this;
  }

   /**
   * Get mimeType
   * @return mimeType
  **/
  @ApiModelProperty(required = true, value = "")
  public String getMimeType() {
    return mimeType;
  }

  public void setMimeType(String mimeType) {
    this.mimeType = mimeType;
  }

  public RenderingDetailsEntry node(Node node) {
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


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RenderingDetailsEntry renderingDetailsEntry = (RenderingDetailsEntry) o;
    return Objects.equals(this.detailsSnippet, renderingDetailsEntry.detailsSnippet) &&
        Objects.equals(this.mimeType, renderingDetailsEntry.mimeType) &&
        Objects.equals(this.node, renderingDetailsEntry.node);
  }

  @Override
  public int hashCode() {
    return Objects.hash(detailsSnippet, mimeType, node);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RenderingDetailsEntry {\n");
    
    sb.append("    detailsSnippet: ").append(toIndentedString(detailsSnippet)).append("\n");
    sb.append("    mimeType: ").append(toIndentedString(mimeType)).append("\n");
    sb.append("    node: ").append(toIndentedString(node)).append("\n");
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

