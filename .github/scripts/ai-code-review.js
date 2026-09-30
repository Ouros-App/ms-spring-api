const fs = require('fs');
const path = require('path');

const JULES_API_BASE = 'https://jules.googleapis.com/v1alpha';

async function getJulesSource(apiKey, repoFullName) {
  try {
    console.log(`Consultando sources conectadas no Jules para ${repoFullName}...`);
    const response = await fetch(`${JULES_API_BASE}/sources`, {
      headers: {
        'x-goog-api-key': apiKey,
        'Content-Type': 'application/json'
      }
    });

    if (response.ok) {
      const data = await response.json();
      const sources = data.sources || [];
      const match = sources.find(s => 
        s.name?.toLowerCase().includes(repoFullName.toLowerCase()) || 
        s.githubRepo?.repository?.toLowerCase() === repoFullName.toLowerCase()
      );
      if (match) {
        console.log(`Source encontrada no Jules: ${match.name}`);
        return match.name;
      }
    } else {
      console.warn(`Não foi possível listar sources (${response.status}). Usando formato padrão.`);
    }
  } catch (err) {
    console.warn('Erro ao listar sources do Jules:', err.message);
  }

  return `sources/github/${repoFullName}`;
}

async function fetchPrFiles(token, repoFullName, prNumber) {
  if (!token) return [];
  try {
    const res = await fetch(`https://api.github.com/repos/${repoFullName}/pulls/${prNumber}/files?per_page=100`, {
      headers: {
        'Authorization': `Bearer ${token}`,
        'Accept': 'application/vnd.github.v3+json',
        'User-Agent': 'ms-spring-api-jules-action'
      }
    });
    if (res.ok) {
      return await res.json();
    }
  } catch (err) {
    console.warn('Erro ao buscar arquivos da PR:', err.message);
  }
  return [];
}

async function cancelActiveSessionsForPr(apiKey, repoFullName, prNumber) {
  try {
    const res = await fetch(`${JULES_API_BASE}/sessions`, {
      headers: {
        'x-goog-api-key': apiKey,
        'Content-Type': 'application/json'
      }
    });

    if (res.ok) {
      const data = await res.json();
      const sessions = data.sessions || [];
      const prefix = `PR #${prNumber}`;
      for (const s of sessions) {
        const state = (s.state || '').toUpperCase();
        if (s.title?.includes(prefix) && (state === 'ACTIVE' || state === 'IN_PROGRESS' || state === 'QUEUED')) {
          console.log(`Cancelando sessão ativa anterior no Jules: ${s.name} (${s.title})...`);
          try {
            await fetch(`${JULES_API_BASE}/${s.name}`, {
              method: 'DELETE',
              headers: {
                'x-goog-api-key': apiKey,
                'Content-Type': 'application/json'
              }
            });
          } catch (deleteErr) {
            console.warn(`Aviso ao cancelar sessão ${s.name}:`, deleteErr.message);
          }
        }
      }
    }
  } catch (err) {
    console.warn('Erro ao verificar sessões anteriores no Jules:', err.message);
  }
}

async function fetchReviewThreads(token, repoFullName, prNumber) {
  if (!token) return { threads: [], resolved: [], unresolved: [] };
  const [owner, repo] = repoFullName.split('/');
  const query = `
    query($owner: String!, $repo: String!, $pr: Int!) {
      repository(owner: $owner, name: $repo) {
        pullRequest(number: $pr) {
          reviewThreads(first: 100) {
            nodes {
              id
              isResolved
              isOutdated
              path
              line
              originalLine
              comments(first: 10) {
                nodes {
                  id
                  databaseId
                  body
                  author {
                    login
                  }
                  createdAt
                }
              }
            }
          }
        }
      }
    }
  `;

  try {
    const res = await fetch('https://api.github.com/graphql', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json',
        'User-Agent': 'ms-spring-api-jules-action'
      },
      body: JSON.stringify({ query, variables: { owner, repo, pr: parseInt(prNumber, 10) } })
    });

    if (res.ok) {
      const data = await res.json();
      const nodes = data?.data?.repository?.pullRequest?.reviewThreads?.nodes || [];
      const resolved = nodes.filter(n => n.isResolved);
      const unresolved = nodes.filter(n => !n.isResolved);
      return { threads: nodes, resolved, unresolved };
    } else {
      console.warn(`Não foi possível buscar review threads via GraphQL (${res.status}).`);
    }
  } catch (err) {
    console.warn('Erro ao consultar review threads via GraphQL:', err.message);
  }

  return { threads: [], resolved: [], unresolved: [] };
}

