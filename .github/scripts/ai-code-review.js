const fs = require('fs');
const path = require('path');

async function main() {
  const token = process.env.GITHUB_TOKEN;
  // Support JULES_API_KEY (primary) or GEMINI_API_KEY (fallback)
  const apiKey = process.env.JULES_API_KEY || process.env.GEMINI_API_KEY;
  const repoFullName = process.env.GITHUB_REPOSITORY; // e.g. "Ouros-App/ms-spring-api"
  const prNumber = process.env.PR_NUMBER;
  const commitSha = process.env.COMMIT_SHA;

  if (!token || !apiKey || !repoFullName || !prNumber) {
    console.error('Missing required environment variables (GITHUB_TOKEN, JULES_API_KEY / GEMINI_API_KEY, GITHUB_REPOSITORY, PR_NUMBER).');
    process.exit(1);
  }

  // 1. Read AGENTS.md rules
  let agentsMdContent = '';
  const agentsPath = path.join(process.cwd(), 'AGENTS.md');
  if (fs.existsSync(agentsPath)) {
    agentsMdContent = fs.readFileSync(agentsPath, 'utf8');
  }

  // 2. Fetch PR diff from GitHub API
  console.log(`Fetching PR #${prNumber} diff for ${repoFullName} at commit ${commitSha || 'latest'}...`);
  const diffResponse = await fetch(`https://api.github.com/repos/${repoFullName}/pulls/${prNumber}`, {
    headers: {
      'Authorization': `Bearer ${token}`,
      'Accept': 'application/vnd.github.v3.diff',
      'User-Agent': 'ms-spring-api-jules-reviewer'
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

  // Truncate diff if extremely large (> 60KB) to prevent token overflow
  const maxDiffLength = 60000;
  const truncatedDiff = prDiff.length > maxDiffLength 
    ? prDiff.substring(0, maxDiffLength) + '\n... [Diff truncado devido ao tamanho]' 
    : prDiff;

  // 3. Build CodeRabbit-style Prompt using Jules Engine
  const prompt = `Você é o Jules (Google AI Coding Agent) atuando como um Revisor de Código Sênior rigoroso no estilo CodeRabbit para o repositório ms-spring-api.

### Diretrizes de Arquitetura e Engenharia (AGENTS.md):
${agentsMdContent}

### Instruções de Análise e Estilo CodeRabbit:
1. **Foco Estrito no Diff da Alteração:** Analise minuciosamente os arquivos e linhas modificados no diff fornecido.
2. **Regras Técnicas Obrigatórias:**
   - **Bugs e Lógica:** Trate exceções, concorrência, bordas e regressões.
   - **Segurança & RBAC:** Verifique autorização (ADM, COMPANY_EMPLOYEE, FARM_OWNER), tokens JWT e vazamento de segredos.
   - **Padrões do Repositório:** DTOs com Java \`record\`, Spring Boot 3.4.0 com \`@MockitoBean\` (nunca usar \`@MockBean\`), anotações Swagger/OpenAPI, transações \`@Transactional\`.
   - **Idiomas:** Código Java em inglês; mensagens, explicações e comentários em **Português (PT-BR)**.
3. **Formato da Resposta:**
   Retorne estritamente um JSON com esta estrutura:
   {
     "summary": "### 🤖 Jules Code Review\\n\\n**Resumo da Alteração:**\\n<explicação em alto nível da alteração nesta commit/update>\\n\\n**Checklist / Tabela de Impacto:**\\n- [ ] Ponto de atenção 1\\n- [ ] Ponto de atenção 2\\n\\n**Veredito:**\\n<Aprovado | Requer Ajustes | Ponto de Atenção>",
     "comments": [
       {
         "path": "caminho/do/arquivo.java",
         "line": 42,
         "title": "[Crítico|Importante|Sugestão] Título objetivo",
         "explanation": "Explicação técnica do problema e impacto em PT-BR.",
         "suggestion": "código exato de substituição para a linha/bloco (sem as aspas de markdown, apenas o código)"
       }
     ]
   }

Atenção: O campo "line" deve apontar para o número da linha no arquivo modificado (lado direito do diff). Se não houver sugestão de substituição em código, defina "suggestion" como null ou string vazia.

### Diff da Alteração:
\`\`\`diff
${truncatedDiff}
\`\`\`
`;

  // 4. Call API with JULES_API_KEY
  console.log('Calling API for Jules Code Review...');
  const apiEndpoint = `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${apiKey}`;

  const apiResponse = await fetch(apiEndpoint, {
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
        temperature: 0.2
      }
    })
  });

  if (!apiResponse.ok) {
    const errorText = await apiResponse.text();
    console.error(`Jules API call failed: ${apiResponse.status} ${errorText}`);
    process.exit(1);
  }

  const apiData = await apiResponse.json();
  const rawContent = apiData?.candidates?.[0]?.content?.parts?.[0]?.text;

  if (!rawContent) {
    console.error('No content returned from Jules API.');
    process.exit(1);
  }

  let reviewResult;
  try {
    reviewResult = JSON.parse(rawContent);
  } catch (e) {
    console.error('Failed to parse response as JSON:', rawContent);
    process.exit(1);
  }

  // 5. Build GitHub Inline Comments (with ```suggestion```)
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
  const summaryBody = summaryHeader + (reviewResult.summary || 'Nenhum problema crítico identificado nesta alteração.');

  // 6. ALWAYS Post a NEW Pull Request Review on GitHub (every commit/alteration gets its own review comment)
  console.log(`Posting NEW PR Review comment for commit ${shortCommit} with ${githubComments.length} inline comment(s)...`);
  
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
      'User-Agent': 'ms-spring-api-jules-reviewer'
    },
    body: JSON.stringify(reviewPayload)
  });

  if (!postReviewResponse.ok) {
    const errBody = await postReviewResponse.text();
    console.error(`Failed to post PR review: ${postReviewResponse.status} ${errBody}`);
    
    // Fallback: If inline comments fail (e.g. line numbers outside diff context), post new summary issue comment
    console.log('Posting new issue comment fallback...');
    const fallbackCommentUrl = `https://api.github.com/repos/${repoFullName}/issues/${prNumber}/comments`;
    let fallbackText = `${summaryBody}`;
    if (githubComments.length > 0) {
      fallbackText += `\n\n---\n### 📌 Sugestões da Alteração:\n` + 
        githubComments.map(c => `**${c.path} (L${c.line}):**\n${c.body}`).join('\n\n');
    }
    
    const fallbackResponse = await fetch(fallbackCommentUrl, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Accept': 'application/vnd.github.v3+json',
        'Content-Type': 'application/json',
        'User-Agent': 'ms-spring-api-jules-reviewer'
      },
      body: JSON.stringify({ body: fallbackText })
    });

    if (!fallbackResponse.ok) {
      console.error('Fallback comment post also failed.');
      process.exit(1);
    }
  }

  console.log(`Successfully posted new Jules Code Review comment for commit ${shortCommit}!`);
}

main().catch(err => {
  console.error('Unhandled error during Jules code review:', err);
  process.exit(1);
});
