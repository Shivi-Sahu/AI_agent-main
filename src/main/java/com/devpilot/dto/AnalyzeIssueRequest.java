package com.devpilot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyzeIssueRequest {

    @NotBlank(message = "GitHub owner is required")
    private String owner;

    @NotBlank(message = "GitHub repository is required")
    private String repository;

    @NotNull(message = "Issue number is required")
    @Positive(message = "Issue number must be a positive integer")
    private Long issueNumber;
}
