# 🤖 Diretrizes para Agentes de IA — ms-spring-api

Este documento é a referência canônica de arquitetura, padrões de engenharia, regras de negócio e fluxos operacionais para agentes de IA (Google Antigravity, Claude Code, GitHub Copilot, etc.) atuando no repositório **ms-spring-api**.

---

## 📌 1. Visão Geral e Contexto do Domínio

O **ms-spring-api** é o microserviço backend central do ecossistema **Ouros App**, uma plataforma voltada à gestão e integração avícola/agropecuária.

### 🏢 Entidades Centrais do Domínio
* **Enterprise (Empresa Integradora):** Empresa que gerencia múltiplos produtores e granjas integradas.
* **CompanyEmployee (Funcionário da Integradora):** Colaborador corporativo com acesso restrito às granjas e dados da sua respectiva integradora.
* **Farm (Fazenda / Granja):** Unidade produtiva que possui capacidade de aves, área, localização geográfica e vínculo com uma empresa integradora.
* **FarmOwner (Produtor Rural / Granjeiro):** Dono ou operador responsável por uma fazenda vinculada à integradora.
* **Lot (Lote de Aves):** Ciclo de criação e alojamento de aves em uma fazenda vinculada (linhagem, quantidade alojada, datas de entrada/saída e status).
* **WaterRegistry (Registro de Água):** Apontamentos e medições periódicas de hidrômetro/consumo de água em uma fazenda.
* **EnergyRegistry (Registro de Energia):** Apontamentos e medições periódicas de consumo de energia elétrica (kWh) em uma fazenda.
* **Address (Endereço):** Entidade de geolocalização e endereço físico, reaproveitável ou gerada de forma embutida/aninhada por empresas e fazendas.
* **Adm (Administrador da Plataforma):** Perfil com privilégios globais de governança no sistema.

---

## 🧰 2. Stack Tecnológica e Versões

| Componente | Tecnologia | Versão / Detalhes |
|---|---|---|
| **Linguagem** | Java | **17 LTS** (compilação com `options.release = 17`) |
| **Framework** | Spring Boot | **3.4.0** |
| **Gerenciador de Build** | Gradle Wrapper | **`gradlew` / `gradlew.bat`** |
| **Persistência** | Spring Data JPA / Hibernate | PostgreSQL (runtime) / H2 (testes) |
| **Segurança & Auth** | Spring Security 6 (OAuth2 Resource Server) | Tokens RS256 validados via JWKS do Keycloak |
| **Validação** | Jakarta Bean Validation | Hibernate Validator (`@CNPJ`, `@Valid`, `@NotBlank`, etc.) |
| **Documentação API** | SpringDoc OpenAPI | **2.8.5** (Swagger UI em `/swagger-ui.html`, OpenAPI em `/v3/api-docs`) |
| **Utilitários** | Lombok | `@Getter`, `@Setter`, `@Builder`, `@ToString.Exclude` |
| **Testes** | JUnit 5 + Mockito | **Spring Boot 3.4 `@MockitoBean`**, MockMvc, Spring Security Test |
| **Cobertura & Qualidade** | JaCoCo, SonarCloud, CodeQL | XML e HTML reports em `build/reports/jacoco/` |

---

## ⚡ 3. Comandos Essenciais (Execução Rápida)

> [!IMPORTANT]
> Em sistemas Windows (PowerShell/CMD), utilize `.\gradlew.bat`. No Linux/macOS ou CI, use `./gradlew`.

### 🔨 Build e Compilação
```bash
# Limpar e compilar o projeto completo
.\gradlew.bat clean build

# Compilar sem rodar testes (para checagem rápida de sintaxe)
.\gradlew.bat compileJava compileTestJava
```

### 🚀 Execução Local
```bash
# Iniciar a aplicação Spring Boot localmente (porta padrão 8080)
.\gradlew.bat bootRun

# Subir com Docker Compose (se necessário banco local)
docker compose up --build
```

### 🧪 Execução de Testes e Cobertura
```bash
# Executar todos os testes automatizados
.\gradlew.bat test

# Executar uma classe de teste específica
.\gradlew.bat test --tests "com.ourosapp.springapi.service.EnterpriseServiceTest"

# Executar um teste específico ou padrão de método
.\gradlew.bat test --tests "com.ourosapp.springapi.controller.EnterpriseControllerMockMvcTest.deve*"

# Gerar relatório completo de cobertura de código (JaCoCo)
.\gradlew.bat clean build jacocoTestReport --no-daemon
```
*O relatório HTML do JaCoCo estará disponível em:* `build/reports/jacoco/test/html/index.html`