async function waitForJulesReviewCompletion(apiKey, token, repoFullName, prNumber, sessionName, startTime, maxWaitMinutes = 15) {
  const pollIntervalMs = 8000; // 8s
  const maxAttempts = Math.ceil((maxWaitMinutes * 60 * 1000) / pollIntervalMs);
  let attempts = 0;

  console.log(`\nAguardando conclusão da revisão do Jules (${sessionName})...`);

  while (attempts < maxAttempts) {
    await new Promise(resolve => setTimeout(resolve, pollIntervalMs));
    attempts++;

    // 1. Verificar se a PR recebeu um novo Review no GitHub (indicador direto de término)
    if (token) {
      try {
        const reviewsRes = await fetch(`https://api.github.com/repos/${repoFullName}/pulls/${prNumber}/reviews?per_page=10`, {
          headers: {
            'Authorization': `Bearer ${token}`,
            'Accept': 'application/vnd.github.v3+json',
            'User-Agent': 'ms-spring-api-jules-action'
          }
        });
        if (reviewsRes.ok) {
          const reviews = await reviewsRes.json();
          const recentReview = reviews.find(r => {
            const t = new Date(r.submitted_at || 0).getTime();
            return t >= (startTime - 5000);
          });
          if (recentReview) {
            console.log(`🎉 Novo Review detectado no GitHub (ID: ${recentReview.id}, State: ${recentReview.state})! Concluído com sucesso.`);
            return { type: 'GITHUB_REVIEW_POSTED', review: recentReview };
          }
        }
      } catch (ghErr) {
        console.warn(`[Poll #${attempts}] Aviso ao verificar reviews no GitHub:`, ghErr.message);
      }
    }

    // 2. Consultar status da Session no Jules
    try {
      const response = await fetch(`${JULES_API_BASE}/${sessionName}`, {
        headers: {
          'x-goog-api-key': apiKey,
          'Content-Type': 'application/json'
        }
      });

      if (response.ok) {
        const data = await response.json();
        const state = (data.state || '').toUpperCase();
        console.log(`[Poll #${attempts} | ${attempts * 8}s] Status da sessão no Jules: ${state}`);

        if (state === 'COMPLETED' || state === 'SUCCEEDED') {
          console.log('🎉 Sessão do Jules concluída com sucesso!');
          return data;
        }

        if (state === 'FAILED' || state === 'CANCELLED' || state === 'ERRORED') {
          throw new Error(`A sessão do Jules finalizou com status: ${state}`);
        }
      }
    } catch (err) {
      if (err.message.includes('finalizou com status')) {
        throw err;
      }
      console.warn(`[Poll #${attempts}] Erro momentâneo de conexão: ${err.message}`);
    }

    // 3. Consultar activities da Session no Jules (identificar resposta/conclusão no chat)
    try {
      const actRes = await fetch(`${JULES_API_BASE}/${sessionName}/activities`, {
        headers: {
          'x-goog-api-key': apiKey,
          'Content-Type': 'application/json'
        }
      });
      if (actRes.ok) {
        const actData = await actRes.json();
        const activities = actData.activities || [];
        const agentResponse = activities.find(a => a.agentMessage || a.agent_message || a.type === 'AGENT_MESSAGE');
        if (agentResponse) {
          console.log('🎉 Resposta do agente Jules detectada nas activities da sessão! Concluído com sucesso.');
          return agentResponse;
        }
      }
    } catch (actErr) {
      // ignorar erros transitórios de activities
    }
  }

  console.warn(`Aviso: Tempo limite atingido (${maxWaitMinutes} minutos) aguardando o Jules.`);
  return null;
}

