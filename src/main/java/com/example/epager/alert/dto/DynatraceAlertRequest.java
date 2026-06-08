package com.example.epager.alert.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DynatraceAlertRequest {

    private String problemId;
    private String problemTitle;
    private String problemDetailsText;
    private String severityLevel;
    private String impactedEntity;
    private Map<String, String> tags;

    public String getProblemId() {
        return problemId;
    }

    public void setProblemId(String problemId) {
        this.problemId = problemId;
    }

    public String getProblemTitle() {
        return problemTitle;
    }

    public void setProblemTitle(String problemTitle) {
        this.problemTitle = problemTitle;
    }

    public String getProblemDetailsText() {
        return problemDetailsText;
    }

    public void setProblemDetailsText(String problemDetailsText) {
        this.problemDetailsText = problemDetailsText;
    }

    public String getSeverityLevel() {
        return severityLevel;
    }

    public void setSeverityLevel(String severityLevel) {
        this.severityLevel = severityLevel;
    }

    public String getImpactedEntity() {
        return impactedEntity;
    }

    public void setImpactedEntity(String impactedEntity) {
        this.impactedEntity = impactedEntity;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }
}