---

## 🏛️ 4. Arquitetura e Estrutura de Diretórios

O projeto adota uma arquitetura em camadas clara e desacoplada:

```text
ms-spring-api/
├── .github/
│   ├── PULL_REQUEST_TEMPLATE/  # Templates de PR (feat, fix, chore, docs, hotfix)
│   └── workflows/              # CI/CD (GitHub Actions com validação de build, testes, Sonar e CodeQL)
├── src/
│   ├── main/
│   │   ├── java/com/ourosapp/springapi/
│   │   │   ├── config/         # Configurações globais (SecurityConfig, OpenApiConfig, GlobalExceptionHandler)
│   │   │   ├── constants/      # Constantes de negócio (RoleConstants, ErrorMessages)
│   │   │   ├── controller/     # Controllers REST (endpoints finos, Swagger annotations)
│   │   │   ├── dto/            # Data Transfer Objects (Java records segregados por domínio)
│   │   │   │   ├── address/
│   │   │   │   ├── companyemployee/
│   │   │   │   ├── energyregistry/
│   │   │   │   ├── enterprise/
│   │   │   │   ├── farm/
│   │   │   │   ├── farmowner/
│   │   │   │   ├── lot/
│   │   │   │   └── waterregistry/
│   │   │   ├── entity/         # Entidades JPA mapeadas para o banco de dados relacional
│   │   │   ├── repository/     # Interfaces Spring Data JPA
│   │   │   ├── security/       # Componentes de segurança (KeycloakJwtAuthenticationConverter, AudienceValidator, UserPrincipal)
│   │   │   ├── service/        # Regras de negócio, transações (@Transactional) e controle RBAC
│   │   │   └── SpringApiApplication.java # Ponto de entrada da aplicação
│   │   └── resources/
│   │       ├── application.properties               # Configurações base da aplicação
│   │       ├── application-local.properties.example # Template para profile de desenvolvimento
│   │       └── application-local.properties         # Configuração local (ignorado pelo git)
│   └── test/
│       └── java/com/ourosapp/springapi/
│           ├── config/         # Testes de infraestrutura e OpenAPI
│           ├── controller/     # Testes de camada web com @WebMvcTest e MockMvc
│           ├── dto/            # Testes unitários de DTOs, imutabilidade e sanitização
│           ├── security/       # Testes unitários de conversão Keycloak, validação de audience e UserPrincipal
│           └── service/        # Testes unitários de regras de negócio com Mockito
├── build.gradle                # Script de build Gradle
├── Dockerfile                  # Empacotamento Docker multi-stage
├── docker-compose.yml          # Orquestração de containers local
└── README.md                   # Documentação geral do repositório
```

---

## 🔐 5. Segurança, Autenticação e RBAC (Role-Based Access Control)

### 🔑 Modelo de Autenticação
* **OAuth2 Resource Server (Bearer JWT):** O microserviço atua exclusivamente como Resource Server. O token JWT assimétrico (RS256) emitido pelo Keycloak deve ser enviado no cabeçalho `Authorization: Bearer <TOKEN>`.
* **Validação de Token:** A validação da assinatura criptográfica é feita contra o JWKS do Keycloak (`/protocol/openid-connect/certs`). O `AudienceValidator` assegura que o token contenha o audience configurado (`ms-spring-api`).
* **Claims do JWT:**
  - `sub`: Identificador único no Keycloak (UUID).
  - `email` / `preferred_username`: Identificador de e-mail do usuário.
  - `realm_access.roles` / `resource_access[clientId].roles`: Roles atribuídas globalmente ou por client no Keycloak (`ADM`, `COMPANY_EMPLOYEE`, `FARM_OWNER`).
  - `database_id`: ID numérico da entidade no banco de dados relacional (injetado via client scope `ouros-identity`).
* **Conversor de JWT (`KeycloakJwtAuthenticationConverter`):**
  - **Validação Rigorosa de Roles:** Rejeita imediatamente tokens sem uma role autorizada com `OAuth2AuthenticationException` (`invalid_token`), garantindo que apenas identidades com perfil válido acessem a aplicação.
  - **Resolução Stateless de Identidade:** Prioriza a extração direta do ID numérico a partir da claim `database_id` do JWT, evitando consultas desnecessárias de I/O ao banco de dados relacional.
  - **Fallback sob Demanda:** Caso a claim `database_id` não esteja no token, consulta o usuário no banco local via `UserDetailsServiceImpl.loadUserByEmailAndRole(email, role)`.
