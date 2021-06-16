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
import io.swagger.client.model.GroupProfile;
import io.swagger.client.model.NodeRef;
import java.io.IOException;

/**
 * Group
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2020-03-20T14:32:44.166+01:00")
public class Group {
  @SerializedName("ref")
  private NodeRef ref = null;

  @SerializedName("editable")
  private Boolean editable = false;

  @SerializedName("authorityName")
  private String authorityName = null;

  /**
   * Gets or Sets authorityType
   */
  @JsonAdapter(AuthorityTypeEnum.Adapter.class)
  public enum AuthorityTypeEnum {
    USER("USER"),
    
    GROUP("GROUP"),
    
    OWNER("OWNER"),
    
    EVERYONE("EVERYONE"),
    
    GUEST("GUEST");

    private String value;

    AuthorityTypeEnum(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    public static AuthorityTypeEnum fromValue(String text) {
      for (AuthorityTypeEnum b : AuthorityTypeEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      return null;
    }

    public static class Adapter extends TypeAdapter<AuthorityTypeEnum> {
      @Override
      public void write(final JsonWriter jsonWriter, final AuthorityTypeEnum enumeration) throws IOException {
        jsonWriter.value(enumeration.getValue());
      }

      @Override
      public AuthorityTypeEnum read(final JsonReader jsonReader) throws IOException {
        String value = jsonReader.nextString();
        return AuthorityTypeEnum.fromValue(String.valueOf(value));
      }
    }
  }

  @SerializedName("authorityType")
  private AuthorityTypeEnum authorityType = null;

  @SerializedName("groupName")
  private String groupName = null;

  @SerializedName("profile")
  private GroupProfile profile = null;

  public Group ref(NodeRef ref) {
    this.ref = ref;
    return this;
  }

   /**
   * Get ref
   * @return ref
  **/
  @ApiModelProperty(value = "")
  public NodeRef getRef() {
    return ref;
  }

  public void setRef(NodeRef ref) {
    this.ref = ref;
  }

  public Group editable(Boolean editable) {
    this.editable = editable;
    return this;
  }

   /**
   * Get editable
   * @return editable
  **/
  @ApiModelProperty(value = "")
  public Boolean isEditable() {
    return editable;
  }

  public void setEditable(Boolean editable) {
    this.editable = editable;
  }

  public Group authorityName(String authorityName) {
    this.authorityName = authorityName;
    return this;
  }

   /**
   * Get authorityName
   * @return authorityName
  **/
  @ApiModelProperty(required = true, value = "")
  public String getAuthorityName() {
    return authorityName;
  }

  public void setAuthorityName(String authorityName) {
    this.authorityName = authorityName;
  }

  public Group authorityType(AuthorityTypeEnum authorityType) {
    this.authorityType = authorityType;
    return this;
  }

   /**
   * Get authorityType
   * @return authorityType
  **/
  @ApiModelProperty(value = "")
  public AuthorityTypeEnum getAuthorityType() {
    return authorityType;
  }

  public void setAuthorityType(AuthorityTypeEnum authorityType) {
    this.authorityType = authorityType;
  }

  public Group groupName(String groupName) {
    this.groupName = groupName;
    return this;
  }

   /**
   * Get groupName
   * @return groupName
  **/
  @ApiModelProperty(value = "")
  public String getGroupName() {
    return groupName;
  }

  public void setGroupName(String groupName) {
    this.groupName = groupName;
  }

  public Group profile(GroupProfile profile) {
    this.profile = profile;
    return this;
  }

   /**
   * Get profile
   * @return profile
  **/
  @ApiModelProperty(value = "")
  public GroupProfile getProfile() {
    return profile;
  }

  public void setProfile(GroupProfile profile) {
    this.profile = profile;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Group group = (Group) o;
    return Objects.equals(this.ref, group.ref) &&
        Objects.equals(this.editable, group.editable) &&
        Objects.equals(this.authorityName, group.authorityName) &&
        Objects.equals(this.authorityType, group.authorityType) &&
        Objects.equals(this.groupName, group.groupName) &&
        Objects.equals(this.profile, group.profile);
  }

  @Override
  public int hashCode() {
    return Objects.hash(ref, editable, authorityName, authorityType, groupName, profile);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Group {\n");
    
    sb.append("    ref: ").append(toIndentedString(ref)).append("\n");
    sb.append("    editable: ").append(toIndentedString(editable)).append("\n");
    sb.append("    authorityName: ").append(toIndentedString(authorityName)).append("\n");
    sb.append("    authorityType: ").append(toIndentedString(authorityType)).append("\n");
    sb.append("    groupName: ").append(toIndentedString(groupName)).append("\n");
    sb.append("    profile: ").append(toIndentedString(profile)).append("\n");
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
