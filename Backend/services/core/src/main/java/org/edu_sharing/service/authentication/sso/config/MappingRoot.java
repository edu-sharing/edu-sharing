package org.edu_sharing.service.authentication.sso.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MappingRoot {

	HashMap<String,String> personMapping;
	Condition personMappingCondition;
	
	List<MappingGroup> groupMappings = new ArrayList<MappingGroup>();
	
	public MappingRoot() {
	}
	
	public void setGroupMapping(List<MappingGroup> groupMapping) {
		this.groupMappings = groupMapping;
	}
	
	public void setPersonMapping(HashMap<String, String> personMapping) {
		this.personMapping = personMapping;
	}
	
	public List<MappingGroup> getGroupMapping() {
		return groupMappings;
	}
	
	public HashMap<String, String> getPersonMapping() {
		return personMapping;
	}
	
	public List<String> getSSOGroupAttributes(){
		List<String> ssoGroupAttributes = new ArrayList<String>();
		for(MappingGroup groupMapping : groupMappings){
			getSSOGroupAttributes(ssoGroupAttributes,groupMapping.getCondition());
		}
		return ssoGroupAttributes;
	}
	
	public List<String> getSSOPersonAttributes(){
		return new ArrayList<String>(personMapping.keySet());
	}
	
	public List<String> getAllSSOAttributes(){
		List<String> result = new ArrayList<String>();
		result.addAll(getSSOPersonAttributes());
		result.addAll(getSSOGroupAttributes());
		return result;
	}
	
	private void getSSOGroupAttributes(List<String> result, Condition c){
		if(c instanceof ConditionBlock){
			for(Condition subc : ((ConditionBlock)c).getConditions()){
				getSSOGroupAttributes(result,subc);
			}
		}else if(c instanceof ConditionSimple){
			result.add(((ConditionSimple)c).getAttribute());
		}
	}
	
	public void setPersonMappingCondition(Condition personMappingCondition) {
		this.personMappingCondition = personMappingCondition;
	}
	
	public Condition getPersonMappingCondition() {
		return personMappingCondition;
	}
}
