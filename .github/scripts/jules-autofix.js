const JULES_API_BASE = 'https://jules.googleapis.com/v1alpha';

async function getJulesSource(apiKey, repoFullName) {
  try {
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
      if (match) return match.name;
    }
  } catch (err) {
    console.warn('Erro ao consultar sources do Jules:', err.message);
  }
  return `sources/github/${repoFullName}`;
}

async function main() {
  const token = process.env.GITHUB_TOKEN;
  const julesApiKey = process.env.JULES_API_KEY;
  const repoFullName = process.env.GITHUB_REPOSITORY;
  const prNumber = process.env.PR_NUMBER;
  const commentBody = process.env.COMMENT_BODY || '';

  if (!julesApiKey || !repoFullName || !prNumber) {
    console.error('Erro: Variáveis obrigatórias ausentes para o Auto-Fix do Jules.');
    process.exit(1);
  }

  // 1. Obter informações da PR via GitHub API (descobrir a branch de origem)
  console.log(`Obtendo detalhes da PR #${prNumber}...`);
  let prBranch = '';
  let prTitle = '';
  let reviewCommentsText = '';

  if (token) {
    try {
      const prResponse = await fetch(`https://api.github.com/repos/${repoFullName}/pulls/${prNumber}`, {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Accept': 'application/vnd.github.v3+json',
          'User-Agent': 'ms-spring-api-jules-autofix'
        }
      });
      if (prResponse.ok) {
        const prData = await prResponse.json();
        prBranch = prData.head?.ref || '';
        prTitle = prData.title || '';
      }

      // Buscar todos os comentários inline de revisão feitos na PR
      console.log(`Buscando apontamentos e sugestões inline da PR #${prNumber}...`);
      const commentsResponse = await fetch(`https://api.github.com/repos/${repoFullName}/pulls/${prNumber}/comments?per_page=100`, {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Accept': 'application/vnd.github.v3+json',
          'User-Agent': 'ms-spring-api-jules-autofix'
        }
      });

      if (commentsResponse.ok) {
        const commentsData = await commentsResponse.json();
        if (Array.isArray(commentsData) && commentsData.length > 0) {
          reviewCommentsText = commentsData.map((c, idx) => {
            const line = c.line || c.original_line || 'N/A';
            return `### Apontamento ${idx + 1}:\n**Arquivo:** \`${c.path}\` (Linha: ${line})\n**Conteúdo / Sugestão:**\n${c.body}`;
          }).join('\n\n---\n\n');
        }
      }
    } catch (err) {
      console.error('Erro ao buscar dados da PR:', err.message);
    }
  }

  if (!prBranch) {
    console.error('Não foi possível identificar a branch da PR para aplicar as correções.');
    process.exit(1);
  }

  console.log(`Disparando Jules Auto-Fix na branch '${prBranch}' da PR #${prNumber}...`);

  // 2. Resolver Source no Jules
  const sourceName = await getJulesSource(julesApiKey, repoFullName);

  // 3. Montar Prompt de Correção para o Jules com a lista completa dos apontamentos
  const prompt = `Você é o Jules atuando como o executor de correções automáticas (Auto-Fix) da Pull Request #${prNumber} no repositório ${repoFullName}.

=== INFORMAÇÕES DA PR ===
- **Número da PR:** #${prNumber}
- **Título:** ${prTitle}
- **Branch de Trabalho:** ${prBranch}

=== LISTA DE APONTAMENTOS E SUGESTÕES PARA CORRIGIR ===
${reviewCommentsText || commentBody || 'Aplique as correções sugeridas na revisão de código da PR.'}

=== DIRETRIZES OBRIGATÓRIAS DE EXECUÇÃO ===
1. **Regras do Projeto:** Leia o arquivo AGENTS.md na raiz do repositório para garantir que qualquer alteração respeite a arquitetura, idioma (código em inglês, mensagens em PT-BR), DTOs record e segurança RBAC.
2. **Aplicar Correções na Branch Existente:**
   - Faça as alterações de código dos apontamentos listados acima diretamente na branch \`${prBranch}\`.
   - **NÃO crie uma nova branch** e **NÃO abra uma nova Pull Request**.
3. **Validação de Testes:**
   - Execute \`./gradlew test\` no seu ambiente para garantir que a compilação e todos os testes automatizados passaram sem erros.
4. **Configuração do Git, Commit e Push (Padrão Obrigatório):**
   - Utilize a variável de ambiente \`GITHUB_TOKEN\` disponível no seu ambiente.
   - Execute os comandos exatamente no seguinte padrão para comitar e enviar as alterações para a branch da PR:

\`\`\`bash
git add -A
git config user.email "jules@google.com" && git config user.name "Jules"
git commit -m "fix(review): aplicar correções solicitadas na PR #${prNumber}"
git push https://x-access-token:\${GITHUB_TOKEN}@github.com/${repoFullName}.git HEAD:refs/heads/${prBranch}
\`\`\`
`;

  // 4. Criar Sessão de Auto-Fix no Jules
  const sessionPayload = {
    title: `Auto-Fix PR #${prNumber} (${prBranch})`,
    prompt: prompt,
    sourceContext: {
      source: sourceName,
      githubRepoContext: {
        startingBranch: prBranch
      }
    }
  };

  const response = await fetch(`${JULES_API_BASE}/sessions`, {
    method: 'POST',
    headers: {
      'x-goog-api-key': julesApiKey,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(sessionPayload)
  });

  if (!response.ok) {
    const errText = await response.text();
    console.error(`Falha ao disparar Auto-Fix no Jules: ${response.status} ${errText}`);
    process.exit(1);
  }

  const sessionData = await response.json();
  console.log('====================================================');
  console.log('🚀 Sessão de Auto-Fix disparada com sucesso no Jules!');
  console.log(`Session Name: ${sessionData.name}`);
  console.log(`Branch Alvo: ${prBranch}`);
  console.log('Acompanhe a aplicação das correções em tempo real em: https://jules.google.com');
  console.log('====================================================');
}

main().catch(err => {
  console.error('Erro fatal no script de Auto-Fix:', err);
  process.exit(1);
});
