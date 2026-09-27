package com.devpilot.controller;

import com.devpilot.dto.AnalyzeIssueRequest;
import com.devpilot.dto.AnalyzeIssueResponse;
import com.devpilot.dto.GitHubIssueDetails;
import com.devpilot.exception.AgentException;
import com.devpilot.exception.GitHubException;
import com.devpilot.exception.GlobalExceptionHandler;
import com.devpilot.service.DevPilotAgentService;
import com.devpilot.service.GitHubService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AgentController.class)
@Import(GlobalExceptionHandler.class)
class AgentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GitHubService gitHubService;

    @MockBean
    private DevPilotAgentService agentService;

    @Test
    void analyzeGitHubIssueShouldReturnSuccessfulEngineeringAnalysis() throws Exception {
        AnalyzeIssueRequest request = AnalyzeIssueRequest.builder()
                .owner("spring-projects")
                .repository("spring-boot")
                .issueNumber(40000L)
                .build();

        GitHubIssueDetails issueDetails = GitHubIssueDetails.builder()
                .number(40000L)
                .title("Support virtual threads in Netty")
                .body("Feature request to support Java 21 virtual threads")
                .state("open")
                .htmlUrl("https://github.com/spring-projects/spring-boot/issues/40000")
                .labels(List.of("theme: virtual-threads"))
                .build();

        AnalyzeIssueResponse expectedResponse = AnalyzeIssueResponse.builder()
                .success(true)
                .summary("Add support for virtual threads in Netty-based Spring Boot starter")
                .rootCause("Netty web server configuration does not currently map executor to virtual threads")
                .implementationPlan(List.of("Update NettyWebServerFactory", "Add configuration property"))
                .potentialFiles(List.of("NettyWebServerFactoryCustomizer.java"))
                .testingStrategy(List.of("Unit test with VirtualThreadExecutor", "Integration test with WebTestClient"))
                .nextSteps(List.of("Benchmark throughput with Loom", "Submit pull request"))
                .build();

        when(gitHubService.getIssue(eq("spring-projects"), eq("spring-boot"), eq(40000L)))
                .thenReturn(issueDetails);
        when(agentService.analyzeIssue(any(GitHubIssueDetails.class)))
                .thenReturn(expectedResponse);

        mockMvc.perform(post("/api/agent/analyze-github-issue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.summary").value("Add support for virtual threads in Netty-based Spring Boot starter"))
                .andExpect(jsonPath("$.rootCause").isNotEmpty())
                .andExpect(jsonPath("$.implementationPlan[0]").value("Update NettyWebServerFactory"))
                .andExpect(jsonPath("$.potentialFiles[0]").value("NettyWebServerFactoryCustomizer.java"))
                .andExpect(jsonPath("$.testingStrategy").isArray())
                .andExpect(jsonPath("$.nextSteps").isArray());
    }

    @Test
    void analyzeWithEmptyOwnerOrRepoShouldReturnValidationBadRequest() throws Exception {
        AnalyzeIssueRequest invalidRequest = AnalyzeIssueRequest.builder()
                .owner("")
                .repository("")
                .issueNumber(12L)
                .build();

        mockMvc.perform(post("/api/agent/analyze-github-issue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    @Test
    void analyzeWhenIssueNotFoundShouldReturnGitHubNotFoundError() throws Exception {
        AnalyzeIssueRequest request = AnalyzeIssueRequest.builder()
                .owner("unknown-org")
                .repository("non-existent-repo")
                .issueNumber(999999L)
                .build();

        when(gitHubService.getIssue(eq("unknown-org"), eq("non-existent-repo"), eq(999999L)))
                .thenThrow(new GitHubException("GitHub issue #999999 not found in repository 'unknown-org/non-existent-repo'", HttpStatus.NOT_FOUND));

        mockMvc.perform(post("/api/agent/analyze-github-issue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("GITHUB_ERROR"))
                .andExpect(jsonPath("$.message").value("GitHub issue #999999 not found in repository 'unknown-org/non-existent-repo'"))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    @Test
    void analyzeWhenOpenAiFailsShouldReturnAgentGatewayError() throws Exception {
        AnalyzeIssueRequest request = AnalyzeIssueRequest.builder()
                .owner("spring-projects")
                .repository("spring-boot")
                .issueNumber(10L)
                .build();

        GitHubIssueDetails details = GitHubIssueDetails.builder()
                .number(10L)
                .title("Sample issue")
                .build();

        when(gitHubService.getIssue(any(), any(), any())).thenReturn(details);
        when(agentService.analyzeIssue(any()))
                .thenThrow(new AgentException("OpenAI API rate limit exceeded", HttpStatus.BAD_GATEWAY));

        mockMvc.perform(post("/api/agent/analyze-github-issue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("AI_AGENT_ERROR"))
                .andExpect(jsonPath("$.message").value("OpenAI API rate limit exceeded"))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }
}