* **Injeção no Controller:** O conversor instancia um `KeycloakAuthenticationToken` cujo principal é um `UserPrincipal` (garantindo coleção de autoridades não nula via `getAuthorities()`), injetado nos endpoints via `@AuthenticationPrincipal UserPrincipal principal`.
* **Tratamento de Erros de Autenticação:** `SecurityConfig` centraliza a resposta de token inválido ou ausente no `authenticationEntryPoint` do Resource Server retornando `401 Unauthorized`.

### 👥 Perfis de Acesso (`RoleConstants`)
```java
public final class RoleConstants {
    public static final String ADM = "ADM";
    public static final String COMPANY_EMPLOYEE = "COMPANY_EMPLOYEE";
    public static final String FARM_OWNER = "FARM_OWNER";
}
```

### 🛡️ Matriz de Permissões por Recurso
| Recurso / Rota | Ação | ADM | COMPANY_EMPLOYEE | FARM_OWNER |
|---|---|:---:|:---:|:---:|
| `/health`, `/`, `/v3/api-docs/**`, `/swagger-ui/**` | Monitoramento / Swagger | Público | Público | Público |
| `/enterprises` (POST) | Cadastrar integradora | ✅ Total | ❌ 403 | ❌ 403 |
| `/enterprises` (GET) | Listar integradoras | ✅ Todas | ✅ Apenas a sua | ❌ 403 |
| `/enterprises/{id}` (GET / PATCH) | Detalhar / Editar integradora | ✅ Total | ✅ Apenas a sua | ❌ 403 |
| `/company-employees` (POST / DELETE) | Criar / Deletar funcionário | ✅ Total | ✅ Na sua empresa | ❌ 403 |
| `/company-employees/me` (GET) | Ver próprio perfil | ❌ | ✅ Apenas os seus dados | ❌ |
| `/farms` (POST) | Cadastrar granja/fazenda | ✅ Total | ✅ Na sua empresa | ❌ 403 |
| `/farms` (GET) | Listar granjas/fazendas | ✅ Todas | ✅ Da sua empresa | ✅ Suas granjas |
| `/farms/{id}` (GET / PATCH / DELETE) | Manter granja | ✅ Total | ✅ Da sua empresa | ✅ Apenas leitura/vínculo |
| `/farm-owners` (POST / DELETE) | Cadastrar / Excluir produtor rural | ✅ Total | ✅ Na sua empresa | ❌ 403 |
| `/farm-owners` (GET / PATCH) | Listar / Detalhar / Editar produtor | ✅ Total | ✅ Na sua empresa | ✅ Apenas os seus dados |
| `/lots` (POST / GET / PATCH / DELETE) | Gestão de lotes de aves | ✅ Total | ✅ Da sua empresa | ✅ Das suas granjas |
| `/water-registries` (POST / GET / PATCH / DELETE) | Registros de consumo de água | ✅ Total | ✅ Da sua empresa | ✅ Das suas granjas |
| `/energy-registries` (POST / GET / PATCH / DELETE) | Registros de consumo de energia | ✅ Total | ✅ Da sua empresa | ✅ Das suas granjas |
| `/addresses` (POST / GET / PATCH) | Gestão de endereços | ✅ Total | ✅ Conforme escopo | ✅ Conforme escopo |

---

## 📐 6. Diretrizes de Desenvolvimento e Padrões de Código

### 1️⃣ Idiomas e Nomenclatura
* **Código Java (Classes, Interfaces, Records, Métodos, Variáveis, DTOs, Pacotes):** Estritamente em **Inglês** (`EnterpriseService`, `poultryCapacity`, `createFarm`).
* **Mensagens voltadas ao Usuário, Validações (`message = "..."`), Descrições OpenAPI/Swagger (`@Operation`, `@Schema`), Logs funcionais e Respostas HTTP:** Estritamente em **Português (PT-BR)**.
* **Commits, PRs e documentação:** Em **Português (PT-BR)** ou padrão de mercado adotado no time.

