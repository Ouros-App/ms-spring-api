# ms-spring-api

<!-- REPO-METADATA:START -->
<div align="center">

[![Repo Size](https://img.shields.io/github/repo-size/Ouros-App/ms-spring-api?style=flat-square&label=REPO%20SIZE)](https://github.com/Ouros-App/ms-spring-api)
[![Languages](https://img.shields.io/github/languages/count/Ouros-App/ms-spring-api?style=flat-square&label=LANGUAGES)](https://github.com/Ouros-App/ms-spring-api/languages)
[![Forks](https://img.shields.io/github/forks/Ouros-App/ms-spring-api?style=flat-square&label=FORKS)](https://github.com/Ouros-App/ms-spring-api/network/members)
[![Issues](https://img.shields.io/github/issues/Ouros-App/ms-spring-api?style=flat-square&label=ISSUES)](https://github.com/Ouros-App/ms-spring-api/issues)
[![Pull Requests](https://img.shields.io/github/issues-pr/Ouros-App/ms-spring-api?style=flat-square&label=PULL%20REQUESTS)](https://github.com/Ouros-App/ms-spring-api/pulls)

[![Java 17](https://img.shields.io/badge/Java-17%20LTS-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot 3.4](https://img.shields.io/badge/Spring%20Boot-3.4.0-6DB33F?style=flat-square&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16%2B-4169E1?style=flat-square&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-NoSQL%20Cache-DC382D?style=flat-square&logo=redis&logoColor=white)](https://redis.io/)
[![SpringDoc OpenAPI](https://img.shields.io/badge/OpenAPI%203-Swagger%20UI-85EA2D?style=flat-square&logo=swagger&logoColor=black)](https://swagger.io/)
[![License MIT](https://img.shields.io/github/license/Ouros-App/ms-spring-api?style=flat-square&label=LICENSE)](LICENSE)

</div>
<!-- REPO-METADATA:END -->

Microsserviço de backend corporativo da plataforma **Ouros**, desenvolvido em **Java 17** com **Spring Boot 3.4**. Atua como a API RESTful central de negócios, governança e persistência relacional do ecossistema avícola, atendendo tanto ao painel administrativo web desktop ([`ms-ouros-front-web`](https://github.com/Ouros-App/ms-ouros-front-web)) quanto ao aplicativo móvel de campo ([`ouros-android-app`](https://github.com/Ouros-App/ouros-android-app)).

> 🌐 **API em Produção:** [https://ms-spring-api.discloud.app](https://ms-spring-api.discloud.app)  
> 📑 **Documentação Interativa Swagger UI:** [https://ms-spring-api.discloud.app/swagger-ui.html](https://ms-spring-api.discloud.app/swagger-ui.html)

---

## 📑 Sumário

- [Visão Geral e Ecossistema](#-visão-geral-e-ecossistema)
- [Arquitetura e Padrões Técnicos](#-arquitetura-e-padrões-técnicos)
- [Módulos e Recursos da API](#-módulos-e-recursos-da-api)
- [Segurança, Autenticação e RBAC](#-segurança-autenticação-e-rbac)
- [Observabilidade e Documentação (Swagger & Prometheus)](#-observabilidade-e-documentação-swagger--prometheus)
- [Tecnologias Utilizadas](#-tecnologias-utilizadas)
- [Estrutura do Projeto](#-estrutura-do-projeto)
- [Configuração de Ambiente](#-configuração-de-ambiente)
- [Instalação, Compilação e Execução](#-instalação-compilação-e-execução)
- [Testes Automatizados e Qualidade](#-testes-automatizados-e-qualidade)
- [Contribuidores e Licença](#-contribuidores-e-licença)

---

## 🔭 Visão Geral e Ecossistema

O **`ms-spring-api`** centraliza o ciclo de vida operacional, governança multi-empresa e persistência transacional da plataforma Ouros. Ele atua conectado de forma coordenada com os demais microsserviços do ecossistema:

```text
                                  ┌────────────────────────┐
                                  │   ms-auth-service      │
                                  │   (Keycloak OAuth2)    │
                                  └───────────┬────────────┘
                                              │ Emite JWT RS256
                                              ▼
┌───────────────────────────┐      ┌────────────────────────┐      ┌───────────────────────────┐
│     ms-ouros-front-web    │ ───► │      ms-spring-api     │ ◄─── │      ouros-android-app    │
│    (Portal Corporativo)   │      │   (Core Business API)  │      │   (App Granjeiro Mobile)  │
└───────────────────────────┘      └───────────┬────────────┘      └───────────────────────────┘
                                               │
               ┌───────────────────────────────┼───────────────────────────────┐
               │                               │                               │
               ▼                               ▼                               ▼
  ┌────────────────────────┐      ┌────────────────────────┐      ┌────────────────────────┐
  │  ms-audience-engine    │      │  PostgreSQL Database   │      │  Redis Cache (Upstash) │
  │ (Segmentação de Dicas) │      │ (Tabelas e Procedures) │      │  (Rankings e Ligas)    │
  └────────────────────────┘      └────────────────────────┘      └────────────────────────┘
```

### Papéis e Responsabilidades:
1. **Governança Multi-tenant:** Gestão isolada de empresas integradoras (`Enterprise`), colaboradores corporativos e produtores integrados (`FarmOwner`).
2. **Ciclo de Vida de Lotes Avícolas (BFF):** Criação, acompanhamento zootécnico diário, saldo de aves restantes (`ChickenLeft`) e encerramento de lotes.
3. **Apontamentos e Histórico de Medições:** Inserção transacional de medições de hidrômetro (água em m³), padrão elétrico (energia em kWh), pesagens e mortalidade.
4. **Metas Zootécnicas e Gamificação:** Metas estaduais paginadas por empresa integradora, metas individuais de produtores e ranking de fazendas com cache em Redis.
5. **Notificações em Tempo Real:** Disparo de eventos de domínio assíncronos (ex: `LotCreatedEvent`) para emissão de push notifications via **Firebase Cloud Messaging (FCM)**.
6. **Enriquecimento de Recomendações Agronômicas:** Integração com o `ms-ouros-audience-engine` para sinalizar dicas relevantes ao perfil do produtor (`audience_match`).

---

## 🏗️ Arquitetura e Padrões Técnicos

A aplicação é construída seguindo os padrões clássicos de **Clean Architecture** e princípios do ecossistema Spring:

- **Camada Controller (REST):** Controladores anotados com `@RestController`, `@RequestMapping` e anotações do SpringDoc OpenAPI (`@Operation`, `@ApiResponse`, `@Tag`).
- **Camada Service:** Regras de negócio transacionais (`@Transactional`), orquestração de chamadas externas e enriquecimento de dados.
- **Camada Repository:** Interfaces estendendo `JpaRepository` com consultas otimizadas (JPQL, Spring Data Projections e métodos derivados).
- **Domain Events & Listeners:** Padrão Publisher/Subscriber desacoplado (`ApplicationEventPublisher`), utilizado para disparar notificações automáticas quando novos lotes avícolas são criados (`LotNotificationListener`).
- **Validação de DTOs:** Validação declarativa via Bean Validation / Jakarta (`@Valid`, `@NotNull`, `@NotBlank`, `@Positive`, `@Size`).
- **Persistência Relacional Segura:** Mapeamento JPA com `hibernate.ddl-auto=none`, mantendo o esquema PostgreSQL sob estrito controle de governança.

---

## 🚀 Módulos e Recursos da API

A API disponibiliza mais de 25 controladores especializados cobrindo todas as áreas de negócio:

### 1. 🏢 Governança Corporativa e Usuários
| Recurso | Rota Base | Descrição |
|---|---|---|
| **Empresas Integradoras** | `/enterprises` | Cadastro, atualização e consulta de empresas integradoras avícolas. |
| **Colaboradores Corporativos** | `/company-employees` | Gestão de administradores, veterinários e técnicos das integradoras. |
| **Planos Corporativos** | `/plans` & `/enterprise-plans` | Catálogo de planos de assinatura do sistema e contratações. |
| **Faturamento e Pagamentos** | `/payments` | Registro e conciliação de pagamentos das empresas clientes. |

### 2. 🚜 Fazendas e Produtores Rurais
| Recurso | Rota Base | Descrição |
|---|---|---|
| **Fazendas / Granjas** | `/farms` | Unidades produtivas avícolas vinculadas a integradoras e produtores. |
| **Produtores Rurais** | `/farm-owners` | Cadastro, primeiro acesso e dados cadastrais dos integrados. |
| **Endereços** | `/addresses` | Localização geográfica e endereços de fazendas e unidades corporativas. |

### 3. 🐥 Lotes Avícolas e Zootecnia
| Recurso | Rota Base | Descrição |
|---|---|---|
| **Lotes Avícolas (BFF)** | `/lots` | Acompanhamento completo, listagens paginadas, detalhe agregado e emissão de eventos. |
| **Aves Restantes e Mortalidade** | `/chicken-left` | Controle diário de baixas, descartes e mortalidade de lotes. |
| **Consumo de Hidrômetro** | `/water-registries` | Apontamento e histórico de consumo de água em metros cúbicos (m³). |
| **Consumo de Energia** | `/energy-registries` | Apontamento e histórico de consumo elétrico em kWh com precisão decimal. |
| **Importação de Histórico** | `/measurement-imports` | Lote transacional de até 500 registros de medições revisados via IA. |

### 4. 🎯 Metas, Ranking e Simulações
| Recurso | Rota Base | Descrição |
|---|---|---|
| **Metas Estaduais** | `/state-goals` | Metas corporativas com listagem paginada e filtro por empresa (`/state-goals/paged`). |
| **Metas por UF** | `/state-goals/by-state` | Consulta de metas ativas e elegíveis agrupadas por estado da federação. |
| **Metas Individuais** | `/individual-goals` | Metas customizadas de produtores para conversão alimentar e ganho de peso. |
| **Ranking de Produtores** | `/farm-rankings` | Classificação de eficiência, ligas (Ferro, Cobre, Bronze, Prata, Ouro) e cache Redis. |
| **Simulações de Cenários** | `/farm-simulations` | Histórico e persistência de simulações preditivas de produtividade e custos. |

### 5. 💡 Conhecimento, Comunicação e Auditoria
| Recurso | Rota Base | Descrição |
|---|---|---|
| **Dicas Agronômicas** | `/tips` & `/categories` | Recomendações de manejo com verificação dinâmica de audiência (`audience_match`). |
| **Notificações Push (FCM)** | `/notifications` | Histórico, leitura e disparo de mensagens push a granjeiros. |
| **Auditoria e Avaliações** | `/reviews` | Avaliações técnicas e auditorias zootécnicas de lotes e granjas. |
| **APIs Internas M2M** | `/internal/v1/**` | Endpoints protegidos para sincronização direta com `ms-auth-service`. |

---

## 🔒 Segurança, Autenticação e RBAC

A segurança do `ms-spring-api` é implementada com **Spring Security 6** no modelo **OAuth2 Resource Server**:

- **Validação de Token JWT:** Validação de tokens RS256 assinados pelo Keycloak através do endpoint de certificados JWK Set (`/protocol/openid-connect/certs`).
- **Mapeamento de Claims e Roles (`account_type`):** O `KeycloakJwtAuthenticationConverter` extrai as claims de perfil do usuário e as mapeia para autoridades do Spring Security:
  - `ROLE_SUPER_ADMIN` / `ROLE_ADMIN`: Acesso global e governança corporativa multi-tenant.
  - `ROLE_FARM_OWNER`: Acesso restrito às granjas, lotes e metas associadas ao produtor rural autenticado.
- **Comunicação Segura M2M:** Os endpoints sob `/internal/v1/**` exigem o cabeçalho `X-Internal-Service-Key` ou `INTERNAL_SERVICE_SECRET`, assegurando que apenas microsserviços internos autorizados realizem operações sensíveis.
- **Scraping Protegido de Métricas:** A rota `/metrics` do Prometheus exige token OAuth2 emitido para a service account autorizada (`METRICS_KEYCLOAK_AUTHORIZED_PARTY=ouros-prometheus`).

---

## 📊 Observabilidade e Documentação (Swagger & Prometheus)

### Documentação Interativa com OpenAPI 3 / Swagger UI
A API fornece documentação navegável completa de todos os endpoints, parâmetros, contratos de request/response e esquemas de dados:
- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON Spec:** `http://localhost:8080/v3/api-docs`

### Métricas e Observabilidade com Prometheus
Integrado com o **Spring Boot Actuator** e **Micrometer**:
- **Endpoint de Métricas:** `http://localhost:8080/metrics`
- **Métricas Exportadas:** Distribuição de requisições HTTP (`http.server.requests`), percentis de latência (p50, p90, p95, p99), uso de memória da JVM, conexões com PostgreSQL e pool de threads.

---

## 🛠️ Tecnologias Utilizadas

| Componente | Tecnologia | Finalidade |
|---|---|---|
| **Linguagem & Framework** | Java 17 LTS / [Spring Boot 3.4.0](https://spring.io/projects/spring-boot) | Core da API RESTful de alta performance |
| **Persistência Relacional** | [Spring Data JPA](https://spring.io/projects/spring-data-jpa) / [Hibernate 6](https://hibernate.org/) | Mapeamento objeto-relacional (ORM) e repositórios |
| **Banco de Dados Relacional** | [PostgreSQL Driver 42+](https://jdbc.postgresql.org/) | Persistência transacional dos dados de negócio |
| **Cache & NoSQL** | [Spring Data Redis](https://spring.io/projects/spring-data-redis) (Jedis/Lettuce) | Cache distribuído para ranking e contagens de ligas |
| **Segurança & Tokens** | [Spring Security 6](https://spring.io/projects/spring-security) / OAuth2 Resource Server | Validação JWT Keycloak RS256 e RBAC |
| **Documentação de API** | [SpringDoc OpenAPI 2.8.5](https://springdoc.org/) | Geração automática de Swagger UI e schemas |
| **Notificações Push** | [Firebase Admin SDK 9.4](https://firebase.google.com/docs/admin/setup) | Disparo de alertas móveis via FCM |
| **Gestão de Segredos** | [Infisical SDK 3.0.7](https://infisical.com/) | Provisionamento seguro de variáveis e chaves |
| **Métricas e Telemetria** | [Micrometer Prometheus Registry](https://micrometer.io/) | Exposição de métricas para scraping do Prometheus |
| **Produtividade** | [Project Lombok](https://projectlombok.org/) | Redução de boilerplate (getters, setters, builders) |
| **Validação de Dados** | Hibernate Validator / Jakarta Validation | Validação estrita de contratos de entrada |
| **Testes Automatizados** | JUnit 5, Mockito, Spring Security Test, H2 Database | Suíte de testes unitários e de integração (MockMvc) |

---

## 📂 Estrutura do Projeto

```text
ms-spring-api/
├── gradle/
│   └── wrapper/                          # Binários do Gradle Wrapper
├── src/
│   ├── main/
│   │   ├── java/com/ourosapp/springapi/
│   │   │   ├── SpringApiApplication.java # Classe principal de inicialização Spring Boot
│   │   │   ├── client/                   # Clientes HTTP (AudienceEngineClient)
│   │   │   ├── config/                   # Configurações de OpenAPI, CORS, Firebase, Redis, etc.
│   │   │   ├── constants/                # Constantes de regras e mensagens do sistema
│   │   │   ├── controller/               # Controladores REST da API (25+ controllers)
│   │   │   ├── dto/                      # Records e Data Transfer Objects agrupados por domínio
│   │   │   ├── entity/                   # Entidades JPA mapeadas para as tabelas PostgreSQL
│   │   │   ├── event/                    # Eventos de domínio (LotCreatedEvent)
│   │   │   ├── listener/                 # Ouvintes de eventos (LotNotificationListener)
│   │   │   ├── repository/               # Repositórios Spring Data JPA e Projections
│   │   │   ├── scheduler/                # Agendadores (recalculo semanal de rankings)
│   │   │   ├── security/                 # Filtros de segurança, JWT Converter e regras M2M
│   │   │   └── service/                  # Camada de serviços e regras de negócio
│   │   └── resources/
│   │       ├── application.properties    # Configurações globais da aplicação
│   │       └── application-local.properties.example # Modelo para datasource local
│   └── test/
│       ├── java/com/ourosapp/springapi/  # Testes unitários de serviços, DTOs e MockMvc
│       └── resources/                    # application.properties de testes (H2 em memória)
├── .env.example                          # Modelo de variáveis de ambiente
├── build.gradle                          # Dependências e tarefas Gradle
├── gradlew                               # Wrapper para Linux/macOS
├── gradlew.bat                           # Wrapper para Windows
└── settings.gradle                       # Nome e configuração do projeto
```

---

## ⚙️ Configuração de Ambiente

Crie o arquivo `.env` na raiz do projeto a partir do modelo `.env.example`:

```bash
cp .env.example .env
```

### Variáveis Disponíveis

| Variável | Descrição | Exemplo Padrão |
|---|---|---|
| `SERVER_PORT` | Porta HTTP da aplicação. | `8080` (ou `8000`) |
| `DB_URL` | URL de conexão JDBC do banco de dados PostgreSQL. | `jdbc:postgresql://<HOST>:<PORT>/<DATABASE>?sslmode=require` |
| `DB_USERNAME` | Usuário de autenticação do PostgreSQL. | `postgres` |
| `DB_PASSWORD` | Senha de autenticação do PostgreSQL. | `sua_senha` |
| `REDIS_URL` | String de conexão para o Redis (ex: Upstash). | `rediss://default:<SENHA>@<HOST>:6379` |
| `KEYCLOAK_ISSUER_URL` | Emissor oficial dos tokens JWT no Keycloak. | `https://ouros-keycloak.discloud.app/realms/ouros` |
| `KEYCLOAK_JWK_SET_URL` | Endpoint de chaves públicas para validação RS256. | `https://ouros-keycloak.discloud.app/realms/ouros/protocol/openid-connect/certs` |
| `KEYCLOAK_AUDIENCE` | Audiência esperada nos tokens JWT. | `ms-spring-api` |
| `AUDIENCE_ENGINE_URL` | Endpoint do serviço de segmentação de audiência. | `https://ouros-audience.discloud.app` |
| `INTERNAL_SERVICE_SECRET` | Chave secreta compartilhada para rotas internas M2M. | `sua-chave-secreta-m2m-min-32-chars` |
| `METRICS_KEYCLOAK_AUTHORIZED_PARTY` | Authorized Party autorizado a acessar `/metrics`. | `ouros-prometheus` |
| `FIREBASE_CREDENTIALS_JSON` | Credencial JSON do Firebase Admin para push FCM. | `{"type": "service_account", ...}` |
| `APP_RANKING_CRON` | Expressão cron para cálculo automático de ranking. | `0 0 0 */7 * *` (a cada 7 dias) |

---

## 💻 Instalação, Compilação e Execução

### Pré-requisitos
- **Java Development Kit (JDK):** Versão 17 LTS instalada.
- **Gradle Wrapper:** Já incluído na raiz (`gradlew` / `gradlew.bat`).
- **PostgreSQL e Redis:** Instâncias ativas (locais, em Docker ou na nuvem).

### Comandos de Execução Local

```powershell
# No Windows PowerShell:
.\gradlew.bat clean build
.\gradlew.bat bootRun

# No Linux ou macOS:
./gradlew clean build
./gradlew bootRun
```

### Execução via Docker Compose

```bash
docker compose up --build
```

Após iniciar, verifique a saúde da aplicação:
```bash
curl http://localhost:8080/
curl http://localhost:8080/health
```

---

## 🧪 Testes Automatizados e Qualidade

O projeto conta com suíte abrangente de testes unitários e de integração utilizando **JUnit 5**, **Mockito**, **MockMvc** e banco em memória **H2**:

```powershell
# Executar todos os testes da aplicação:
# No Windows:
.\gradlew.bat test

# No Linux ou macOS:
./gradlew test

# Gerar relatório de cobertura JaCoCo (XML e HTML):
.\gradlew.bat test jacocoTestReport
```

Os relatórios HTML de cobertura gerados pelo JaCoCo ficam disponíveis em `build/reports/jacoco/test/html/index.html`.

---

## 👥 Contribuidores e Licença

### Principais Contribuidores

<!-- CONTRIBUTORS:START -->
- [@Lucas-Cayres-Porto](https://github.com/Lucas-Cayres-Porto) — Arquitetura de software, segurança OAuth2 e regras de negócio
- [@Nicolas25vlad](https://github.com/Nicolas25vlad) — Integrações de telemetria, endpoints e manutenção
<!-- CONTRIBUTORS:END -->

> Atualizado automaticamente pelo workflow de metadados do repositório.

### Licença

Este projeto é distribuído sob a licença **MIT**. Para maiores detalhes, consulte o arquivo [LICENSE](LICENSE).
