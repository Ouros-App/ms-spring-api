const fs = require('fs');
const path = require('path');

const JULES_API_BASE = 'https://jules.googleapis.com/v1alpha';

async function getJulesSource(apiKey, repoFullName) {
  try {
    console.log(`Querying connected sources on Jules API for ${repoFullName}...`);
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
        console.log(`Found connected Jules source: ${match.name}`);
        return match.name;
      }
    } else {
      console.warn(`Could not list sources (Status: ${response.status}). Using standard source name.`);
    }
  } catch (err) {
    console.warn('Error querying Jules sources:', err.message);
  }

  // Standard fallback format for GitHub sources in Jules
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
    console.error('Error: JULES_API_KEY is not defined in repository secrets.');
    process.exit(1);
  }

  if (!repoFullName || !prNumber || !prBranch) {
    console.error('Error: Missing required environment variables (GITHUB_REPOSITORY, PR_NUMBER, PR_BRANCH).');
    process.exit(1);
  }

  const shortCommit = commitSha ? commitSha.substring(0, 7) : 'latest';
  console.log(`Starting Jules Code Review session for PR #${prNumber} (Branch: ${prBranch}, Commit: ${shortCommit})...`);

  // 1. Fetch PR Title and Description from GitHub API
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
      console.warn('Could not fetch PR metadata from GitHub:', err.message);
    }
  }

  // 2. Resolve Jules Source for the repository
  const sourceName = await getJulesSource(julesApiKey, repoFullName);

  // 3. Build Prompt for Jules Session
  const prompt = `Você é o Jules executando a revisão de código no estilo CodeRabbit para a Pull Request #${prNumber} no repositório ${repoFullName}.

=== INFORMAÇÕES DA PULL REQUEST ===
- **Número da PR:** #${prNumber}
- **Branch:** ${prBranch}
- **Commit Atual:** ${shortCommit}
- **Título:** ${prTitle || 'Sem título'}
- **Descrição da PR (Contexto de Negócio e Escopo):**
${prBody || 'Nenhuma descrição detalhada fornecida.'}

=== DIRETRIZES OBRIGATÓRIAS ===
1. **Contexto:** Todas as informações de negócio e escopo estão na descrição da PR acima e no arquivo AGENTS.md na raiz do projeto.
2. **Skill de Revisão:** Siga rigorosamente as diretrizes em .github/skills/revisor-de-codigo/SKILL.md.
3. **Escopo:** Analise exclusivamente o diff das alterações da branch ${prBranch} em relação à main. Não execute testes no Gradle durante a revisão analítica.
4. **Publicação no GitHub:**
   - Publique a análise diretamente na Pull Request #${prNumber} no GitHub.
   - Inclua um comentário principal de resumo com visão geral e checklist de tarefas (- [ ]).
   - Publique os comentários inline no diff utilizando o formato de sugestão de 1 clique (\`\`\`suggestion).
`;

  // 4. Create Session on jules.google.com via Jules API
  console.log(`Sending POST ${JULES_API_BASE}/sessions to jules.google.com...`);
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
    console.error(`Failed to create Jules session: ${sessionResponse.status} ${sessionResponse.statusText}`);
    console.error('Response details:', errorBody);
    process.exit(1);
  }

  const sessionData = await sessionResponse.json();
  console.log('====================================================');
  console.log('🎉 Sessão do Jules criada com sucesso no jules.google.com!');
  console.log(`Session Name: ${sessionData.name || 'Nova Sessão'}`);
  console.log(`State: ${sessionData.state || 'ACTIVE'}`);
  console.log('Acompanhe a execução em tempo real no painel: https://jules.google.com');
  console.log('====================================================');
}

main().catch(err => {
  console.error('Fatal error triggering Jules session:', err);
  process.exit(1);
});
