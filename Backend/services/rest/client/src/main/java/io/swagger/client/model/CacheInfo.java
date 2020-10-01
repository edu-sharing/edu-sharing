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

/**
 * CacheInfo
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2020-03-20T14:32:44.166+01:00")
public class CacheInfo {
  @SerializedName("size")
  private Integer size = null;

  @SerializedName("statisticHits")
  private Long statisticHits = null;

  @SerializedName("name")
  private String name = null;

  @SerializedName("backupCount")
  private Integer backupCount = null;

  @SerializedName("backupEntryCount")
  private Long backupEntryCount = null;

  @SerializedName("backupEntryMemoryCost")
  private Long backupEntryMemoryCost = null;

  @SerializedName("heapCost")
  private Long heapCost = null;

  @SerializedName("ownedEntryCount")
  private Long ownedEntryCount = null;

  @SerializedName("getOwnedEntryMemoryCost")
  private Long getOwnedEntryMemoryCost = null;

  @SerializedName("sizeInMemory")
  private Long sizeInMemory = null;

  @SerializedName("member")
  private String member = null;

  @SerializedName("groupName")
  private String groupName = null;

  @SerializedName("maxSize")
  private Integer maxSize = null;

  public CacheInfo size(Integer size) {
    this.size = size;
    return this;
  }

   /**
   * Get size
   * @return size
  **/
  @ApiModelProperty(value = "")
  public Integer getSize() {
    return size;
  }

  public void setSize(Integer size) {
    this.size = size;
  }

  public CacheInfo statisticHits(Long statisticHits) {
    this.statisticHits = statisticHits;
    return this;
  }

   /**
   * Get statisticHits
   * @return statisticHits
  **/
  @ApiModelProperty(value = "")
  public Long getStatisticHits() {
    return statisticHits;
  }

  public void setStatisticHits(Long statisticHits) {
    this.statisticHits = statisticHits;
  }

  public CacheInfo name(String name) {
    this.name = name;
    return this;
  }

   /**
   * Get name
   * @return name
  **/
  @ApiModelProperty(value = "")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public CacheInfo backupCount(Integer backupCount) {
    this.backupCount = backupCount;
    return this;
  }

   /**
   * Get backupCount
   * @return backupCount
  **/
  @ApiModelProperty(value = "")
  public Integer getBackupCount() {
    return backupCount;
  }

  public void setBackupCount(Integer backupCount) {
    this.backupCount = backupCount;
  }

  public CacheInfo backupEntryCount(Long backupEntryCount) {
    this.backupEntryCount = backupEntryCount;
    return this;
  }

   /**
   * Get backupEntryCount
   * @return backupEntryCount
  **/
  @ApiModelProperty(value = "")
  public Long getBackupEntryCount() {
    return backupEntryCount;
  }

  public void setBackupEntryCount(Long backupEntryCount) {
    this.backupEntryCount = backupEntryCount;
  }

  public CacheInfo backupEntryMemoryCost(Long backupEntryMemoryCost) {
    this.backupEntryMemoryCost = backupEntryMemoryCost;
    return this;
  }

   /**
   * Get backupEntryMemoryCost
   * @return backupEntryMemoryCost
  **/
  @ApiModelProperty(value = "")
  public Long getBackupEntryMemoryCost() {
    return backupEntryMemoryCost;
  }

  public void setBackupEntryMemoryCost(Long backupEntryMemoryCost) {
    this.backupEntryMemoryCost = backupEntryMemoryCost;
  }

  public CacheInfo heapCost(Long heapCost) {
    this.heapCost = heapCost;
    return this;
  }

   /**
   * Get heapCost
   * @return heapCost
  **/
  @ApiModelProperty(value = "")
  public Long getHeapCost() {
    return heapCost;
  }

  public void setHeapCost(Long heapCost) {
    this.heapCost = heapCost;
  }

  public CacheInfo ownedEntryCount(Long ownedEntryCount) {
    this.ownedEntryCount = ownedEntryCount;
    return this;
  }

   /**
   * Get ownedEntryCount
   * @return ownedEntryCount
  **/
  @ApiModelProperty(value = "")
  public Long getOwnedEntryCount() {
    return ownedEntryCount;
  }

  public void setOwnedEntryCount(Long ownedEntryCount) {
    this.ownedEntryCount = ownedEntryCount;
  }

  public CacheInfo getOwnedEntryMemoryCost(Long getOwnedEntryMemoryCost) {
    this.getOwnedEntryMemoryCost = getOwnedEntryMemoryCost;
    return this;
  }

   /**
   * Get getOwnedEntryMemoryCost
   * @return getOwnedEntryMemoryCost
  **/
  @ApiModelProperty(value = "")
  public Long getGetOwnedEntryMemoryCost() {
    return getOwnedEntryMemoryCost;
  }

  public void setGetOwnedEntryMemoryCost(Long getOwnedEntryMemoryCost) {
    this.getOwnedEntryMemoryCost = getOwnedEntryMemoryCost;
  }

  public CacheInfo sizeInMemory(Long sizeInMemory) {
    this.sizeInMemory = sizeInMemory;
    return this;
  }

   /**
   * Get sizeInMemory
   * @return sizeInMemory
  **/
  @ApiModelProperty(value = "")
  public Long getSizeInMemory() {
    return sizeInMemory;
  }

  public void setSizeInMemory(Long sizeInMemory) {
    this.sizeInMemory = sizeInMemory;
  }

  public CacheInfo member(String member) {
    this.member = member;
    return this;
  }

   /**
   * Get member
   * @return member
  **/
  @ApiModelProperty(value = "")
  public String getMember() {
    return member;
  }

  public void setMember(String member) {
    this.member = member;
  }

  public CacheInfo groupName(String groupName) {
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

  public CacheInfo maxSize(Integer maxSize) {
    this.maxSize = maxSize;
    return this;
  }

   /**
   * Get maxSize
   * @return maxSize
  **/
  @ApiModelProperty(value = "")
  public Integer getMaxSize() {
    return maxSize;
  }

  public void setMaxSize(Integer maxSize) {
    this.maxSize = maxSize;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CacheInfo cacheInfo = (CacheInfo) o;
    return Objects.equals(this.size, cacheInfo.size) &&
        Objects.equals(this.statisticHits, cacheInfo.statisticHits) &&
        Objects.equals(this.name, cacheInfo.name) &&
        Objects.equals(this.backupCount, cacheInfo.backupCount) &&
        Objects.equals(this.backupEntryCount, cacheInfo.backupEntryCount) &&
        Objects.equals(this.backupEntryMemoryCost, cacheInfo.backupEntryMemoryCost) &&
        Objects.equals(this.heapCost, cacheInfo.heapCost) &&
        Objects.equals(this.ownedEntryCount, cacheInfo.ownedEntryCount) &&
        Objects.equals(this.getOwnedEntryMemoryCost, cacheInfo.getOwnedEntryMemoryCost) &&
        Objects.equals(this.sizeInMemory, cacheInfo.sizeInMemory) &&
        Objects.equals(this.member, cacheInfo.member) &&
        Objects.equals(this.groupName, cacheInfo.groupName) &&
        Objects.equals(this.maxSize, cacheInfo.maxSize);
  }

  @Override
  public int hashCode() {
    return Objects.hash(size, statisticHits, name, backupCount, backupEntryCount, backupEntryMemoryCost, heapCost, ownedEntryCount, getOwnedEntryMemoryCost, sizeInMemory, member, groupName, maxSize);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CacheInfo {\n");
    
    sb.append("    size: ").append(toIndentedString(size)).append("\n");
    sb.append("    statisticHits: ").append(toIndentedString(statisticHits)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    backupCount: ").append(toIndentedString(backupCount)).append("\n");
    sb.append("    backupEntryCount: ").append(toIndentedString(backupEntryCount)).append("\n");
    sb.append("    backupEntryMemoryCost: ").append(toIndentedString(backupEntryMemoryCost)).append("\n");
    sb.append("    heapCost: ").append(toIndentedString(heapCost)).append("\n");
    sb.append("    ownedEntryCount: ").append(toIndentedString(ownedEntryCount)).append("\n");
    sb.append("    getOwnedEntryMemoryCost: ").append(toIndentedString(getOwnedEntryMemoryCost)).append("\n");
    sb.append("    sizeInMemory: ").append(toIndentedString(sizeInMemory)).append("\n");
    sb.append("    member: ").append(toIndentedString(member)).append("\n");
    sb.append("    groupName: ").append(toIndentedString(groupName)).append("\n");
    sb.append("    maxSize: ").append(toIndentedString(maxSize)).append("\n");
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

