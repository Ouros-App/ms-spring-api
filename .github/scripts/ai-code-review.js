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

  // 1. Obter título e descrição da PR via GitHub API
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

  // 2. Resolver Source no Jules
  const sourceName = await getJulesSource(julesApiKey, repoFullName);

  // 3. Montar Prompt Estrito para o Jules (Review com comentários inline reais no GitHub)
  const prompt = `Você é o Jules executando o Code Review da Pull Request #${prNumber} no repositório ${repoFullName}.

=== INFORMAÇÕES DA PULL REQUEST ===
- **Número da PR:** #${prNumber}
- **Branch:** ${prBranch}
- **Commit Atual:** ${shortCommit}
- **Título:** ${prTitle || 'Sem título'}
- **Descrição da PR:**
${prBody || 'Nenhuma descrição detalhada fornecida.'}

=== DIRETRIZES DE REVISÃO (AGENTS.md & SKILL) ===
1. Siga rigorosamente as diretrizes em AGENTS.md e .github/skills/revisor-de-codigo/SKILL.md.
2. Analise exclusivamente as linhas e arquivos modificados no diff da branch ${prBranch} em relação à main.
3. NÃO faça resumos longos no texto principal. NÃO crie nova PR e NÃO abra nova branch.

=== FORMATO OBRIGATÓRIO DE PUBLICAÇÃO VIA GITHUB API ===
Você possui a variável de ambiente \`GITHUB_TOKEN\` disponível no seu ambiente.
Você deve publicar a revisão usando a API oficial de Pull Request Reviews do GitHub (\`POST /repos/${repoFullName}/pulls/${prNumber}/reviews\`).

Crie um arquivo JSON chamado \`review_payload.json\` e envie via \`gh api\`:
\`\`\`bash
cat << 'EOF' > review_payload.json
{
  "commit_id": "${commitSha || ''}",
  "body": "Revisão detalhada de código realizada com base nas diretrizes do \`AGENTS.md\`. Seguem os apontamentos identificados com sugestões prontas para aplicação em 1 clique.\\n\\n- [ ] **Corrigir todos os apontamentos automaticamente**",
  "event": "COMMENT",
  "comments": [
    {
      "path": "caminho/do/arquivo.java",
      "line": 42,
      "side": "RIGHT",
      "body": "### \`[Sugestão]\` Título do problema\\nExplicação técnica objetiva do problema e impacto em Português.\\n\\n\`\`\`suggestion\\ncódigo exato de substituição\\n\`\`\`"
    }
  ]
}
EOF

gh api repos/${repoFullName}/pulls/${prNumber}/reviews --input review_payload.json
\`\`\`

Atenção:
- O campo "body" principal do review DEVE SER EXATAMENTE o texto curto com o checkbox:
  "Revisão detalhada de código realizada com base nas diretrizes do \`AGENTS.md\`. Seguem os apontamentos identificados com sugestões prontas para aplicação em 1 clique.\\n\\n- [ ] **Corrigir todos os apontamentos automaticamente**"
- Cada problema identificado DEVE estar no array "comments" apontando para o "path" e "line" corretos no diff, com o bloco de substituição \`\`\`suggestion.
- Se não houver nenhum problema identificado no diff, envie o array "comments" vazio e o "body" como: "Revisão de código realizada com base nas diretrizes do \`AGENTS.md\`. Nenhum problema identificado. Código aprovado!"
`;

  // 4. Criar Sessão no jules.google.com via API Oficial
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
