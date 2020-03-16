package org.edu_sharing.restservices.tracking.v1.model;

import org.edu_sharing.restservices.shared.Group;
import org.edu_sharing.restservices.shared.Organization;
import org.edu_sharing.service.tracking.TrackingService;
import org.edu_sharing.service.tracking.model.StatisticEntry;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class Tracking {
    private final Map<TrackingService.EventType, Integer> counts;
    private final String date;
    private final Authority authority;
    private final Map<String, Serializable> fields;
    private final Map<String, Map<String, Long>> groups;

    public Tracking(String date, Authority authority, Map<TrackingService.EventType,Integer> counts, Map<String,Serializable> fields,Map<String,Map<String,Long>> groups){
        this.date=date;
        this.authority=authority;
        this.counts=counts;
        this.fields=fields;
        this.groups=groups;

    }

    public Map<TrackingService.EventType, Integer> getCounts() {
        return counts;
    }

    public String getDate() {
        return date;
    }

    public Map<String, Serializable> getFields() {
        return fields;
    }

    public Map<String, Map<String, Long>> getGroups() {
        return groups;
    }

    public Authority getAuthority() {
        return authority;
    }

    public static class Authority {
        private String hash;
        private List<Organization> organization;
        private List<Group> mediacenter;

        public String getHash() {
            return hash;
        }

        public void setHash(String hash) {
            this.hash = hash;
        }

        public List<Organization> getOrganization() {
            return organization;
        }

        public void setOrganization(List<Organization> organization) {
            this.organization = organization;
        }

        public List<Group> getMediacenter() {
            return mediacenter;
        }

        public void setMediacenter(List<Group> mediacenter) {
            this.mediacenter = mediacenter;
        }
    }
}