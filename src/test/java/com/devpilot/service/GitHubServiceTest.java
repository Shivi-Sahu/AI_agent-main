package com.devpilot.service;

import com.devpilot.dto.GitHubIssueDetails;
import com.devpilot.exception.GitHubException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GitHubServiceTest {

    private MockRestServiceServer mockServer;
    private GitHubService gitHubService;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        gitHubService = new GitHubService(builder, "https://api.github.com", "test-token-123");
    }

    @Test
    void getIssueShouldReturnParsedDetailsAndComments() {
        String issueJson = """
                {
                  "number": 42,
                  "title": "Fix NullPointerException in RequestHandler",
                  "body": "NullPointerException occurs when header is missing",
                  "state": "open",
                  "html_url": "https://github.com/test-owner/test-repo/issues/42",
                  "labels": [
                    {"name": "bug"},
                    {"name": "high-priority"}
                  ]
                }
                """;

        String commentsJson = """
                [
                  {
                    "user": {"login": "contributor1"},
                    "body": "I reproduced this in version 2.1"
                  }
                ]
                """;

        mockServer.expect(requestTo("https://api.github.com/repos/test-owner/test-repo/issues/42"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer test-token-123"))
                .andRespond(withSuccess(issueJson, MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo("https://api.github.com/repos/test-owner/test-repo/issues/42/comments"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(commentsJson, MediaType.APPLICATION_JSON));

        GitHubIssueDetails result = gitHubService.getIssue("test-owner", "test-repo", 42L);

        assertNotNull(result);
        assertEquals(42L, result.getNumber());
        assertEquals("Fix NullPointerException in RequestHandler", result.getTitle());
        assertEquals("NullPointerException occurs when header is missing", result.getBody());
        assertEquals("open", result.getState());
        assertEquals(2, result.getLabels().size());
        assertEquals("bug", result.getLabels().get(0));
        assertEquals("high-priority", result.getLabels().get(1));
        assertEquals(1, result.getComments().size());
        assertEquals("contributor1: I reproduced this in version 2.1", result.getComments().get(0));

        mockServer.verify();
    }

    @Test
    void getIssueNotFoundShouldThrowGitHubExceptionWith404() {
        mockServer.expect(requestTo("https://api.github.com/repos/test-owner/test-repo/issues/999"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        GitHubException ex = assertThrows(GitHubException.class, () ->
                gitHubService.getIssue("test-owner", "test-repo", 999L)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("GitHub issue #999 not found in repository 'test-owner/test-repo'. Please check the owner, repository, and issue number.", ex.getMessage());
        mockServer.verify();
    }

    @Test
    void getIssueUnauthorizedShouldThrowGitHubExceptionWith401() {
        mockServer.expect(requestTo("https://api.github.com/repos/test-owner/test-repo/issues/1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        GitHubException ex = assertThrows(GitHubException.class, () ->
                gitHubService.getIssue("test-owner", "test-repo", 1L)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        assertEquals("Invalid GitHub token. Please verify your GITHUB_TOKEN environment variable.", ex.getMessage());
        mockServer.verify();
    }

    @Test
    void getIssueForbiddenShouldThrowGitHubExceptionWith403() {
        mockServer.expect(requestTo("https://api.github.com/repos/test-owner/test-repo/issues/1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        GitHubException ex = assertThrows(GitHubException.class, () ->
                gitHubService.getIssue("test-owner", "test-repo", 1L)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("GitHub API access forbidden or rate limit exceeded. Set a valid GITHUB_TOKEN in your environment.", ex.getMessage());
        mockServer.verify();
    }
}
