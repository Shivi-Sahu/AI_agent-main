# DevPilot — AI Software Engineer Agent

[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-1.0.0--M3-blue.svg)](https://spring.io/projects/spring-ai)
[![OpenAI](https://img.shields.io/badge/AI-OpenAI%20GPT--4o--mini-purple.svg)](https://openai.com/)
[![Track](https://img.shields.io/badge/Track%201-AI%20Software%20Engineer-success.svg)](#)

> **DevPilot** is an agentic AI Software Engineer that ingests GitHub issues and discussion threads, analyzes them through specialized software engineering instructions using **Spring AI** & **OpenAI**, and delivers actionable, structured engineering blueprints: Problem Summary, Likely Root Cause, Recommended Implementation Plan, Potential Files/Classes, Testing Strategy, and Next Steps.

---

## 📌 Table of Contents
1. [Project Overview](#-project-overview)
2. [Problem Statement](#-problem-statement)
3. [Key Features](#-key-features)
4. [Architecture & Workflow](#-architecture--workflow)
5. [Tech Stack](#-tech-stack)
6. [Prerequisites & Setup](#-prerequisites--setup)
7. [Environment Variables](#-environment-variables)
8. [Running the Application](#-running-the-application)
9. [API Documentation](#-api-documentation)
10. [Developer Dashboard Demo](#-developer-dashboard-demo)
11. [Testing & Verification](#-testing--verification)
12. [Future Improvements](#-future-improvements)

---

## 🚀 Project Overview

Triage and initial diagnosis of GitHub issues is one of the most time-consuming aspects of software development. Engineers often spend 30–60 minutes per issue reproducing bugs, reading through messy comment threads, locating relevant codebases, and figuring out what files are involved.

**DevPilot** serves as an autonomous software engineering co-pilot. By connecting directly to the **GitHub REST API** and leveraging **Spring AI with OpenAI**, DevPilot extracts issue details and conversation threads, distinguishes facts from assumptions, and generates an end-to-end technical implementation plan in seconds.

---

## 🎯 Problem Statement

Software engineering teams face:
- **Information Asymmetry**: Issue descriptions are often incomplete or scattered across dozens of user comments.
- **Triage Latency**: Critical bugs wait hours or days for senior engineers to analyze root causes.
- **Inconsistent Planning**: Junior engineers may implement fixes without a proper testing strategy or understanding potential blast radius.

**DevPilot solves this** by executing an automated AI Software Engineer workflow:
1. Fetching raw issue data + contributor comments via GitHub REST API.
2. Formulating software engineering context with strict instructions:
   > *"Never invent information that is not present in the issue. Clearly distinguish facts from assumptions."*
3. Returning structured JSON containing exact implementation steps, potential files, test plan, and next actions.
4. Rendering everything on an intuitive, interactive developer dashboard.

---

## ✨ Key Features

- **GitHub REST API Integration**: Retrieves issue title, body, state, labels, and issue discussion comments automatically.
- **Agentic AI Software Engineer**: Powered by Spring AI `ChatClient` with specialized engineering system instructions.
- **Strict Structured JSON Schema**: Clean programmatic contract returning problem summary, root cause, implementation plan, target files, test strategy, and next steps.
- **Embedded Developer Dashboard**: Modern, responsive dark-themed dashboard served directly by Spring Boot (no separate frontend server or CORS configuration needed).
- **1-Click Quick Samples**: Pre-configured buttons for popular open-source issues (React, Spring Boot, Vue) for instant evaluation.
- **One-Click Markdown Export**: Copy full formatted engineering report directly to your clipboard for GitHub PRs or team discussions.
- **Robust Error Handling**: Friendly error notifications for missing tokens, GitHub 404s, rate limits, OpenAI outages, and validation errors (no stack trace exposure).
- **Automated Test Suite**: 100% test pass rate across controllers, GitHub services, validations, and error handlers.

---

## 🏛️ Architecture & Workflow

```
[ Developer / User ]
        │
        ▼ (1) Enters Owner, Repo, Issue # & Clicks "Analyze Issue"
[ Frontend Dashboard ] (HTML5 / Modern CSS / Vanilla JS)
        │
        ▼ (2) POST /api/agent/analyze-github-issue
[ Spring Boot REST API: AgentController ]
        │
        ├─▶ [ Validation & Global Exception Handler ] (Strict DTO checking)
        │
        ├─▶ [ GitHubService ]
        │         │
        │         ▼ (3) GET /repos/{owner}/{repo}/issues/{issueNumber}
        │         ▼ (4) GET /repos/{owner}/{repo}/issues/{issueNumber}/comments
        │      [ GitHub REST API ]
        │
        ├─▶ [ DevPilotAgentService ]
        │         │
        │         ▼ (5) Spring AI ChatClient (System Engineering Prompt + Issue Details)
        │      [ OpenAI API (gpt-4o-mini) ]
        │         │
        │         ▼ (6) Validates & parses structured JSON response
        │
        ▼ (7) Returns AnalyzeIssueResponse
[ Developer Dashboard Displays Results ]
        ├─ Summary Card
        ├─ Likely Root Cause Card
        ├─ Step-by-step Implementation Plan
        ├─ Potential Files / Classes Chips
        ├─ Testing Strategy Checklist
        └─ Next Steps Checklist
```

---

## 💻 Tech Stack

### Backend
- **Java**: 21 (LTS)
- **Framework**: Spring Boot 3.3.4
- **AI Framework**: Spring AI 1.0.0-M3 (`spring-ai-openai-spring-boot-starter`)
- **Web**: Spring MVC / `RestClient`
- **Validation**: Jakarta Bean Validation (`spring-boot-starter-validation`)
- **Boilerplate Reduction**: Project Lombok
- **JSON Processing**: Jackson

### Frontend
- **HTML5 & Vanilla JavaScript**: Zero external UI framework bloat, fast load times.
- **Modern CSS**: Dark theme inspired by GitHub / Linear, responsive grid, status pulses, and badges.

### Integration
- **GitHub REST API**: `https://api.github.com`
- **OpenAI API**: `gpt-4o-mini` (or any configurable OpenAI model)

---

## ⚙️ Prerequisites & Setup

### Requirements
- **Java 21** or higher (`java -version`)
- **Apache Maven 3.8+** (`mvn -version`)
- **OpenAI API Key** (from [platform.openai.com](https://platform.openai.com))
- **GitHub Personal Access Token** *(Optional, recommended to prevent GitHub API rate limits)*

### Clone / Open Project
```bash
cd "/Users/shivambhardwaj/Desktop/AI agent"
```

---

## 🔑 Environment Variables

DevPilot uses standard environment variables. You can configure them via shell export or create a `.env` file from `.env.example`:

```bash
cp .env.example .env
```

Edit `.env` (or set environment variables):
```bash
# OpenAI API Key (Required for AI Analysis)
export OPENAI_API_KEY="sk-proj-your-openai-key-here"

# GitHub Token (Optional for public repositories; recommended for higher rate limits)
export GITHUB_TOKEN="ghp_your_github_token_here"
```

### GitHub Token Setup (Optional)
1. Go to **GitHub Settings** → **Developer Settings** → **Personal Access Tokens** → **Tokens (classic)**.
2. Click **Generate new token**.
3. Select `public_repo` (or read access) and click **Generate Token**.
4. Set `export GITHUB_TOKEN="ghp_..."`.

### OpenAI API Key Setup
1. Go to [platform.openai.com/api-keys](https://platform.openai.com/api-keys).
2. Create a new secret key.
3. Set `export OPENAI_API_KEY="sk-..."`.

---

## 🏃 Running the Application

### 1. Compile & Package
```bash
mvn clean compile
```

### 2. Run Spring Boot
```bash
# With environment variables exported:
mvn spring-boot:run

# Or passing keys inline:
OPENAI_API_KEY="sk-..." GITHUB_TOKEN="ghp_..." mvn spring-boot:run
```

The application will start on:
👉 **`http://localhost:8080`**

### 3. Accessing the Dashboard & Backend
- **Frontend Dashboard**: Open your browser to [`http://localhost:8080`](http://localhost:8080)
- **Health Check Endpoint**: [`http://localhost:8080/api/health`](http://localhost:8080/api/health)

---

## 📖 API Documentation

### 1. Health Check
```http
GET /api/health
```

#### Response (`200 OK`):
```json
{
  "status": "UP",
  "service": "DevPilot - AI Software Engineer Agent",
  "version": "1.0.0",
  "timestamp": "2026-09-25T16:20:00Z"
}
```

---

### 2. Analyze GitHub Issue
```http
POST /api/agent/analyze-github-issue
Content-Type: application/json
```

#### Request Body:
```json
{
  "owner": "spring-projects",
  "repository": "spring-boot",
  "issueNumber": 40000
}
```

#### Response Body (`200 OK`):
```json
{
  "success": true,
  "summary": "Request to add virtual thread executor support to Spring Boot's Netty reactive web server starter.",
  "rootCause": "NettyWebServerFactory does not currently expose an executor configuration for Java 21 Loom virtual threads.",
  "implementationPlan": [
    "Identify Netty configuration hook for event loop executors",
    "Add spring.threads.virtual.enabled conditional executor binding in NettyWebServerFactory",
    "Expose property in ServerProperties.java",
    "Verify backward compatibility with Java 17 and traditional platform threads"
  ],
  "potentialFiles": [
    "org.springframework.boot.autoconfigure.web.reactive.ReactiveWebServerFactoryConfiguration",
    "org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory",
    "org.springframework.boot.autoconfigure.web.ServerProperties"
  ],
  "testingStrategy": [
    "Unit test verifying VirtualThreadTaskExecutor is instantiated when spring.threads.virtual.enabled=true",
    "Integration test booting Netty reactive web server on Java 21",
    "Regression test verifying standard thread pool is preserved on earlier Java baselines"
  ],
  "nextSteps": [
    "Review existing Netty customizers in spring-boot project",
    "Draft minimal reproducible spike project",
    "Submit Pull Request with test coverage"
  ],
  "issueTitle": "Support virtual threads with Netty",
  "issueUrl": "https://github.com/spring-projects/spring-boot/issues/40000",
  "issueState": "open",
  "labels": [
    "status: ideal-for-contribution",
    "theme: virtual-threads"
  ]
}
```

#### Error Response (`400 Bad Request` / `404 Not Found` / `502 Bad Gateway`):
```json
{
  "success": false,
  "error": "GITHUB_ERROR",
  "message": "GitHub issue #999999 not found in repository 'owner/repo'. Please check the owner, repository, and issue number.",
  "timestamp": "2026-09-25T16:21:00Z"
}
```

---

## 🖥️ Developer Dashboard Demo

1. Open **`http://localhost:8080`**.
2. Notice the live **Backend Status Indicator** (turns green when the Spring Boot API is online).
3. Click one of the **Quick Examples** (e.g. `React #28000` or `Spring Boot #35000`) or input any public repository.
4. Click **Analyze Issue**.
5. The animated radar spinner displays live pipeline progress.
6. The resulting dashboard displays:
   - **Header Card**: Live status badge (OPEN/CLOSED), repository tag, title, and direct link to GitHub.
   - **Problem Summary**: Distilled overview of the bug or feature request.
   - **Likely Root Cause**: Engineering breakdown of why the issue occurs.
   - **Recommended Implementation Plan**: Numbered step-by-step development roadmap.
   - **Potential Files / Classes**: Interactive code tags highlighting relevant files.
   - **Testing Strategy**: Concrete checklist for unit, integration, and regression testing.
   - **Next Steps**: Actionable tasks to execute immediately.
   - **Copy Markdown**: Copies the entire structured analysis to your clipboard for instant pasting into GitHub PRs or Jira.

---

## 🧪 Testing & Verification

The project includes unit and integration tests covering all critical components:

```bash
mvn test
```

### Test Suite Summary:
- **`HealthControllerTest`**: Verifies `/api/health` returns status `UP` with timestamp.
- **`AgentControllerTest`**:
  - Valid request processing and response structure.
  - Validation failures for empty owner, repository, or negative issue numbers (HTTP 400).
  - GitHub 404 error translation (HTTP 404).
  - OpenAI failure translation (HTTP 502).
- **`GitHubServiceTest`**:
  - Real JSON parsing of issue details, labels, and contributor comments via `MockRestServiceServer`.
  - Handling of 404 Not Found, 401 Unauthorized, and 403 Rate Limit responses.
- **`ValidationTest`**:
  - Jakarta Bean Validation constraint checks on `AnalyzeIssueRequest`.

All 13 tests execute and pass out-of-the-box.

---

## 🔮 Future Improvements

- **Jira Integration**: Fetch and analyze Jira issues by key (`GET /api/jira/{issueKey}`).
- **Slack & Discord Notifications**: Webhook integration to broadcast engineering analysis directly to team channels.
- **Automated PR Generation**: Feed the recommended implementation plan directly into an automated branch and pull request generator.
- **Codebase Indexing**: Use AST / vector embeddings across the full repository codebase to pinpoint exact line numbers for file changes.
- **Multi-Model Support**: Toggle between OpenAI, Anthropic Claude, and Google Gemini via Spring AI model providers.

---

## 📄 License
This project is licensed under the Apache License 2.0.
