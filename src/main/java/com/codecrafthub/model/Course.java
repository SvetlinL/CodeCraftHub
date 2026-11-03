package com.codecrafthub.model;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.time.LocalDateTime;
// A record is a compact Java data class. Jackson maps these components to JSON.
public record Course(
    Long id,
    String name,
    String description,
    @JsonProperty("target_date") @JsonFormat(pattern = "uuuu-MM-dd", lenient = com.fasterxml.jackson.annotation.OptBoolean.FALSE) LocalDate targetDate,
    CourseStatus status,
    @JsonProperty("created_at") @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createdAt
) {}
