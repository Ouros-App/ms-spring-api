# Alinhamento do web com o esquema existente

O esquema SQL é mantido. A pasta completa define `create_state_goal` e `create_tip`
em `procedures.sql`, e `calculate_water_consumption` e `calculate_goals_progress`
em `funcoes_consumo_metas.sql`. Essas rotinas existem nos scripts, embora não estejam
no arquivo isolado de tabelas. A presença no banco instalado depende das migrações aplicadas.
A implementação JPA de criação de metas é uma alternativa transacional à procedure existente.

- Criação de meta: uma transação JPA grava `state_goals`, `farm_goals` e, quando
  houver região, `regions_goals` e `state_goal_regions`.
- Consumo recente: diferença entre final e início da última medição da fazenda;
  desempate por ID. A unidade é m³. Sem medição, retorna zero.
- Progresso: média dos status de `individual_goals` e `farm_goals`, conforme a função
  do banco: Concluída = 100, Em andamento = 50, Pendente e desconhecidos = 0.
  Sem metas, retorna zero. A query replica essa regra sem invocar a função.
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

## Revisão da pasta completa de scripts

`create_tip` está definido em procedures.sql e é compatível com as tabelas tips,
farms_tips e tip_categories. Não foi removido.

Há diferenças de contrato ainda relevantes: o banco preserva TIMESTAMP nas medições,
mas a API usa LocalDate; farm_simulations usa NUMERIC(8,4) para CGI/energia (máximo
9999,9999), NUMERIC(10,2) para preços e TIMESTAMPTZ para datas. O DTO usa Double sem
limites de escala e LocalDateTime nas entidades; exemplos como CGI 12000 e preço
0,015 não representam valores persistíveis sem erro ou arredondamento nesse esquema.
O banco não tem unicidade em vários e-mails/documentos que a aplicação trata como únicos.
Não se pode inferir a ordem de instalação nem as migrações aplicadas apenas pelos arquivos.
