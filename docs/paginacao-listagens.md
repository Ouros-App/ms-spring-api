# Paginação das listagens (P22)

As rotas de lista existentes permanecem compatíveis. As novas rotas aceitam `page` (zero por padrão), `size` (10 por padrão, entre 1 e 100), `search`, `sort` e `direction` (`asc` ou `desc`). Página negativa, offset acima de 2147483647 e campo/direção inválidos retornam HTTP 400 com ProblemDetail. Página além do final retorna conteúdo vazio e preserva os totais.

Resposta: `{ content, totalElements, totalPages, page, size }`. `content` mantém os DTOs atuais, inclusive os nomes JSON snake_case dos itens. Os metadados usam camelCase. Filtros, busca sem distinguir maiúsculas e RBAC são aplicados antes da contagem. Caracteres `%` e `_` da busca são literais.

| Rota GET | Filtros | Busca | Campos de sort |
| --- | --- | --- | --- |
| `/lots/production-summaries/paged` | `id_farm`, `id_enterprise` | código L-ID e nome da fazenda | `id`, `hidrometro`, `custo`, `entraram`, `sairam`, `galinhas_atuais`, `data_emissao`, `farm_name` |
| `/individual-goals/paged` | `farm_id` | título e descrição | `id`, `title`, `type`, `status`, `target_value` |
| `/state-goals/paged` | `farm_id`, `region` | título e região principal | `id`, `title`, `type`, `status`, `target_value`, `date_creation`, `date_end`, `region` |
| `/farms/simulations/paged` | `id_farm`, `id_enterprise` | código do lote | `id`, `id_lot`, `current_cgi`, `target_cgi`, `water_price`, `energy_price`, `poultry_quantity`, `target_energy_kwh`, `created_at` |
| `/farms/rankings/paged` | `id_enterprise`, `region` | nome e região | `score`, `farm_name` |

Ordenação padrão: `id desc`; ranking: `score asc`, seguindo a convenção atual (menor CGI primeiro). O ID desempata ordenações relacionais. Redis desempata scores pelo identificador textual da fazenda. `rank_position` representa a posição por score no conjunto filtrado completo, mesmo com ordenação visual por nome ou descendente.

Metas estaduais incluem `statusCounts`, um mapa de status para quantidade de metas no conjunto filtrado completo. Esse agregado permite construir o resumo sem usar apenas `content`. Uma meta com várias fazendas aparece uma vez; seu status e região de fallback vêm do vínculo de menor ID dentro do escopo autorizado. Regiões explicitamente vinculadas têm precedência sobre a região da fazenda. Na busca por região explícita, qualquer vínculo de região pode satisfazer o filtro.

ADM pode filtrar todas as empresas; funcionário fica restrito à sua empresa; produtor fica restrito à sua fazenda. Ranking completo é acessível apenas a ADM e funcionário. Totais respeitam o mesmo escopo dos itens. Não há mudança de schema.

Ranking sem busca usa range/offset no Redis. Consulta ADM global combina os índices das empresas. Busca, região e ordenação por nome constroem índices temporários em blocos de 100, com TTL de cinco minutos e limpeza ao final; seu custo é proporcional ao ranking consultado. A posição continua calculada pelo índice de scores filtrado. Assim como as rotas atuais, cache ausente é recalculado sob demanda. Paginação não oferece snapshot entre requisições durante atualizações concorrentes do ranking ou do banco.

## Integração TypeScript

```ts
interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}
interface StateGoalPageResponse<T> extends PageResponse<T> {
  statusCounts: Record<string, number>;
}
```

Mapear as opções existentes das telas para os nomes JSON acima: `atuais` → `galinhas_atuais`, `consumoAtual` → `current_cgi`, `consumoAlvo` → `target_cgi`, `precoAgua` → `water_price`, `precoEnergia` → `energy_price`, `quantidadeAves` → `poultry_quantity`, `nome` → `farm_name`, `pontuacao` → `score`. O ranking não persiste data de atualização por fazenda; esse campo não pode ser usado para ordenação. O cliente web não é alterado nesta PR de backend.

Verificações: `.\gradlew.bat compileJava compileTestJava`, `.\gradlew.bat test` e `.\gradlew.bat jacocoTestReport`.
