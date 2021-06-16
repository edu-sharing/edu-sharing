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
import io.swagger.client.model.MdsQueryPropertyParameter;
import io.swagger.client.model.MdsQueryPropertyValue;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * MdsQueryProperty
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2020-03-20T14:32:44.166+01:00")
public class MdsQueryProperty {
  @SerializedName("name")
  private String name = null;

  @SerializedName("label")
  private String label = null;

  @SerializedName("labelHint")
  private String labelHint = null;

  @SerializedName("formHeight")
  private String formHeight = null;

  @SerializedName("formLength")
  private String formLength = null;

  @SerializedName("widget")
  private String widget = null;

  @SerializedName("widgetTitle")
  private String widgetTitle = null;

  @SerializedName("copyFrom")
  private List<String> copyFrom = new ArrayList<String>();

  @SerializedName("parameters")
  private List<MdsQueryPropertyParameter> parameters = new ArrayList<MdsQueryPropertyParameter>();

  @SerializedName("values")
  private List<MdsQueryPropertyValue> values = new ArrayList<MdsQueryPropertyValue>();

  @SerializedName("defaultValues")
  private List<String> defaultValues = new ArrayList<String>();

  @SerializedName("multiple")
  private Boolean multiple = false;

  @SerializedName("placeHolder")
  private String placeHolder = null;

  @SerializedName("styleName")
  private String styleName = null;

  @SerializedName("styleNameLabel")
  private String styleNameLabel = null;

  @SerializedName("type")
  private String type = null;

  @SerializedName("validators")
  private List<String> validators = new ArrayList<String>();

  @SerializedName("statement")
  private String statement = null;

  @SerializedName("multipleJoin")
  private String multipleJoin = null;

  @SerializedName("toogle")
  private Boolean toogle = false;

  @SerializedName("initByGetParam")
  private String initByGetParam = null;

  public MdsQueryProperty name(String name) {
    this.name = name;
    return this;
  }

   /**
   * Get name
   * @return name
  **/
  @ApiModelProperty(required = true, value = "")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public MdsQueryProperty label(String label) {
    this.label = label;
    return this;
  }

   /**
   * Get label
   * @return label
  **/
  @ApiModelProperty(required = true, value = "")
  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public MdsQueryProperty labelHint(String labelHint) {
    this.labelHint = labelHint;
    return this;
  }

   /**
   * Get labelHint
   * @return labelHint
  **/
  @ApiModelProperty(required = true, value = "")
  public String getLabelHint() {
    return labelHint;
  }

  public void setLabelHint(String labelHint) {
    this.labelHint = labelHint;
  }

  public MdsQueryProperty formHeight(String formHeight) {
    this.formHeight = formHeight;
    return this;
  }

   /**
   * Get formHeight
   * @return formHeight
  **/
  @ApiModelProperty(required = true, value = "")
  public String getFormHeight() {
    return formHeight;
  }

  public void setFormHeight(String formHeight) {
    this.formHeight = formHeight;
  }

  public MdsQueryProperty formLength(String formLength) {
    this.formLength = formLength;
    return this;
  }

   /**
   * Get formLength
   * @return formLength
  **/
  @ApiModelProperty(required = true, value = "")
  public String getFormLength() {
    return formLength;
  }

  public void setFormLength(String formLength) {
    this.formLength = formLength;
  }

  public MdsQueryProperty widget(String widget) {
    this.widget = widget;
    return this;
  }

   /**
   * Get widget
   * @return widget
  **/
  @ApiModelProperty(required = true, value = "")
  public String getWidget() {
    return widget;
  }

  public void setWidget(String widget) {
    this.widget = widget;
  }

  public MdsQueryProperty widgetTitle(String widgetTitle) {
    this.widgetTitle = widgetTitle;
    return this;
  }

   /**
   * Get widgetTitle
   * @return widgetTitle
  **/
  @ApiModelProperty(required = true, value = "")
  public String getWidgetTitle() {
    return widgetTitle;
  }

  public void setWidgetTitle(String widgetTitle) {
    this.widgetTitle = widgetTitle;
  }

  public MdsQueryProperty copyFrom(List<String> copyFrom) {
    this.copyFrom = copyFrom;
    return this;
  }

  public MdsQueryProperty addCopyFromItem(String copyFromItem) {
    this.copyFrom.add(copyFromItem);
    return this;
  }

   /**
   * Get copyFrom
   * @return copyFrom
  **/
  @ApiModelProperty(required = true, value = "")
  public List<String> getCopyFrom() {
    return copyFrom;
  }

  public void setCopyFrom(List<String> copyFrom) {
    this.copyFrom = copyFrom;
  }

