package org.grnet.status.enums;

public enum LatestDataStatusFilter {
    OK("ok"),
    NON_OK("non-ok"),
    CRITICAL("critical"),
    WARNING("warning"),
    UNKNOWN("unknown"),
    MISSING("missing"),
    ALL("all");

    private final String value;

    LatestDataStatusFilter(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}