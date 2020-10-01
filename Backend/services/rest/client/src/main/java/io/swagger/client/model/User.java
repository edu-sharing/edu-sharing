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
import io.swagger.client.model.UserProfile;
import io.swagger.client.model.UserQuota;
import io.swagger.client.model.UserStats;
import io.swagger.client.model.UserStatus;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2020-03-20T14:32:44.166+01:00")
public class User {
  @SerializedName("status")
  private UserStatus status = null;

  @SerializedName("quota")
  private UserQuota quota = null;

  @SerializedName("properties")
  private Map<String, List<String>> properties = null;

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

  @SerializedName("userName")
  private String userName = null;

  @SerializedName("profile")
  private UserProfile profile = null;

  @SerializedName("stats")
  private UserStats stats = null;

  @SerializedName("homeFolder")
  private NodeRef homeFolder = null;

  @SerializedName("sharedFolders")
  private List<NodeRef> sharedFolders = null;

  public User status(UserStatus status) {
    this.status = status;
    return this;
  }

   /**
   * Get status
   * @return status
  **/
  @ApiModelProperty(value = "")
  public UserStatus getStatus() {
    return status;
  }

  public void setStatus(UserStatus status) {
    this.status = status;
  }

  public User quota(UserQuota quota) {
    this.quota = quota;
    return this;
  }

   /**
   * Get quota
   * @return quota
  **/
  @ApiModelProperty(value = "")
  public UserQuota getQuota() {
    return quota;
  }

  public void setQuota(UserQuota quota) {
    this.quota = quota;
  }

  public User properties(Map<String, List<String>> properties) {
    this.properties = properties;
    return this;
  }

  public User putPropertiesItem(String key, List<String> propertiesItem) {
    if (this.properties == null) {
      this.properties = new HashMap<String, List<String>>();
    }
    this.properties.put(key, propertiesItem);
    return this;
  }

   /**
   * Get properties
   * @return properties
  **/
  @ApiModelProperty(value = "")
  public Map<String, List<String>> getProperties() {
    return properties;
  }

  public void setProperties(Map<String, List<String>> properties) {
    this.properties = properties;
  }

  public User authorityName(String authorityName) {
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

  public User authorityType(AuthorityTypeEnum authorityType) {
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

  public User userName(String userName) {
    this.userName = userName;
    return this;
  }

   /**
   * Get userName
   * @return userName
  **/
  @ApiModelProperty(value = "")
  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public User profile(UserProfile profile) {
    this.profile = profile;
    return this;
  }

   /**
   * Get profile
   * @return profile
  **/
  @ApiModelProperty(value = "")
  public UserProfile getProfile() {
    return profile;
  }

  public void setProfile(UserProfile profile) {
    this.profile = profile;
  }

  public User stats(UserStats stats) {
    this.stats = stats;
    return this;
  }

   /**
   * Get stats
   * @return stats
  **/
  @ApiModelProperty(value = "")
  public UserStats getStats() {
    return stats;
  }

  public void setStats(UserStats stats) {
    this.stats = stats;
  }

  public User homeFolder(NodeRef homeFolder) {
    this.homeFolder = homeFolder;
    return this;
  }

   /**
   * Get homeFolder
   * @return homeFolder
  **/
  @ApiModelProperty(required = true, value = "")
  public NodeRef getHomeFolder() {
    return homeFolder;
  }

  public void setHomeFolder(NodeRef homeFolder) {
    this.homeFolder = homeFolder;
  }

  public User sharedFolders(List<NodeRef> sharedFolders) {
    this.sharedFolders = sharedFolders;
    return this;
  }

  public User addSharedFoldersItem(NodeRef sharedFoldersItem) {
    if (this.sharedFolders == null) {
      this.sharedFolders = new ArrayList<NodeRef>();
    }
    this.sharedFolders.add(sharedFoldersItem);
    return this;
  }

   /**
   * Get sharedFolders
   * @return sharedFolders
  **/
  @ApiModelProperty(value = "")
  public List<NodeRef> getSharedFolders() {
    return sharedFolders;
  }

  public void setSharedFolders(List<NodeRef> sharedFolders) {
    this.sharedFolders = sharedFolders;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    User user = (User) o;
    return Objects.equals(this.status, user.status) &&
        Objects.equals(this.quota, user.quota) &&
        Objects.equals(this.properties, user.properties) &&
        Objects.equals(this.authorityName, user.authorityName) &&
        Objects.equals(this.authorityType, user.authorityType) &&
        Objects.equals(this.userName, user.userName) &&
        Objects.equals(this.profile, user.profile) &&
        Objects.equals(this.stats, user.stats) &&
        Objects.equals(this.homeFolder, user.homeFolder) &&
        Objects.equals(this.sharedFolders, user.sharedFolders);
  }

  @Override
  public int hashCode() {
    return Objects.hash(status, quota, properties, authorityName, authorityType, userName, profile, stats, homeFolder, sharedFolders);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class User {\n");
    
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    quota: ").append(toIndentedString(quota)).append("\n");
    sb.append("    properties: ").append(toIndentedString(properties)).append("\n");
    sb.append("    authorityName: ").append(toIndentedString(authorityName)).append("\n");
    sb.append("    authorityType: ").append(toIndentedString(authorityType)).append("\n");
    sb.append("    userName: ").append(toIndentedString(userName)).append("\n");
    sb.append("    profile: ").append(toIndentedString(profile)).append("\n");
    sb.append("    stats: ").append(toIndentedString(stats)).append("\n");
    sb.append("    homeFolder: ").append(toIndentedString(homeFolder)).append("\n");
    sb.append("    sharedFolders: ").append(toIndentedString(sharedFolders)).append("\n");
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

