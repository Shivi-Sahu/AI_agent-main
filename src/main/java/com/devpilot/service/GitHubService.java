package com.devpilot.service;

import com.devpilot.dto.GitHubIssueDetails;
import com.devpilot.exception.GitHubException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class GitHubService {

    private final RestClient restClient;
    private final String githubToken;

    public GitHubService(RestClient.Builder restClientBuilder,
                         @Value("${github.api.base-url:https://api.github.com}") String baseUrl,
                         @Value("${github.token:}") String githubToken) {
        this.githubToken = (githubToken != null && !githubToken.isBlank()) ? githubToken.trim() : null;

        RestClient.Builder builder = restClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .defaultHeader(HttpHeaders.USER_AGENT, "DevPilot-AI-Agent");

        if (this.githubToken != null) {
            log.info("Configuring GitHubService with personal access token authentication.");
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + this.githubToken);
        } else {
            log.warn("No GITHUB_TOKEN provided. Operating under standard unauthenticated GitHub rate limits (60 req/hr).");
        }

        this.restClient = builder.build();
    }

    public GitHubIssueDetails getIssue(String owner, String repository, Long issueNumber) {
        log.info("Fetching issue #{} from {}/{}", issueNumber, owner, repository);

        JsonNode issueNode;
        try {
            issueNode = restClient.get()
                    .uri("/repos/{owner}/{repository}/issues/{issueNumber}", owner, repository, issueNumber)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        HttpStatus status = HttpStatus.resolve(response.getStatusCode().value());
                        if (status == HttpStatus.NOT_FOUND) {
                            throw new GitHubException(
                                    String.format("GitHub issue #%d not found in repository '%s/%s'. Please check the owner, repository, and issue number.",
                                            issueNumber, owner, repository),
                                    HttpStatus.NOT_FOUND
                            );
                        } else if (status == HttpStatus.UNAUTHORIZED) {
                            throw new GitHubException(
                                    "Invalid GitHub token. Please verify your GITHUB_TOKEN environment variable.",
                                    HttpStatus.UNAUTHORIZED
                            );
                        } else if (status == HttpStatus.FORBIDDEN) {
                            throw new GitHubException(
                                    "GitHub API access forbidden or rate limit exceeded. Set a valid GITHUB_TOKEN in your environment.",
                                    HttpStatus.FORBIDDEN
                            );
                        } else {
                            throw new GitHubException(
                                    "GitHub API returned error: " + response.getStatusText() + " (status " + response.getStatusCode() + ")",
                                    status != null ? status : HttpStatus.BAD_GATEWAY
                            );
                        }
                    })
                    .body(JsonNode.class);
        } catch (GitHubException e) {
            throw e;
        } catch (Exception e) {
            log.error("Network or connection error while contacting GitHub API: {}", e.getMessage());
            throw new GitHubException("Failed to reach GitHub API: " + e.getMessage(), HttpStatus.SERVICE_UNAVAILABLE, e);
        }

        if (issueNode == null) {
            throw new GitHubException("Empty response received from GitHub API for issue #" + issueNumber, HttpStatus.BAD_GATEWAY);
        }

        String title = issueNode.path("title").asText("");
        String body = issueNode.path("body").asText("");
        String state = issueNode.path("state").asText("unknown");
        String htmlUrl = issueNode.path("html_url").asText("");

        List<String> labels = new ArrayList<>();
        JsonNode labelsNode = issueNode.path("labels");
        if (labelsNode.isArray()) {
            for (JsonNode label : labelsNode) {
                String labelName = label.path("name").asText(null);
                if (labelName != null && !labelName.isBlank()) {
                    labels.add(labelName);
                }
            }
        }

        List<String> comments = fetchComments(owner, repository, issueNumber);

        return GitHubIssueDetails.builder()
                .number(issueNumber)
                .title(title)
                .body(body)
                .state(state)
                .htmlUrl(htmlUrl)
                .labels(labels)
                .comments(comments)
                .build();
    }

    private List<String> fetchComments(String owner, String repository, Long issueNumber) {
        List<String> comments = new ArrayList<>();
        try {
            JsonNode commentsNode = restClient.get()
                    .uri("/repos/{owner}/{repository}/issues/{issueNumber}/comments", owner, repository, issueNumber)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(JsonNode.class);

            if (commentsNode != null && commentsNode.isArray()) {
                for (JsonNode comment : commentsNode) {
                    String author = comment.path("user").path("login").asText("unknown");
                    String commentBody = comment.path("body").asText("");
                    if (!commentBody.isBlank()) {
                        comments.add(author + ": " + commentBody);
                    }
                }
            }
            log.info("Fetched {} comments for issue #{}", comments.size(), issueNumber);
        } catch (Exception e) {
            // Comments are optional; if comments fail (e.g. rate limit), log and proceed without failing the whole flow
            log.warn("Could not retrieve comments for issue #{}: {}", issueNumber, e.getMessage());
        }
        return comments;
    }
}
