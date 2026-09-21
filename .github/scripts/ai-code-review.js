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

  // 2. Obter título e descrição da PR via GitHub API
  let prTitle = '';
  let prBody = '';
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
  }

  // 3. Resolver Source no Jules
  const sourceName = await getJulesSource(julesApiKey, repoFullName);

  // 4. Montar Prompt com as instruções completas da Skill diretamente no texto
  const prompt = `Você é o Jules atuando como o Engenheiro Revisor de Código Sênior para a Pull Request #${prNumber} no repositório ${repoFullName}.

=== INFORMAÇÕES DA PULL REQUEST ===
- **Número da PR:** #${prNumber}
- **Branch:** ${prBranch}
- **Commit Atual:** ${shortCommit}
- **Título:** ${prTitle || 'Sem título'}
- **Descrição da PR (Objetivo, Contexto e Escopo):**
${prBody || 'Nenhuma descrição detalhada fornecida.'}

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
Se o código estiver de alta qualidade, sem bugs e seguindo os padrões do \`AGENTS.md\`, defina \`"event": "APPROVE"\`, envie o array \`"comments": []\` vazio e **APROVE A PULL REQUEST DIRETAMENTE**:

\`\`\`bash
cat << 'EOF' > review_payload.json
{
  "commit_id": "${commitSha || ''}",
  "body": "Revisão de código realizada com base nas diretrizes do \`AGENTS.md\`. A implementação está limpa, segura e em total conformidade. Pull Request aprovada com sucesso! ✅",
  "event": "APPROVE",
  "comments": []
}
EOF

gh api repos/${repoFullName}/pulls/${prNumber}/reviews --input review_payload.json
\`\`\`
`;

  // 5. Criar Sessão no jules.google.com via API Oficial
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
  console.log('====================================================');
  console.log('🎉 Sessão do Jules criada com sucesso no jules.google.com!');
  console.log(`Session Name: ${sessionData.name || 'Nova Sessão'}`);
  console.log(`State: ${sessionData.state || 'ACTIVE'}`);
  console.log('Acompanhe a execução em tempo real em: https://jules.google.com');
  console.log('====================================================');
}

main().catch(err => {
  console.error('Erro fatal ao disparar sessão do Jules:', err);
  process.exit(1);
});
