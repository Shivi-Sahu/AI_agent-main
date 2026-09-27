package com.devpilot.service;

import com.devpilot.dto.AnalyzeIssueResponse;
import com.devpilot.dto.GitHubIssueDetails;
import com.devpilot.exception.AgentException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class DevPilotAgentService {

    private static final String SYSTEM_INSTRUCTION = """
            You are DevPilot, an AI Software Engineer.
            
            Analyze software development issues using the information provided.
            
            Never invent information that is not present in the issue.
            
            Clearly distinguish facts from assumptions.
            
            Return a practical engineering analysis containing:
            
            1. Problem Summary
            2. Likely Root Cause
            3. Recommended Implementation
            4. Potential Files/Classes
            5. Testing Strategy
            6. Next Steps.
            
            You must return your output strictly in valid JSON format matching this schema:
            {
              "summary": "Concise summary of the problem reported in the issue",
              "rootCause": "Analysis of the likely root cause based on available information",
              "implementationPlan": ["Step 1 description", "Step 2 description", ...],
              "potentialFiles": ["File or Class name 1", "File or Class name 2", ...],
              "testingStrategy": ["Test case or strategy 1", "Test case or strategy 2", ...],
              "nextSteps": ["Immediate next action 1", "Next action 2", ...]
            }
            Do not include any markdown fences or explanatory text outside the JSON object.
            """;

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    public DevPilotAgentService(ChatClient.Builder chatClientBuilder,
                                ObjectMapper objectMapper,
                                @Value("${spring.ai.openai.api-key:}") String apiKey) {
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
    }

    public AnalyzeIssueResponse analyzeIssue(GitHubIssueDetails issue) {
        if (apiKey == null || apiKey.isBlank() || "demo-key".equalsIgnoreCase(apiKey.trim())) {
            throw new AgentException(
                    "OpenAI API key is missing or not configured. Please set OPENAI_API_KEY in your environment or .env file.",
                    HttpStatus.UNAUTHORIZED
            );
        }

        String userPrompt = buildUserPrompt(issue);

        try {
            log.info("Sending issue #{} '{}' to OpenAI via Spring AI...", issue.getNumber(), issue.getTitle());

            String rawResponse = chatClient.prompt()
                    .system(SYSTEM_INSTRUCTION)
                    .user(userPrompt)
                    .call()
                    .content();

            log.debug("Raw OpenAI response: {}", rawResponse);

            AnalyzeIssueResponse analysis = parseAiResponse(rawResponse);
            analysis.setSuccess(true);
            analysis.setIssueTitle(issue.getTitle());
            analysis.setIssueUrl(issue.getHtmlUrl());
            analysis.setIssueState(issue.getState());
            analysis.setLabels(issue.getLabels());

            return analysis;
        } catch (AgentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to analyze issue with OpenAI: {}", e.getMessage(), e);
            throw new AgentException("OpenAI API error: " + e.getMessage(), HttpStatus.BAD_GATEWAY, e);
        }
    }

    public AnalyzeIssueResponse analyzeDirectText(String issueTitle, String issueBody) {
        GitHubIssueDetails details = GitHubIssueDetails.builder()
                .number(0L)
                .title(issueTitle)
                .body(issueBody)
                .state("open")
                .labels(List.of())
                .comments(List.of())
                .build();
        return analyzeIssue(details);
    }

    private String buildUserPrompt(GitHubIssueDetails issue) {
        StringBuilder sb = new StringBuilder();
        sb.append("Issue Title: ").append(issue.getTitle()).append("\n");
        sb.append("State: ").append(issue.getState()).append("\n");

        if (issue.getLabels() != null && !issue.getLabels().isEmpty()) {
            sb.append("Labels: ").append(String.join(", ", issue.getLabels())).append("\n");
        }

        sb.append("\nIssue Description/Body:\n");
        if (issue.getBody() != null && !issue.getBody().isBlank()) {
            sb.append(issue.getBody()).append("\n");
        } else {
            sb.append("(No description provided)\n");
        }

        if (issue.getComments() != null && !issue.getComments().isEmpty()) {
            sb.append("\nIssue Comments:\n");
            for (int i = 0; i < issue.getComments().size(); i++) {
                sb.append("--- Comment #").append(i + 1).append(" ---\n");
                sb.append(issue.getComments().get(i)).append("\n");
            }
        }

        sb.append("\nPlease conduct a thorough engineering analysis and respond with the requested JSON.");
        return sb.toString();
    }

    private AnalyzeIssueResponse parseAiResponse(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            throw new AgentException("Received empty response from OpenAI", HttpStatus.BAD_GATEWAY);
        }

        // Clean possible markdown json fences (```json ... ```)
        String cleaned = rawResponse.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        cleaned = cleaned.trim();

        try {
            return objectMapper.readValue(cleaned, AnalyzeIssueResponse.class);
        } catch (Exception e) {
            log.warn("Failed to parse AI response as direct JSON. Attempting fallback extraction. Raw: {}", cleaned);
            return fallbackParse(cleaned);
        }
    }

    private AnalyzeIssueResponse fallbackParse(String text) {
        // Fallback: extract JSON object if wrapped in text
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            String jsonSub = text.substring(start, end + 1);
            try {
                return objectMapper.readValue(jsonSub, AnalyzeIssueResponse.class);
            } catch (Exception ex) {
                log.warn("Sub-string JSON extraction failed: {}", ex.getMessage());
            }
        }

        // Ultimate graceful fallback if AI returned plain text instead of JSON
        return AnalyzeIssueResponse.builder()
                .success(true)
                .summary(text.length() > 500 ? text.substring(0, 500) + "..." : text)
                .rootCause("Extracted from unstructured AI response.")
                .implementationPlan(List.of("Review the full analysis summary above"))
                .potentialFiles(List.of())
                .testingStrategy(List.of("Verify fix against reported issue scenario"))
                .nextSteps(List.of("Review issue discussion and proceed with reproduction"))
                .build();
    }
}
