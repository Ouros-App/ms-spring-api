# Metas compartilhadas por UF

Sem migração do banco. A UF é obtida de addresses.state, associada por farms.id_address.
Somente endereços do Brasil são elegíveis. farms.region não determina a UF.

GET /state-goals/audience?state=SP&id_enterprise=1 retorna state, id_enterprise e
participants_count. Funcionários podem omitir empresa; ADM deve escolher uma empresa.
O funcionário fica restrito à empresa do seu cadastro e produtores não podem lançar.

POST /state-goals/by-state recebe:

```json
{
  "state": "SP",
  "id_enterprise": 1,
  "goal": {
    "title": "Realizar manutenção preventiva",
    "date_creation": "2026-10-31",
    "date_end": "2027-01-31"
  }
}
```

O objeto goal recebe apenas título, descrição opcional e datas. A API define
TASK_COMPLETION e target_value=1: uma unidade de conclusão por granja, acompanhada
pelo status, sem alvo numérico no formulário. O servidor seleciona as granjas por UF/empresa. Uma transação cria state_goals, todos os farm_goals
com status Pendente e o vínculo da UF em regions_goals/state_goal_regions. Resposta
201 com Location /state-goals/{id}, id, state, id_enterprise e participants_count real.
Uma UF sem granjas retorna 400 sem criar meta. A criação recalcula o público;
a contagem prévia não é garantia de quantidade. Novas granjas não entram automaticamente.
O progresso de cada produtor permanece vinculado à sua granja, não a cada pessoa.

Implantar a API antes do web. Os endpoints legados continuam disponíveis.
Testes cobrem contrato HTTP, RBAC, seleção pelo endereço, status independente e rollback.
O teste local do Gradle foi tentado e bloqueado por Unable to establish loopback connection;
a CI deve executar clean build jacocoTestReport antes do merge.