async function main() {
  const token = process.env.GITHUB_TOKEN;
  const julesApiKey = process.env.JULES_API_KEY;
  const repoFullName = process.env.GITHUB_REPOSITORY; // e.g. "Ouros-App/ms-spring-api"
  const prNumber = process.env.PR_NUMBER;
  const prBranch = process.env.PR_BRANCH;
  const commitSha = process.env.COMMIT_SHA;

  if (!julesApiKey) {
    console.error('Erro: JULES_API_KEY não foi encontrada nos secrets do repositório.');
    process.exit(1);
  }

  if (!repoFullName || !prNumber || !prBranch) {
    console.error('Erro: Variáveis obrigatórias ausentes (GITHUB_REPOSITORY, PR_NUMBER, PR_BRANCH).');
    process.exit(1);
  }

  const shortCommit = commitSha ? commitSha.substring(0, 7) : 'latest';
  console.log(`Iniciando sessão do Jules para Code Review da PR #${prNumber} (Branch: ${prBranch}, Commit: ${shortCommit})...`);

  // 1. Carregar conteúdo completo da Skill revisor-de-codigo
  let skillContent = '';
  const skillPath = path.join(process.cwd(), '.github', 'skills', 'revisor-de-codigo', 'SKILL.md');
  if (fs.existsSync(skillPath)) {
    skillContent = fs.readFileSync(skillPath, 'utf8');
  }

  // 2. Obter título, descrição, arquivos alterados e histórico de threads da PR via GitHub API
  let prTitle = '';
  let prBody = '';
  let prFiles = [];
  let threadsData = { threads: [], resolved: [], unresolved: [] };

  if (token) {
    try {
      const prResponse = await fetch(`https://api.github.com/repos/${repoFullName}/pulls/${prNumber}`, {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Accept': 'application/vnd.github.v3+json',
          'User-Agent': 'ms-spring-api-jules-action'
        }
      });
      if (prResponse.ok) {
        const prData = await prResponse.json();
        prTitle = prData.title || '';
        prBody = prData.body || '';
      }
    } catch (err) {
      console.warn('Não foi possível obter os metadados da PR:', err.message);
    }

    // Buscar lista de arquivos modificados e patches da PR
    prFiles = await fetchPrFiles(token, repoFullName, prNumber);
    console.log(`Arquivos modificados no PR: ${prFiles.length}`);

    // Buscar histórico de review threads
    threadsData = await fetchReviewThreads(token, repoFullName, prNumber);
    console.log(`Threads de revisão encontradas: ${threadsData.threads.length} (Resolvidas: ${threadsData.resolved.length}, Pendentes: ${threadsData.unresolved.length})`);
  }

  // 3. Montar contexto de arquivos alterados e patches
  let changedFilesContext = '';
  if (prFiles.length > 0) {
    const filesList = prFiles.map(f => `- \`${f.filename}\` (${f.status}, +${f.additions} -${f.deletions})`).join('\n');
    let diffContent = '';
    let accumulatedLength = 0;
    for (const f of prFiles) {
      if (f.patch && accumulatedLength + f.patch.length < 25000) {
        diffContent += `\n--- Diff de \`${f.filename}\` ---\n${f.patch}\n`;
        accumulatedLength += f.patch.length;
      }
    }

    changedFilesContext = `
=== ARQUIVOS ALTERADOS NO DIFF ATUAL ===
Total de arquivos modificados: ${prFiles.length}
${filesList}

${diffContent ? `=== PATCH / DIFF DETALHADO DAS ALTERAÇÕES ===\n\`\`\`diff\n${diffContent}\n\`\`\`` : ''}
`;
  }

  // 4. Montar contexto de histórico de revisões anteriores
  let previousFeedbackContext = '';
  if (threadsData.threads.length > 0) {
    const resolvedList = threadsData.resolved.map(t => {
      const firstComment = t.comments?.nodes?.[0]?.body || 'Sem conteúdo';
      const firstLine = firstComment.split('\n')[0].replace(/###\s*/, '').trim();
      return `- [CORRIGIDO/RESOLVIDO] Arquivo: \`${t.path}\` (Linha: ${t.line || t.originalLine || 'N/A'}) — ${firstLine}`;
    }).join('\n');

    const unresolvedList = threadsData.unresolved.map(t => {
      const firstComment = t.comments?.nodes?.[0]?.body || 'Sem conteúdo';
      const firstLine = firstComment.split('\n')[0].replace(/###\s*/, '').trim();
      return `- [PENDENTE/ABERTO] Arquivo: \`${t.path}\` (Linha: ${t.line || t.originalLine || 'N/A'}) — ${firstLine}`;
    }).join('\n');

    previousFeedbackContext = `
=== HISTÓRICO DE APONTAMENTOS E REVISÕES ANTERIORES ===
Esta Pull Request possui histórico de revisões anteriores. Abaixo está a relação dos apontamentos:

${resolvedList ? `**Apontamentos Anteriores Já Corrigidos / Resolvidos:**\n${resolvedList}\n` : ''}
${unresolvedList ? `**Apontamentos Ainda Pendentes / Abertos:**\n${unresolvedList}\n` : ''}

**DIRETRIZES DE REVISÃO INCREMENTAL E RECONHECIMENTO DE CORREÇÕES:**
1. **Validar Correções:** Verifique se as alterações do commit atual atenderam satisfatoriamente aos apontamentos anteriores.
2. **Especificar Pontos Corrigidos no Resumo:** No campo "body" da sua revisão, mencione expressamente e em destaque que os apontamentos anteriores foram corrigidos com sucesso no código atual.
3. **Não Duplicar Apontamentos Já Sanados:** Se os problemas anteriores foram sanados, NÃO crie novos comentários para eles.
4. **Foco no Diff Atual:** Concentre novos apontamentos exclusivamente em problemas reais ainda não resolvidos ou novos bugs inseridos no último diff. Se todos os pontos foram corrigidos e o código estiver em conformidade com o AGENTS.md, aprove a PR com "APPROVE"!
`;
  }

  // 5. Cancelar eventuais sessões anteriores ainda ativas para esta PR no Jules
  await cancelActiveSessionsForPr(julesApiKey, repoFullName, prNumber);

  // 6. Resolver Source no Jules
  const sourceName = await getJulesSource(julesApiKey, repoFullName);

  // 7. Montar Prompt com as instruções completas da Skill diretamente no texto
  const prompt = `Você é o Jules atuando como o Engenheiro Revisor de Código Sênior para a Pull Request #${prNumber} no repositório ${repoFullName}.

=== INFORMAÇÕES DA PULL REQUEST ===
- **Número da PR:** #${prNumber}
- **Branch:** ${prBranch}
- **Commit Atual:** ${shortCommit}
- **Título:** ${prTitle || 'Sem título'}
- **Descrição da PR (Objetivo, Contexto e Escopo):**
${prBody || 'Nenhuma descrição detalhada fornecida.'}
${changedFilesContext}
${previousFeedbackContext}

=== MENTALIDADE E DIRETRIZES DA REVISÃO ===
1. **Postura Construtiva, Propositiva e Pragmática:**
   - Atue como parceiro e colaborador do autor da PR. Valorize boas decisões e implementações limpas.
   - **NÃO crie falsos problemas:** Não faça apontamentos sobre preferências puramente pessoais de estilo, nomes subjetivos ou formatação se o código atender com clareza ao \`AGENTS.md\`.
   - **Obrigatoriedade de Sugestão de Código:** Para QUALQUER apontamento ou melhoria, forneça OBRIGATORIAMENTE o bloco \`\`\`suggestion com o código exato de substituição para aplicação em 1 clique.
2. **Escopo e Foco:**
   - Analise exclusivamente o diff modificado na branch em relação à \`main\`. Não aponte problemas em código preexistente intocado.
   - Não execute testes no Gradle durante a revisão (a análise é estática/cognitiva).
3. **Padrões do Projeto (AGENTS.md):**
   - Java 17 LTS, Spring Boot 3.4.0.
   - Código Java em inglês; mensagens de erro, validações e documentação OpenAPI em Português (PT-BR).
   - DTOs em Java \`record\` com compact constructor e anotações \`@JsonProperty\`/\`@JsonAlias\`.
   - RBAC com \`@AuthenticationPrincipal UserPrincipal\` e checagem de perfis (\`ADM\`, \`COMPANY_EMPLOYEE\`, \`FARM_OWNER\`).
   - Testes unitários/WebMvc utilizando \`@MockitoBean\` (Spring Boot 3.4).

=== FORMATO OBRIGATÓRIO DE PUBLICAÇÃO VIA GITHUB API ===
Você possui a variável de ambiente \`GITHUB_TOKEN\` disponível no seu ambiente.
Você deve publicar a revisão usando a API oficial de Pull Request Reviews do GitHub (\`POST /repos/${repoFullName}/pulls/${prNumber}/reviews\`).

REGRAS CRÍTICAS DE EXECUÇÃO:
1. Faça **EXATAMENTE UMA ÚNICA CHAMADA** para a API de Reviews. NUNCA execute em loop e NUNCA crie múltiplos reviews separados.
2. Cada apontamento DEVE conter a explicação em PT-BR acompanhada da sugestão pronta no formato \`\`\`suggestion.
3. Escolha exatamente um dos cenários abaixo para montar o arquivo \`review_payload.json\`:

---

### 🔴 CENÁRIO 1: IDENTIFICADOS APONTAMENTOS OU SUGESTÕES DE MELHORIA
Defina \`"event": "COMMENT"\`, adicione o checkbox único no \`"body"\` e reúna **TODOS os apontamentos no array "comments"** (cada um com seu bloco \`\`\`suggestion):

\`\`\`bash
cat << 'EOF' > review_payload.json
{
  "commit_id": "${commitSha || ''}",
  "body": "Revisão detalhada de código realizada com base nas diretrizes do \`AGENTS.md\`. Seguem os apontamentos identificados com sugestões prontas para aplicação em 1 clique.\\n\\n- [ ] **Corrigir todos os apontamentos automaticamente**",
  "event": "COMMENT",
  "comments": [
    {
      "path": "src/main/java/com/ourosapp/springapi/exemplo/Arquivo.java",
      "line": 42,
      "side": "RIGHT",
      "body": "### \`[Sugestão]\` Título objetivo da melhoria\\nExplicação técnica clara em Português (PT-BR) sobre o motivo e impacto.\\n\\n\`\`\`suggestion\\n// Código exato de substituição pronto para 1 clique\\n\`\`\`"
    }
  ]
}
EOF

gh api repos/${repoFullName}/pulls/${prNumber}/reviews --input review_payload.json
\`\`\`

---

### 🟢 CENÁRIO 2: CÓDIGO CORRETO E EM CONFORMIDADE (SEM BUGS OU VIOLAÇÕES)
Se o código estiver de alta qualidade, sem bugs e seguindo os padrões do \`AGENTS.md\` (incluindo as correções de apontamentos anteriores), defina \`"event": "APPROVE"\`, envie o array \`"comments": []\` vazio e **APROVE A PULL REQUEST DIRETAMENTE**:

\`\`\`bash
cat << 'EOF' > review_payload.json
{
  "commit_id": "${commitSha || ''}",
  "body": "Revisão de código realizada com base nas diretrizes do \`AGENTS.md\`. Os pontos levantados anteriormente foram validados e corrigidos com sucesso. A implementação está limpa, segura e em total conformidade. Pull Request aprovada com sucesso! ✅",
  "event": "APPROVE",
  "comments": []
}
EOF

gh api repos/${repoFullName}/pulls/${prNumber}/reviews --input review_payload.json
\`\`\`
`;

  const startTime = Date.now();

  // 6. Criar Sessão no jules.google.com via API Oficial
  console.log(`Disparando POST ${JULES_API_BASE}/sessions no jules.google.com...`);
  const sessionPayload = {
    title: `Code Review PR #${prNumber} - ${shortCommit}`,
    prompt: prompt,
    sourceContext: {
      source: sourceName,
      githubRepoContext: {
        startingBranch: prBranch
      }
    }
  };

  const sessionResponse = await fetch(`${JULES_API_BASE}/sessions`, {
    method: 'POST',
    headers: {
      'x-goog-api-key': julesApiKey,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(sessionPayload)
  });

  if (!sessionResponse.ok) {
    const errorBody = await sessionResponse.text();
    console.error(`Falha ao criar sessão no Jules: ${sessionResponse.status} ${sessionResponse.statusText}`);
    console.error('Detalhes do erro:', errorBody);
    process.exit(1);
  }

  const sessionData = await sessionResponse.json();
  const sessionName = sessionData.name || '';
  console.log('====================================================');
  console.log('🎉 Sessão do Jules criada com sucesso no jules.google.com!');
  console.log(`Session Name: ${sessionName || 'Nova Sessão'}`);
  console.log(`State Inicial: ${sessionData.state || 'ACTIVE'}`);
  console.log('Acompanhe a execução em tempo real em: https://jules.google.com');
  console.log('====================================================');

  // 7. Polling ativo para manter a verificação do GitHub em progresso até o Jules responder
  if (sessionName) {
    await waitForJulesReviewCompletion(julesApiKey, token, repoFullName, prNumber, sessionName, startTime);
  }
}

main().catch(err => {
  console.error('Erro fatal ao disparar sessão do Jules:', err);
  process.exit(1);
});