  public MdsQueryProperty parameters(List<MdsQueryPropertyParameter> parameters) {
    this.parameters = parameters;
    return this;
  }

  public MdsQueryProperty addParametersItem(MdsQueryPropertyParameter parametersItem) {
    this.parameters.add(parametersItem);
    return this;
  }

   /**
   * Get parameters
   * @return parameters
  **/
  @ApiModelProperty(required = true, value = "")
  public List<MdsQueryPropertyParameter> getParameters() {
    return parameters;
  }

  public void setParameters(List<MdsQueryPropertyParameter> parameters) {
    this.parameters = parameters;
  }

  public MdsQueryProperty values(List<MdsQueryPropertyValue> values) {
    this.values = values;
    return this;
  }

  public MdsQueryProperty addValuesItem(MdsQueryPropertyValue valuesItem) {
    this.values.add(valuesItem);
    return this;
  }

   /**
   * Get values
   * @return values
  **/
  @ApiModelProperty(required = true, value = "")
  public List<MdsQueryPropertyValue> getValues() {
    return values;
  }

  public void setValues(List<MdsQueryPropertyValue> values) {
    this.values = values;
  }

  public MdsQueryProperty defaultValues(List<String> defaultValues) {
    this.defaultValues = defaultValues;
    return this;
  }

  public MdsQueryProperty addDefaultValuesItem(String defaultValuesItem) {
    this.defaultValues.add(defaultValuesItem);
    return this;
  }

   /**
   * Get defaultValues
   * @return defaultValues
  **/
  @ApiModelProperty(required = true, value = "")
  public List<String> getDefaultValues() {
    return defaultValues;
  }

  public void setDefaultValues(List<String> defaultValues) {
    this.defaultValues = defaultValues;
  }

  public MdsQueryProperty multiple(Boolean multiple) {
    this.multiple = multiple;
    return this;
  }

   /**
   * Get multiple
   * @return multiple
  **/
  @ApiModelProperty(required = true, value = "")
  public Boolean isMultiple() {
    return multiple;
  }

  public void setMultiple(Boolean multiple) {
    this.multiple = multiple;
  }

  public MdsQueryProperty placeHolder(String placeHolder) {
    this.placeHolder = placeHolder;
    return this;
  }

   /**
   * Get placeHolder
   * @return placeHolder
  **/
  @ApiModelProperty(required = true, value = "")
  public String getPlaceHolder() {
    return placeHolder;
  }

  public void setPlaceHolder(String placeHolder) {
    this.placeHolder = placeHolder;
  }

  public MdsQueryProperty styleName(String styleName) {
    this.styleName = styleName;
    return this;
  }

   /**
   * Get styleName
   * @return styleName
  **/
  @ApiModelProperty(required = true, value = "")
  public String getStyleName() {
    return styleName;
  }

  public void setStyleName(String styleName) {
    this.styleName = styleName;
  }

  public MdsQueryProperty styleNameLabel(String styleNameLabel) {
    this.styleNameLabel = styleNameLabel;
    return this;
  }

   /**
   * Get styleNameLabel
   * @return styleNameLabel
  **/
  @ApiModelProperty(required = true, value = "")
  public String getStyleNameLabel() {
    return styleNameLabel;
  }

  public void setStyleNameLabel(String styleNameLabel) {
    this.styleNameLabel = styleNameLabel;
  }

  public MdsQueryProperty type(String type) {
    this.type = type;
    return this;
  }

   /**
   * Get type
   * @return type
  **/
  @ApiModelProperty(required = true, value = "")
  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public MdsQueryProperty validators(List<String> validators) {
    this.validators = validators;
    return this;
  }

  public MdsQueryProperty addValidatorsItem(String validatorsItem) {
    this.validators.add(validatorsItem);
    return this;
  }

   /**
   * Get validators
   * @return validators
  **/
  @ApiModelProperty(required = true, value = "")
  public List<String> getValidators() {
    return validators;
  }

  public void setValidators(List<String> validators) {
    this.validators = validators;
  }

  public MdsQueryProperty statement(String statement) {
    this.statement = statement;
    return this;
  }

   /**
   * Get statement
   * @return statement
  **/
  @ApiModelProperty(required = true, value = "")
  public String getStatement() {
    return statement;
  }

  public void setStatement(String statement) {
    this.statement = statement;
  }

  public MdsQueryProperty multipleJoin(String multipleJoin) {
    this.multipleJoin = multipleJoin;
    return this;
  }

   /**
   * Get multipleJoin
   * @return multipleJoin
  **/
  @ApiModelProperty(required = true, value = "")
  public String getMultipleJoin() {
    return multipleJoin;
  }

