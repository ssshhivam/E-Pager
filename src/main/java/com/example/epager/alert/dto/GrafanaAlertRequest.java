package com.example.epager.alert.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GrafanaAlertRequest {

    private String status;
    private String title;
    private String message;
    private String externalURL;
    private Map<String, String> commonLabels;
    private Map<String, String> commonAnnotations;
    private List<GrafanaAlertItem> alerts;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getExternalURL() {
        return externalURL;
    }

    public void setExternalURL(String externalURL) {
        this.externalURL = externalURL;
    }

    public Map<String, String> getCommonLabels() {
        return commonLabels;
    }

    public void setCommonLabels(Map<String, String> commonLabels) {
        this.commonLabels = commonLabels;
    }

    public Map<String, String> getCommonAnnotations() {
        return commonAnnotations;
    }

    public void setCommonAnnotations(Map<String, String> commonAnnotations) {
        this.commonAnnotations = commonAnnotations;
    }

    public List<GrafanaAlertItem> getAlerts() {
        return alerts;
    }

    public void setAlerts(List<GrafanaAlertItem> alerts) {
        this.alerts = alerts;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GrafanaAlertItem {
        private String fingerprint;
        private Map<String, String> labels;
        private Map<String, String> annotations;

        public String getFingerprint() {
            return fingerprint;
        }

        public void setFingerprint(String fingerprint) {
            this.fingerprint = fingerprint;
        }

        public Map<String, String> getLabels() {
            return labels;
        }

        public void setLabels(Map<String, String> labels) {
            this.labels = labels;
        }

        public Map<String, String> getAnnotations() {
            return annotations;
        }

        public void setAnnotations(Map<String, String> annotations) {
            this.annotations = annotations;
        }
    }
}