### 2️⃣ DTOs (Data Transfer Objects)
* **Sempre usar Java `record`** para todos os DTOs (`RequestDTO`, `ResponseDTO`, `UpdateDTO`).
* **Sanitização no Compact Constructor:** Fazer `trim()`, converter e-mails para `toLowerCase()` e limpar formatação de documentos (remover máscaras de CPF/CNPJ).
* **Compatibilidade JSON:** Anotar campos com `@JsonProperty("snake_case")` e `@JsonAlias("camelCase")`.
* **Swagger/OpenAPI:** Descrever campos com `@Schema(description = "...", example = "...")`.
* **Padrão de Criação Composta de Endereço:** Em entidades que requerem endereço (ex.: Empresa, Fazenda), o DTO de requisição deve suportar ou um ID existente (`id_address`) ou um objeto embutido (`address`), validado com `@AssertTrue`:
  ```java
  @Schema(hidden = true)
  @AssertTrue(message = "É obrigatório informar exatamente uma forma de endereço: 'id_address' ou o objeto 'address' completo, mas não ambos nem nenhum")
  public boolean hasValidAddressInfo() {
      return (idAddress != null) ^ (address != null);
  }
  ```
* **Métodos Utilitários:**
  - `ResponseDTO`: Fornecer método estático `public static EnterpriseResponseDTO fromEntity(Enterprise entity)`.
  - `UpdateDTO`: Fornecer método booleano `public boolean hasUpdates()`.

### 3️⃣ Controllers REST
* **Controllers Finos:** Não inserir regras de negócio em Controllers. Delegar toda a lógica para a camada de `Service`.
* **Anotações Obrigatórias:** `@RestController`, `@RequestMapping("/rota")`, `@RequiredArgsConstructor`, `@Tag(name = "...", description = "...")`, `@SecurityRequirement(name = "BearerAuth")`.
* **Respostas HTTP Padronizadas:**
  - `POST` (Criação): Retornar `201 Created` com cabeçalho `Location` via `ServletUriComponentsBuilder`:
    ```java
    URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}").buildAndExpand(response.id()).toUri();
    return ResponseEntity.created(location).body(response);
    ```
  - `GET` / `PATCH` / `PUT`: Retornar `200 OK` com o corpo DTO.
  - `DELETE`: Retornar `204 No Content` (`ResponseEntity.noContent().build()`).
* **Documentação OpenAPI:** Anotar cada endpoint com `@Operation` e `@ApiResponses` cobrindo status esperados (ex.: 200, 201, 400, 401, 403, 404, 409).

### 4️⃣ Services e Regras de Negócio
* **Anotações de Transação:** Utilizar `@Transactional` para operações de escrita e `@Transactional(readOnly = true)` para operações de consulta.
* **Autenticação e Permissão:**
  - Validar a presença do usuário logado: `ensureAuthenticated(principal)`.
  - Validar regras RBAC de multi-inquilinato (multi-tenancy) antes de acessar o banco de dados.
* **Checagem de Unicidade Prévia:** Validar se já existe registro com o mesmo documento/e-mail usando métodos `existsBy...` do repositório, lançando `ResponseStatusException(HttpStatus.CONFLICT, "...")`.
* **Tratamento de Exceções HTTP:** Utilizar `ResponseStatusException(HttpStatus.<STATUS>, "<MENSAGEM>")` para erros de negócio previsíveis:
  - `400 BAD_REQUEST` — Parâmetros inválidos ou incompatíveis.
  - `401 UNAUTHORIZED` — Usuário não autenticado ou token ausente.
  - `403 FORBIDDEN` — Acesso negado pelo perfil de usuário.
  - `404 NOT_FOUND` — Registro não encontrado no banco de dados.
  - `409 CONFLICT` — Registro duplicado ou restrição de integridade violada.
* **Reaproveitamento de Mensagens:** Utilizar constantes centralizadas em `ErrorMessages` sempre que aplicável.

