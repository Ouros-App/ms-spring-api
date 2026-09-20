const fs = require('fs');
const path = require('path');

async function main() {
  const token = process.env.GITHUB_TOKEN;
  const julesApiKey = process.env.JULES_API_KEY || process.env.GEMINI_API_KEY;
  const repoFullName = process.env.GITHUB_REPOSITORY; // e.g. "Ouros-App/ms-spring-api"
  const prNumber = process.env.PR_NUMBER;
  const commitSha = process.env.COMMIT_SHA;

  if (!token || !julesApiKey || !repoFullName || !prNumber) {
    console.error('Missing required environment variables (GITHUB_TOKEN, JULES_API_KEY, GITHUB_REPOSITORY, PR_NUMBER).');
    process.exit(1);
  }

  // 1. Read AGENTS.md
  let agentsMdContent = '';
  const agentsPath = path.join(process.cwd(), 'AGENTS.md');
  if (fs.existsSync(agentsPath)) {
    agentsMdContent = fs.readFileSync(agentsPath, 'utf8');
  }

  // 2. Read Revisor de Código Skill
  let skillContent = '';
  const skillPath = path.join(process.cwd(), '.github', 'skills', 'revisor-de-codigo', 'SKILL.md');
  if (fs.existsSync(skillPath)) {
    skillContent = fs.readFileSync(skillPath, 'utf8');
  }

  // 3. Fetch PR metadata (Title and Description/Body)
  console.log(`Fetching PR #${prNumber} metadata (Title & Description)...`);
  let prTitle = '';
  let prBody = '';
  try {
    const metaResponse = await fetch(`https://api.github.com/repos/${repoFullName}/pulls/${prNumber}`, {
      headers: {
        'Authorization': `Bearer ${token}`,
        'Accept': 'application/vnd.github.v3+json',
        'User-Agent': 'ms-spring-api-jules-agent'
      }
    });
    if (metaResponse.ok) {
      const metaData = await metaResponse.json();
      prTitle = metaData.title || '';
      prBody = metaData.body || '';
    }
  } catch (err) {
    console.warn('Could not fetch PR metadata:', err.message);
  }

  // 4. Fetch PR diff from GitHub API
  console.log(`Fetching PR #${prNumber} diff for ${repoFullName} (Commit: ${commitSha || 'latest'})...`);
  const diffResponse = await fetch(`https://api.github.com/repos/${repoFullName}/pulls/${prNumber}`, {
    headers: {
      'Authorization': `Bearer ${token}`,
      'Accept': 'application/vnd.github.v3.diff',
      'User-Agent': 'ms-spring-api-jules-agent'
    }
  });

  if (!diffResponse.ok) {
    console.error(`Failed to fetch PR diff: ${diffResponse.status} ${diffResponse.statusText}`);
    process.exit(1);
  }

  const prDiff = await diffResponse.text();

  if (!prDiff || prDiff.trim().length === 0) {
    console.log('PR diff is empty. Skipping review.');
    return;
  }

  // Limit diff size if overly large to fit context
  const maxDiffLength = 60000;
  const truncatedDiff = prDiff.length > maxDiffLength 
    ? prDiff.substring(0, maxDiffLength) + '\n... [Diff truncado devido ao tamanho]' 
    : prDiff;

  // 5. Construct Prompt using Antigravity revisor-de-codigo Skill + AGENTS.md + PR Description
  const prompt = `Você é o Jules (Google Coding Agent) executando a Skill oficial de Code Review do Antigravity.

=== SKILL INSTRUCTIONS (revisor-de-codigo) ===
${skillContent}

=== REPOSITORY RULES (AGENTS.md) ===
${agentsMdContent}

=== CONTEXTO DA PULL REQUEST (TÍTULO E DESCRIÇÃO) ===
**Título:** ${prTitle || 'Sem título'}
**Descrição da PR:**
${prBody || 'Nenhuma descrição fornecida na PR.'}

=== INSTRUÇÕES ESPECÍFICAS DESTE REVIEW ===
1. Todas as informações de contexto (objetivo, regras de negócio e escopo) estão descritas acima no TÍTULO E DESCRIÇÃO DA PULL REQUEST.
2. Analise exclusivamente as alterações presentes no diff abaixo.
3. Não avalie código preexistente intocado.
4. Responda ESTRITAMENTE em formato JSON com o seguinte schema:
{
  "summary": "### 🤖 Jules Code Review (Antigravity Skill)\\n\\n**Resumo da Alteração:**\\n<visão geral das modificações com base na descrição da PR>\\n\\n**Checklist / Tabela de Impacto:**\\n- [ ] Ponto 1\\n- [ ] Ponto 2\\n\\n**Veredito:**\\n<Aprovado | Requer Ajustes>",
  "comments": [
    {
      "path": "caminho/do/arquivo.java",
      "line": 42,
      "title": "[Crítico|Importante|Sugestão] Título",
      "explanation": "Explicação técnica do problema e impacto em Português (PT-BR).",
      "suggestion": "código exato de substituição para a linha/bloco modificado (sem crases de markdown)"
    }
  ]
}

Atenção: A propriedade "line" deve ser um número inteiro indicando a linha no novo código (lado direito do diff). Se não houver sugestão de código de 1-clique, envie "suggestion" como null.

=== DIFF DA ALTERAÇÃO ===
\`\`\`diff
${truncatedDiff}
\`\`\`
`;

  // 6. Call Google Jules / Gemini Engine with JULES_API_KEY
  console.log('Executing review via Jules API Engine with JULES_API_KEY...');
  const endpoint = `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${julesApiKey}`;

  const response = await fetch(endpoint, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'x-goog-api-key': julesApiKey
    },
    body: JSON.stringify({
      contents: [
        {
          role: 'user',
          parts: [{ text: prompt }]
        }
      ],
      generationConfig: {
        responseMimeType: 'application/json',
        temperature: 0.1
      }
    })
  });

  if (!response.ok) {
    const errorText = await response.text();
    console.error(`Jules execution failed: ${response.status} ${errorText}`);
    process.exit(1);
  }

  const responseData = await response.json();
  const rawContent = responseData?.candidates?.[0]?.content?.parts?.[0]?.text;

  if (!rawContent) {
    console.error('Empty response received from Jules.');
    process.exit(1);
  }

  let reviewResult;
  try {
    reviewResult = JSON.parse(rawContent);
  } catch (e) {
    console.error('Failed to parse Jules review response as JSON:', rawContent);
    process.exit(1);
  }

  // 7. Format Inline Comments
  const githubComments = [];
  if (Array.isArray(reviewResult.comments)) {
    for (const c of reviewResult.comments) {
      if (!c.path || !c.line) continue;

      let bodyText = `### ${c.title || '[Sugestão]'}\n\n${c.explanation || ''}`;
      if (c.suggestion && c.suggestion.trim().length > 0) {
        bodyText += `\n\n\`\`\`suggestion\n${c.suggestion.trim()}\n\`\`\``;
      }

      githubComments.push({
        path: c.path,
        line: parseInt(c.line, 10),
        side: 'RIGHT',
        body: bodyText
      });
    }
  }

  const shortCommit = commitSha ? commitSha.substring(0, 7) : 'update';
  const summaryHeader = `## 🤖 Jules Code Review (Commit: \`${shortCommit}\`)\n\n`;
  const summaryBody = summaryHeader + (reviewResult.summary || 'Nenhum problema identificado nesta alteração.');

  // 8. Post a NEW Review on GitHub for this alteration/commit
  console.log(`Posting new Jules review for commit ${shortCommit} (${githubComments.length} inline suggestion(s))...`);
  
  const reviewPayload = {
    commit_id: commitSha || undefined,
    body: summaryBody,
    event: 'COMMENT',
    comments: githubComments
  };

  const postReviewUrl = `https://api.github.com/repos/${repoFullName}/pulls/${prNumber}/reviews`;
  const postReviewResponse = await fetch(postReviewUrl, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Accept': 'application/vnd.github.v3+json',
      'Content-Type': 'application/json',
      'User-Agent': 'ms-spring-api-jules-agent'
    },
    body: JSON.stringify(reviewPayload)
  });

  if (!postReviewResponse.ok) {
    const errBody = await postReviewResponse.text();
    console.warn(`Inline review comment rejected by GitHub (${postReviewResponse.status}): ${errBody}`);
    console.log('Posting review as new issue comment fallback...');
    
    const fallbackCommentUrl = `https://api.github.com/repos/${repoFullName}/issues/${prNumber}/comments`;
    let fallbackText = `${summaryBody}`;
    if (githubComments.length > 0) {
      fallbackText += `\n\n---\n### 📌 Sugestões Inline da Skill:\n` + 
        githubComments.map(c => `**${c.path} (L${c.line}):**\n${c.body}`).join('\n\n');
    }
    
    const fallbackResponse = await fetch(fallbackCommentUrl, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Accept': 'application/vnd.github.v3+json',
        'Content-Type': 'application/json',
        'User-Agent': 'ms-spring-api-jules-agent'
      },
      body: JSON.stringify({ body: fallbackText })
    });

    if (!fallbackResponse.ok) {
      console.error('Failed to post fallback review comment.');
      process.exit(1);
    }
  }

  console.log(`Jules Code Review successfully posted for commit ${shortCommit}!`);
}

main().catch(err => {
  console.error('Fatal error during Jules code review:', err);
  process.exit(1);
});
