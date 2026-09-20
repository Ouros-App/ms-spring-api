const fs = require('fs');
const path = require('path');

async function main() {
  const token = process.env.GITHUB_TOKEN;
  const apiKey = process.env.JULES_API_KEY || process.env.GEMINI_API_KEY;
  const repoFullName = process.env.GITHUB_REPOSITORY; // e.g. "Ouros-App/ms-spring-api"
  const prNumber = process.env.PR_NUMBER;
  const prBranch = process.env.PR_BRANCH;
  const commitSha = process.env.COMMIT_SHA;

  if (!token) {
    console.error('Error: GITHUB_TOKEN is not defined.');
    process.exit(1);
  }

  if (!apiKey) {
    console.error('Error: JULES_API_KEY is not defined in repository secrets.');
    process.exit(1);
  }

  if (!repoFullName || !prNumber) {
    console.error('Error: Missing required environment variables (GITHUB_REPOSITORY, PR_NUMBER).');
    process.exit(1);
  }

  const shortCommit = commitSha ? commitSha.substring(0, 7) : 'latest';
  console.log(`Starting automated Code Review for PR #${prNumber} (Branch: ${prBranch || 'PR'}, Commit: ${shortCommit})...`);

  // 1. Read AGENTS.md rules
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

  // 3. Fetch PR Metadata (Title and Body) from GitHub API
  console.log(`Fetching PR #${prNumber} metadata (Title & Description)...`);
  let prTitle = '';
  let prBody = '';
  try {
    const metaResponse = await fetch(`https://api.github.com/repos/${repoFullName}/pulls/${prNumber}`, {
      headers: {
        'Authorization': `Bearer ${token}`,
        'Accept': 'application/vnd.github.v3+json',
        'User-Agent': 'ms-spring-api-jules-action'
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
  console.log(`Fetching PR #${prNumber} diff from GitHub API...`);
  const diffResponse = await fetch(`https://api.github.com/repos/${repoFullName}/pulls/${prNumber}`, {
    headers: {
      'Authorization': `Bearer ${token}`,
      'Accept': 'application/vnd.github.v3.diff',
      'User-Agent': 'ms-spring-api-jules-action'
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

  // 5. Construct CodeRabbit-style Review Prompt
  const prompt = `Você é o Jules atuando como um Revisor de Código Sênior no estilo CodeRabbit para o repositório ${repoFullName}.

=== SKILL INSTRUCTIONS (revisor-de-codigo) ===
${skillContent}

=== REPOSITORY RULES (AGENTS.md) ===
${agentsMdContent}

=== CONTEXTO DA PULL REQUEST (TÍTULO E DESCRIÇÃO) ===
**Título:** ${prTitle || 'Sem título'}
**Descrição da PR:**
${prBody || 'Nenhuma descrição detalhada fornecida.'}

=== INSTRUÇÕES ESPECÍFICAS DESTE REVIEW ===
1. Todas as informações de contexto (objetivo, regras de negócio e escopo) estão descritas acima no TÍTULO E DESCRIÇÃO DA PULL REQUEST.
2. Analise exclusivamente as alterações presentes no diff abaixo. Não avalie código preexistente intocado.
3. Não execute testes no Gradle durante a análise.
4. Responda ESTRITAMENTE em formato JSON com o seguinte schema:
{
  "summary": "### 🤖 Jules Code Review\\n\\n**Resumo da Alteração:**\\n<visão geral das modificações com base na descrição da PR e diff>\\n\\n**Checklist / Tabela de Impacto:**\\n- [ ] Ponto 1\\n- [ ] Ponto 2\\n\\n**Veredito:**\\n<Aprovado | Requer Ajustes | Ponto de Atenção>",
  "comments": [
    {
      "path": "caminho/do/arquivo.java",
      "line": 42,
      "title": "[Crítico|Importante|Sugestão] Título",
      "explanation": "Explicação técnica do problema e impacto em Português (PT-BR).",
      "suggestion": "código exato de substituição para a linha/bloco modificado (sem crases de markdown, apenas o código puro)"
    }
  ]
}

Atenção: A propriedade "line" deve ser um número inteiro indicando a linha no novo código (lado direito do diff). Se não houver sugestão de código de 1-clique, envie "suggestion" como null ou string vazia.

=== DIFF DA ALTERAÇÃO ===
\`\`\`diff
${truncatedDiff}
\`\`\`
`;

  // 6. Execute AI Review using Google Jules/Gemini Engine
  console.log('Generating code review via Jules AI Engine with JULES_API_KEY...');
  const endpoint = `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${apiKey}`;

  const response = await fetch(endpoint, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'x-goog-api-key': apiKey
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
    console.error(`Jules AI execution failed: ${response.status} ${errorText}`);
    process.exit(1);
  }

  const responseData = await response.json();
  const rawContent = responseData?.candidates?.[0]?.content?.parts?.[0]?.text;

  if (!rawContent) {
    console.error('Empty response received from AI engine.');
    process.exit(1);
  }

  let reviewResult;
  try {
    reviewResult = JSON.parse(rawContent);
  } catch (e) {
    console.error('Failed to parse review response as JSON:', rawContent);
    process.exit(1);
  }

  // 7. Format GitHub Inline Review Comments (with ```suggestion``` blocks)
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

  const summaryHeader = `## 🤖 Jules Code Review (Commit: \`${shortCommit}\`)\n\n`;
  const summaryBody = summaryHeader + (reviewResult.summary || 'Nenhum problema identificado nesta alteração.');

  // 8. AUTOMATICALLY Post the Review Directly on the Current GitHub PR!
  console.log(`Publishing review comment directly to GitHub PR #${prNumber} (Commit: ${shortCommit})...`);
  
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
      'User-Agent': 'ms-spring-api-jules-action'
    },
    body: JSON.stringify(reviewPayload)
  });

  if (!postReviewResponse.ok) {
    const errBody = await postReviewResponse.text();
    console.warn(`Inline review comment rejected by GitHub (${postReviewResponse.status}): ${errBody}`);
    console.log('Posting review as new issue comment fallback on PR...');
    
    const fallbackCommentUrl = `https://api.github.com/repos/${repoFullName}/issues/${prNumber}/comments`;
    let fallbackText = `${summaryBody}`;
    if (githubComments.length > 0) {
      fallbackText += `\n\n---\n### 📌 Sugestões da Revisão:\n` + 
        githubComments.map(c => `**${c.path} (L${c.line}):**\n${c.body}`).join('\n\n');
    }
    
    const fallbackResponse = await fetch(fallbackCommentUrl, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Accept': 'application/vnd.github.v3+json',
        'Content-Type': 'application/json',
        'User-Agent': 'ms-spring-api-jules-action'
      },
      body: JSON.stringify({ body: fallbackText })
    });

    if (!fallbackResponse.ok) {
      console.error('Failed to post fallback review comment to PR.');
      process.exit(1);
    }
  }

  console.log(`🎉 Sucesso! Code Review do Jules publicado diretamente na PR #${prNumber}!`);
}

main().catch(err => {
  console.error('Fatal error during code review:', err);
  process.exit(1);
});
