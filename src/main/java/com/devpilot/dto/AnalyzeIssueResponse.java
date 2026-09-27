package com.devpilot.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnalyzeIssueResponse {

    @Builder.Default
    private boolean success = true;

    private String summary;

    private String rootCause;

    @Builder.Default
    private List<String> implementationPlan = new ArrayList<>();

    @Builder.Default
    private List<String> potentialFiles = new ArrayList<>();

    @Builder.Default
    private List<String> testingStrategy = new ArrayList<>();

    @Builder.Default
    private List<String> nextSteps = new ArrayList<>();

    // Additional helpful metadata for frontend/clients
    private String issueTitle;
    private String issueUrl;
    private String issueState;
    private List<String> labels;
}