### 5️⃣ Entidades JPA e Repositórios
* **Anotações Lombok:** Usar `@Entity`, `@Table(name = "plural")`, `@Getter`, `@Setter`, `@ToString`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`.
* **Proteção de Dados Sensíveis:** Sempre adicionar `@ToString.Exclude` em campos de senha (`password`).
* **Chaves Estrangeiras:** O modelo atual utiliza referências diretas de ID numérico (`idEnterprise`, `idAddress`, `idFarm`) em vez de relacionamentos bidirecionais pesados. Manter consistência com esse padrão arquitetural.
* **Geração de IDs:** Usar `@GeneratedValue(strategy = GenerationType.IDENTITY)`.

### 6️⃣ Tratamento Global de Erros (`GlobalExceptionHandler`)
* A API utiliza `org.springframework.http.ProblemDetail` (RFC 7807).
* `DataIntegrityViolationException` lançada pelo Hibernate é interceptada e convertida automaticamente para status `409 CONFLICT`.

---

## 🧪 7. Estratégia de Testes Automatizados

> [!TIP]
> **Atenção (Spring Boot 3.4+):** Utilize a anotação `@MockitoBean` da biblioteca Spring Framework / Spring Boot 3.4. A anotação legada `@MockBean` do `org.springframework.boot.test.mock.mockito` está obsoleta e não deve ser usada.

### 🧩 Padrão de Teste de Controller (WebMvc)
```java
@WebMvcTest(EnterpriseController.class)
@Import({SecurityConfig.class, KeycloakJwtAuthenticationConverter.class})
class EnterpriseControllerMockMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EnterpriseService enterpriseService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private KeycloakJwtAuthenticationConverter keycloakJwtAuthenticationConverter;

    // Simulação de usuário autenticado no MockMvc:
    // .with(user(userPrincipal))
}
```

### ⚙️ Padrão de Teste de Service (Unitário com Mockito)
```java
@ExtendWith(MockitoExtension.class)
class EnterpriseServiceTest {

    @Mock
    private EnterpriseRepository enterpriseRepository;

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private AddressService addressService;

    @InjectMocks
    private EnterpriseService enterpriseService;

    // Testar: Cenário de Sucesso, Validações de Permissão (403), Conflitos (409) e Não Encontrado (404)
}
```

### 📋 Cobertura Esperada
* Toda nova lógica de negócio ou endpoint deve vir acompanhado de testes unitários para a camada de `Service` e testes de integração web para o `Controller`.
* Nomenclatura dos métodos de teste em formato descritivo BDD (ex.: `deveCadastrarEmpresaComSucesso`, `deveLancarExcecaoQuandoCnpjDuplicado`).

---

## 🌿 8. Padrão de Commits e Fluxo Git (CI/CD)

O pipeline do GitHub Actions valida estritamente a convenção **Conventional Commits** em títulos de Pull Requests e mensagens de commit.

### 📝 Formato Obrigatório
```text
<tipo>(<escopo opcional>): <descrição no imperativo e em minúsculas>

Exemplos:
feat(enterprise): adicionar suporte a upload de logo da empresa
fix(auth): corrigir validacao de expiracao do token jwt
test(farm): adicionar testes de integracao para listagem por perfil
docs(agents): atualizar instrucoes de arquitetura para agentes
refactor(security): simplificar extracao de claims no jwtutil
```

### 🏷️ Tipos Aceitos
* `feat`: Nova funcionalidade para o usuário ou API.
* `fix`: Correção de bug.
* `docs`: Alterações exclusivas de documentação.
* `style`: Formatação, ponto e vírgula ausente, sem alteração lógica.
* `refactor`: Refatoração de código sem alteração de comportamento.
* `perf`: Melhoria de performance.
* `test`: Adição ou correção de testes.
* `build` / `ci`: Alterações no build Gradle ou workflows do GitHub Actions.
* `chore`: Tarefas de manutenção ou dependências.

---

## ✅ 9. Checklist Obrigatório para o Agente de IA

Antes de considerar qualquer tarefa como concluída, o agente deve verificar se todos os itens abaixo foram cumpridos:

- [ ] **Compilação sem Erros:** `.\gradlew.bat compileJava compileTestJava` executa com sucesso.
- [ ] **Testes 100% Verificados:** `.\gradlew.bat test` roda sem falhas ou regressões.
- [ ] **Cobertura JaCoCo:** Relatório gerado via `.\gradlew.bat jacocoTestReport`.
- [ ] **Padronização de Idioma:** Código em inglês; mensagens, validações e documentação em português.
- [ ] **Segurança & RBAC:** Validações de perfil (`ADM`, `COMPANY_EMPLOYEE`, `FARM_OWNER`) e autenticação implementadas e testadas.
- [ ] **Documentação OpenAPI:** Endpoints e DTOs devidamente anotados com `@Operation`, `@ApiResponses` e `@Schema`.
- [ ] **Zero Segredos:** Nenhuma credencial, token ou chave secreta commitada no código ou em arquivos de teste.
- [ ] **Conventional Commits:** Título do PR ou mensagens de commit seguem o padrão esperado pela CI.
