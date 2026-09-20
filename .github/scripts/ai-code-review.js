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

  // 3. Montar Prompt Estrito para o Jules (Sem criar nova PR, apenas postar comentário)
  const prompt = `Você é o Jules executando a revisão de código no estilo CodeRabbit para a Pull Request #${prNumber} no repositório ${repoFullName}.

=== INFORMAÇÕES DA PULL REQUEST ===
- **Número da PR:** #${prNumber}
- **Branch:** ${prBranch}
- **Commit Atual:** ${shortCommit}
- **Título:** ${prTitle || 'Sem título'}
- **Descrição da PR (Contexto de Negócio e Escopo):**
${prBody || 'Nenhuma descrição detalhada fornecida.'}

=== INSTRUÇÕES OBRIGATÓRIAS DE EXECUÇÃO ===
1. **Regras e Padrões:** Siga rigorosamente o AGENTS.md e a skill em .github/skills/revisor-de-codigo/SKILL.md.
2. **Foco:** Analise exclusivamente o diff das alterações da branch ${prBranch} em relação à main.
3. **NÃO CRIAR NOVA PR NEM NOVA BRANCH:** Você NÃO deve abrir uma nova Pull Request nem criar novas branches.
4. **COMO PUBLICAR O COMENTÁRIO NA PR #${prNumber}:**
   - Você possui a variável de ambiente \`GITHUB_TOKEN\` configurada no seu ambiente.
   - Use o GitHub CLI (\`gh\`) ou cURL com a GitHub API para publicar a sua análise diretamente como comentário na Pull Request #${prNumber}:
     \`\`\`bash
     gh pr comment ${prNumber} --body "<SEU_MARKDOWN_DE_REVISAO>"
     \`\`\`
   - O comentário deve conter:
     - Resumo das alterações da PR
     - Checklist de tarefas com checkboxes (- [ ])
     - Sugestões inline com blocos de 1-clique (\`\`\`suggestion)
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
