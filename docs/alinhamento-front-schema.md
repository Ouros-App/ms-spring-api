# Alinhamento do web com o esquema existente

O esquema SQL é mantido. A aplicação não depende mais de `create_state_goal`,
`calculate_water_consumption` ou `calculate_goals_progress`.

- Criação de meta: uma transação JPA grava `state_goals`, `farm_goals` e, quando
  houver região, `regions_goals` e `state_goal_regions`.
- Consumo recente: diferença entre final e início da última medição da fazenda;
  desempate por ID. A unidade é m³. Sem medição, retorna zero.
- Progresso: percentual dos vínculos em `farm_goals` cujo status representa
  conclusão. Sem vínculos, retorna zero. Não representa progresso parcial da métrica.
- `PATCH /company-employees/me`: resolve a tabela pelo perfil autenticado;
  administradores não atualizam funcionários com o mesmo ID. A alteração de senha
  deve usar o serviço de autenticação. Nome e foto são persistidos.
- A edição de metas aceita título e mantém a restrição de produtores a status.

## Limites do esquema

`water_registries` referencia apenas a fazenda. O resumo de um lote contém o consumo
acumulado da fazenda, repetido nos lotes dela; não deve ser somado entre lotes nem
apresentado como consumo exclusivo de um ciclo. A leitura inicial do cadastro agregado
cria uma medição com início igual ao final, portanto consumo inicial zero.

`poultry_capacity` e `place` são campos legados transitórios. Não podem ser usados
como dados persistidos; utilize `area_property`, `region`, `chickens_now` e `id_address`.
Não há coluna de criação de lote: `delivery_date` é data de entrega.

## Implantação e validação

Implantar a API antes do front, que passa a usar PATCH /company-employees/me.
Foram adicionados testes de repositório sem procedures, contrato web e isolamento de
perfil. Neste ambiente Gradle falha ao iniciar com `Unable to establish loopback connection`,
inclusive com IPv4 e execução elevada. Compilação, testes Java e JaCoCo dependem de CI.
A homologação com PostgreSQL e tokens reais ADM/COMPANY_EMPLOYEE continua necessária.
Nenhum teste autenticado contra ambiente remoto foi realizado.
