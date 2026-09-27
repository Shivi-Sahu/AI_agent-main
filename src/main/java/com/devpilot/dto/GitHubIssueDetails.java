package com.devpilot.dto;

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
public class GitHubIssueDetails {

    private Long number;
    private String title;
    private String body;
    private String state;
    private String htmlUrl;

    @Builder.Default
    private List<String> labels = new ArrayList<>();

    @Builder.Default
    private List<String> comments = new ArrayList<>();
}
