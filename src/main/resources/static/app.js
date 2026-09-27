document.addEventListener('DOMContentLoaded', () => {
  // Elements
  const analyzeForm = document.getElementById('analyze-form');
  const ownerInput = document.getElementById('owner');
  const repoInput = document.getElementById('repository');
  const issueInput = document.getElementById('issueNumber');
  const submitBtn = document.getElementById('submit-btn');

  const backendStatusEl = document.getElementById('backend-status');
  const loadingStateEl = document.getElementById('loading-state');
  const errorStateEl = document.getElementById('error-state');
  const errorTitleEl = document.getElementById('error-title');
  const errorMessageEl = document.getElementById('error-message');
  const resultsEl = document.getElementById('results-dashboard');

  // Result Elements
  const resStateBadge = document.getElementById('res-state-badge');
  const resRepoTag = document.getElementById('res-repo-tag');
  const resIssueTitle = document.getElementById('res-issue-title');
  const resLabels = document.getElementById('res-labels');
  const resGithubLink = document.getElementById('res-github-link');
  const resSummary = document.getElementById('res-summary');
  const resRootCause = document.getElementById('res-root-cause');
  const resPlan = document.getElementById('res-implementation-plan');
  const resFiles = document.getElementById('res-potential-files');
  const resTesting = document.getElementById('res-testing-strategy');
  const resNextSteps = document.getElementById('res-next-steps');
  const copyMarkdownBtn = document.getElementById('copy-markdown-btn');

  let currentAnalysisData = null;

  // 1. Check backend health on startup
  checkBackendHealth();

  // 2. Setup Quick Sample buttons
  document.querySelectorAll('.sample-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      ownerInput.value = btn.dataset.owner;
      repoInput.value = btn.dataset.repo;
      issueInput.value = btn.dataset.issue;
      ownerInput.focus();
    });
  });

  // 3. Handle Form Submission
  analyzeForm.addEventListener('submit', async (e) => {
    e.preventDefault();

    const owner = ownerInput.value.trim();
    const repository = repoInput.value.trim();
    const issueNumber = parseInt(issueInput.value, 10);

    if (!owner || !repository || isNaN(issueNumber) || issueNumber <= 0) {
      showError('Validation Error', 'Please enter a valid GitHub owner, repository, and positive issue number.');
      return;
    }

    // Set UI to loading state
    setLoading(true);

    try {
      const response = await fetch('/api/agent/analyze-github-issue', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json'
        },
        body: JSON.stringify({ owner, repository, issueNumber })
      });

      const data = await response.json();

      if (!response.ok || !data.success) {
        const errorMsg = data.message || `Server returned error (${response.status})`;
        showError(data.error || 'Analysis Failed', errorMsg);
        return;
      }

      currentAnalysisData = { ...data, owner, repository, issueNumber };
      renderResults(currentAnalysisData);
    } catch (err) {
      showError('Network Error', `Could not connect to DevPilot backend: ${err.message}. Is the Spring Boot server running?`);
    } finally {
      setLoading(false);
    }
  });

  // 4. Copy as Markdown
  copyMarkdownBtn.addEventListener('click', () => {
    if (!currentAnalysisData) return;
    const md = buildMarkdown(currentAnalysisData);
    navigator.clipboard.writeText(md).then(() => {
      const originalText = copyMarkdownBtn.innerHTML;
      copyMarkdownBtn.innerHTML = `
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#3fb950" stroke-width="2">
          <polyline points="20 6 9 17 4 12"></polyline>
        </svg>
        <span>Copied!</span>
      `;
      setTimeout(() => {
        copyMarkdownBtn.innerHTML = originalText;
      }, 2000);
    }).catch(err => {
      console.error('Failed to copy: ', err);
    });
  });

  // Helper functions
  async function checkBackendHealth() {
    try {
      const res = await fetch('/api/health');
      if (res.ok) {
        backendStatusEl.className = 'status-indicator online';
        backendStatusEl.querySelector('.status-text').textContent = 'Backend Online (Spring AI)';
      } else {
        markOffline();
      }
    } catch {
      markOffline();
    }
  }

  function markOffline() {
    backendStatusEl.className = 'status-indicator offline';
    backendStatusEl.querySelector('.status-text').textContent = 'Backend Offline';
  }

  function setLoading(isLoading) {
    if (isLoading) {
      loadingStateEl.classList.remove('hidden');
      errorStateEl.classList.add('hidden');
      resultsEl.classList.add('hidden');
      submitBtn.disabled = true;
      submitBtn.querySelector('span').textContent = 'Analyzing...';
    } else {
      loadingStateEl.classList.add('hidden');
      submitBtn.disabled = false;
      submitBtn.querySelector('span').textContent = 'Analyze Issue';
    }
  }

  function showError(title, message) {
    errorTitleEl.textContent = title;
    errorMessageEl.textContent = message;
    errorStateEl.classList.remove('hidden');
    resultsEl.classList.add('hidden');
    loadingStateEl.classList.add('hidden');
    errorStateEl.scrollIntoView({ behavior: 'smooth' });
  }

  function renderResults(data) {
    errorStateEl.classList.add('hidden');
    resultsEl.classList.remove('hidden');

    // Header metadata
    const state = (data.issueState || 'open').toLowerCase();
    resStateBadge.textContent = state.toUpperCase();
    resStateBadge.className = `badge status-${state === 'closed' ? 'closed' : 'open'}`;

    resRepoTag.textContent = `${data.owner}/${data.repository}#${data.issueNumber}`;
    resIssueTitle.textContent = data.issueTitle || `Issue #${data.issueNumber}`;

    if (data.issueUrl) {
      resGithubLink.href = data.issueUrl;
      resGithubLink.classList.remove('hidden');
    } else {
      resGithubLink.href = `https://github.com/${data.owner}/${data.repository}/issues/${data.issueNumber}`;
    }

    // Labels
    resLabels.innerHTML = '';
    if (data.labels && data.labels.length > 0) {
      data.labels.forEach(lbl => {
        const span = document.createElement('span');
        span.className = 'label-pill';
        span.textContent = lbl;
        resLabels.appendChild(span);
      });
    }

    // Problem Summary
    resSummary.textContent = data.summary || 'No summary provided.';

    // Root Cause
    resRootCause.textContent = data.rootCause || 'No root cause identified.';

    // Implementation Plan
    resPlan.innerHTML = '';
    if (data.implementationPlan && data.implementationPlan.length > 0) {
      data.implementationPlan.forEach(step => {
        const li = document.createElement('li');
        li.textContent = step;
        resPlan.appendChild(li);
      });
    } else {
      const li = document.createElement('li');
      li.textContent = 'No explicit implementation steps identified.';
      resPlan.appendChild(li);
    }

    // Potential Files
    resFiles.innerHTML = '';
    if (data.potentialFiles && data.potentialFiles.length > 0) {
      data.potentialFiles.forEach(file => {
        const div = document.createElement('div');
        div.className = 'file-chip';
        div.innerHTML = `
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M13 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z"></path>
            <polyline points="13 2 13 9 20 9"></polyline>
          </svg>
          <span>${escapeHtml(file)}</span>
        `;
        resFiles.appendChild(div);
      });
    } else {
      resFiles.innerHTML = '<span style="color: var(--text-muted); font-size: 0.9rem;">None explicitly specified in the issue.</span>';
    }

    // Testing Strategy
    resTesting.innerHTML = '';
    if (data.testingStrategy && data.testingStrategy.length > 0) {
      data.testingStrategy.forEach(item => {
        const li = document.createElement('li');
        li.innerHTML = `
          <svg class="bullet-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="9 11 12 14 22 4"></polyline>
            <path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"></path>
          </svg>
          <span>${escapeHtml(item)}</span>
        `;
        resTesting.appendChild(li);
      });
    } else {
      resTesting.innerHTML = '<li><span>Standard unit and regression testing recommended.</span></li>';
    }

    // Next Steps
    resNextSteps.innerHTML = '';
    if (data.nextSteps && data.nextSteps.length > 0) {
      data.nextSteps.forEach(step => {
        const li = document.createElement('li');
        li.innerHTML = `
          <svg class="bullet-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"></circle>
            <polyline points="12 6 12 12 16 14"></polyline>
          </svg>
          <span>${escapeHtml(step)}</span>
        `;
        resNextSteps.appendChild(li);
      });
    } else {
      resNextSteps.innerHTML = '<li><span>Proceed with implementation and test coverage.</span></li>';
    }

    resultsEl.scrollIntoView({ behavior: 'smooth' });
  }

  function buildMarkdown(data) {
    let md = `# DevPilot Engineering Analysis: ${data.owner}/${data.repository}#${data.issueNumber}\n\n`;
    if (data.issueTitle) md += `**Title**: ${data.issueTitle}\n`;
    if (data.issueUrl) md += `**Link**: ${data.issueUrl}\n\n`;
    md += `## 1. Problem Summary\n${data.summary}\n\n`;
    md += `## 2. Likely Root Cause\n${data.rootCause}\n\n`;
    md += `## 3. Recommended Implementation\n`;
    (data.implementationPlan || []).forEach((step, idx) => {
      md += `${idx + 1}. ${step}\n`;
    });
    md += `\n## 4. Potential Files / Classes\n`;
    (data.potentialFiles || []).forEach(f => {
      md += `- \`${f}\`\n`;
    });
    md += `\n## 5. Testing Strategy\n`;
    (data.testingStrategy || []).forEach(t => {
      md += `- [ ] ${t}\n`;
    });
    md += `\n## 6. Next Steps\n`;
    (data.nextSteps || []).forEach(n => {
      md += `- ${n}\n`;
    });
    return md;
  }

  function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/[&<>"']/g, (m) => ({
      '&': '&amp;',
      '<': '&lt;',
      '>': '&gt;',
      '"': '&quot;',
      "'": '&#39;'
    })[m]);
  }
});
