package org.grnet.status.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class LatestDataResponse {

    private Status status;
    private MetricDataResponse data;

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public MetricDataResponse getData() {
        return data;
    }

    public void setData(MetricDataResponse data) {
        this.data = data;
    }

    public static class Status {

        private String message;
        private String code;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }
    }

    public static class MetricDataResponse {

        @JsonProperty("metric_data")
        private List<MetricData> metricData;

        public List<MetricData> getMetricData() {
            return metricData;
        }

        public void setMetricData(List<MetricData> metricData) {
            this.metricData = metricData;
        }
    }

    public static class MetricData {

        @JsonProperty("endpoint_group")
        private String endpointGroup;

        private String service;
        private String endpoint;
        private String metric;
        private String timestamp;
        private String status;
        private String summary;
        private String message;

        public String getEndpointGroup() {
            return endpointGroup;
        }

        public void setEndpointGroup(String endpointGroup) {
            this.endpointGroup = endpointGroup;
        }

        public String getService() {
            return service;
        }

        public void setService(String service) {
            this.service = service;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getMetric() {
            return metric;
        }

        public void setMetric(String metric) {
            this.metric = metric;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getSummary() {
            return summary;
        }

        public void setSummary(String summary) {
            this.summary = summary;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