  public void setMultipleJoin(String multipleJoin) {
    this.multipleJoin = multipleJoin;
  }

  public MdsQueryProperty toogle(Boolean toogle) {
    this.toogle = toogle;
    return this;
  }

   /**
   * Get toogle
   * @return toogle
  **/
  @ApiModelProperty(required = true, value = "")
  public Boolean isToogle() {
    return toogle;
  }

  public void setToogle(Boolean toogle) {
    this.toogle = toogle;
  }

  public MdsQueryProperty initByGetParam(String initByGetParam) {
    this.initByGetParam = initByGetParam;
    return this;
  }

   /**
   * Get initByGetParam
   * @return initByGetParam
  **/
  @ApiModelProperty(required = true, value = "")
  public String getInitByGetParam() {
    return initByGetParam;
  }

  public void setInitByGetParam(String initByGetParam) {
    this.initByGetParam = initByGetParam;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MdsQueryProperty mdsQueryProperty = (MdsQueryProperty) o;
    return Objects.equals(this.name, mdsQueryProperty.name) &&
        Objects.equals(this.label, mdsQueryProperty.label) &&
        Objects.equals(this.labelHint, mdsQueryProperty.labelHint) &&
        Objects.equals(this.formHeight, mdsQueryProperty.formHeight) &&
        Objects.equals(this.formLength, mdsQueryProperty.formLength) &&
        Objects.equals(this.widget, mdsQueryProperty.widget) &&
        Objects.equals(this.widgetTitle, mdsQueryProperty.widgetTitle) &&
        Objects.equals(this.copyFrom, mdsQueryProperty.copyFrom) &&
        Objects.equals(this.parameters, mdsQueryProperty.parameters) &&
        Objects.equals(this.values, mdsQueryProperty.values) &&
        Objects.equals(this.defaultValues, mdsQueryProperty.defaultValues) &&
        Objects.equals(this.multiple, mdsQueryProperty.multiple) &&
        Objects.equals(this.placeHolder, mdsQueryProperty.placeHolder) &&
        Objects.equals(this.styleName, mdsQueryProperty.styleName) &&
        Objects.equals(this.styleNameLabel, mdsQueryProperty.styleNameLabel) &&
        Objects.equals(this.type, mdsQueryProperty.type) &&
        Objects.equals(this.validators, mdsQueryProperty.validators) &&
        Objects.equals(this.statement, mdsQueryProperty.statement) &&
        Objects.equals(this.multipleJoin, mdsQueryProperty.multipleJoin) &&
        Objects.equals(this.toogle, mdsQueryProperty.toogle) &&
        Objects.equals(this.initByGetParam, mdsQueryProperty.initByGetParam);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, label, labelHint, formHeight, formLength, widget, widgetTitle, copyFrom, parameters, values, defaultValues, multiple, placeHolder, styleName, styleNameLabel, type, validators, statement, multipleJoin, toogle, initByGetParam);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MdsQueryProperty {\n");
    
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    label: ").append(toIndentedString(label)).append("\n");
    sb.append("    labelHint: ").append(toIndentedString(labelHint)).append("\n");
    sb.append("    formHeight: ").append(toIndentedString(formHeight)).append("\n");
    sb.append("    formLength: ").append(toIndentedString(formLength)).append("\n");
    sb.append("    widget: ").append(toIndentedString(widget)).append("\n");
    sb.append("    widgetTitle: ").append(toIndentedString(widgetTitle)).append("\n");
    sb.append("    copyFrom: ").append(toIndentedString(copyFrom)).append("\n");
    sb.append("    parameters: ").append(toIndentedString(parameters)).append("\n");
    sb.append("    values: ").append(toIndentedString(values)).append("\n");
    sb.append("    defaultValues: ").append(toIndentedString(defaultValues)).append("\n");
    sb.append("    multiple: ").append(toIndentedString(multiple)).append("\n");
    sb.append("    placeHolder: ").append(toIndentedString(placeHolder)).append("\n");
    sb.append("    styleName: ").append(toIndentedString(styleName)).append("\n");
    sb.append("    styleNameLabel: ").append(toIndentedString(styleNameLabel)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    validators: ").append(toIndentedString(validators)).append("\n");
    sb.append("    statement: ").append(toIndentedString(statement)).append("\n");
    sb.append("    multipleJoin: ").append(toIndentedString(multipleJoin)).append("\n");
    sb.append("    toogle: ").append(toIndentedString(toogle)).append("\n");
    sb.append("    initByGetParam: ").append(toIndentedString(initByGetParam)).append("\n");
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
