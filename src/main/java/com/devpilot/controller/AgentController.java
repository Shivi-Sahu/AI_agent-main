package com.devpilot.controller;

import com.devpilot.dto.AnalyzeIssueRequest;
import com.devpilot.dto.AnalyzeIssueResponse;
import com.devpilot.dto.GitHubIssueDetails;
import com.devpilot.service.DevPilotAgentService;
import com.devpilot.service.GitHubService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {

    private final GitHubService gitHubService;
    private final DevPilotAgentService agentService;

    @PostMapping("/analyze-github-issue")
    public ResponseEntity<AnalyzeIssueResponse> analyzeGitHubIssue(@Valid @RequestBody AnalyzeIssueRequest request) {
        log.info("Received request to analyze issue: owner='{}', repo='{}', issueNumber={}",
                request.getOwner(), request.getRepository(), request.getIssueNumber());

        // 1. Fetch GitHub issue and comments
        GitHubIssueDetails issueDetails = gitHubService.getIssue(
                request.getOwner().trim(),
                request.getRepository().trim(),
                request.getIssueNumber()
        );

        // 2. Perform AI Software Engineer analysis via Spring AI
        AnalyzeIssueResponse response = agentService.analyzeIssue(issueDetails);

        return ResponseEntity.ok(response);
    }
}
