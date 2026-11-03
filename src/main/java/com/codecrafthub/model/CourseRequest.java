package com.codecrafthub.model;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.OptBoolean;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
// Requests contain editable fields only; the server owns id and created_at.
public record CourseRequest(
    @NotBlank String name,
    @NotBlank String description,
    @NotNull @JsonProperty("target_date") @JsonFormat(pattern = "uuuu-MM-dd", lenient = OptBoolean.FALSE) LocalDate targetDate,
    @NotNull CourseStatus status
) {}
