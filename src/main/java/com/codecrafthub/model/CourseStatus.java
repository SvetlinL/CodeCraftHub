package com.codecrafthub.model;
import com.fasterxml.jackson.annotation.JsonValue;
public enum CourseStatus {
    NOT_STARTED("Not Started"), IN_PROGRESS("In Progress"), COMPLETED("Completed");
    private final String label;
    CourseStatus(String label) { this.label = label; }
    @JsonValue public String getLabel() { return label; }
}
