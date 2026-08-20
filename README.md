# ms-spring-api

<!-- REPO-METADATA:START -->
<div align="center">

[![Repo Size](https://img.shields.io/github/repo-size/Ouros-App/ms-spring-api?style=flat-square&label=REPO%20SIZE)](https://github.com/Ouros-App/ms-spring-api)
[![Languages](https://img.shields.io/github/languages/count/Ouros-App/ms-spring-api?style=flat-square&label=LANGUAGES)](https://github.com/Ouros-App/ms-spring-api/languages)
[![Forks](https://img.shields.io/github/forks/Ouros-App/ms-spring-api?style=flat-square&label=FORKS)](https://github.com/Ouros-App/ms-spring-api/network/members)
[![Issues](https://img.shields.io/github/issues/Ouros-App/ms-spring-api?style=flat-square&label=ISSUES)](https://github.com/Ouros-App/ms-spring-api/issues)
[![Pull Requests](https://img.shields.io/github/issues-pr/Ouros-App/ms-spring-api?style=flat-square&label=PULL%20REQUESTS)](https://github.com/Ouros-App/ms-spring-api/pulls)

</div>
<!-- REPO-METADATA:END -->

Projeto inicial de uma API REST com Spring Boot e Gradle.

## Status e escopo

A implementação atual é mínima e contém:

- endpoint `GET /` com uma mensagem de disponibilidade;
- endpoint `GET /health` que retorna `{"status":"ok"}`;
- configuração inicial de JPA e PostgreSQL;
- dependências de validação, segurança e JWT declaradas para evolução do projeto.

Não há, no código atual, recursos de domínio, operações CRUD ou autenticação JWT implementados. A configuração automática padrão do Spring Security está desativada em `SpringApiApplication`.

## Pré-requisitos

- Java 17, conforme a configuração de compilação e o workflow de CI.
- O Gradle Wrapper incluído no repositório: `gradlew` ou `gradlew.bat`.
- PostgreSQL apenas quando a configuração de datasource for utilizada.

## Instalação e configuração

O arquivo `.env.example` documenta:

- `SERVER_PORT`;
- `DB_URL`;
- `DB_USERNAME`;
- `DB_PASSWORD`.

A aplicação lê `SERVER_PORT` em `src/main/resources/application.properties` e usa a porta `8080` como padrão.

Para configurar o datasource local, use `src/main/resources/application-local.properties.example` como modelo para criar `src/main/resources/application-local.properties`. O modelo define:

```properties
spring.datasource.url=jdbc:postgresql://<HOST>:<PORT>/<DATABASE>?sslmode=require
spring.datasource.username=<USERNAME>
spring.datasource.password=<PASSWORD>
spring.datasource.driver-class-name=org.postgresql.Driver
```

O arquivo local é importado de forma opcional por `application.properties` e não deve conter credenciais versionadas.

## Execução

No Linux/macOS:

```bash
./gradlew clean build
./gradlew bootRun
```

No Windows:

```bat
gradlew.bat clean build
gradlew.bat bootRun
```

Com Docker Compose:

```bash
docker compose up --build
```

O Compose publica a porta `8080` do host para a porta `8000` do container. A documentação automática do Spring Boot não está configurada no repositório.

## Uso

Após iniciar a aplicação:

```bash
curl http://localhost:8080/
curl http://localhost:8080/health
```

Respostas esperadas:

```json
{"message":"Spring REST API com Gradle pronta para evoluir."}
{"status":"ok"}
```

## Testes e qualidade

```bash
./gradlew test
./gradlew clean build jacocoTestReport --no-daemon
```

O workflow de CI executa build, testes, relatório JaCoCo, SonarCloud e CodeQL.

## Estrutura do projeto

```text
.
├── build.gradle
├── gradlew
├── gradlew.bat
├── src/main/java/com/ourosapp/springapi
│   ├── SpringApiApplication.java
│   └── controller
│       ├── HealthController.java
│       └── HomeController.java
├── src/main/resources
│   ├── application.properties
│   └── application-local.properties.example
└── src/test
    └── java/com/ourosapp/springapi/SpringApiApplicationTests.java
```

## Licença

Este projeto está sob a licença MIT, conforme o arquivo [LICENSE](LICENSE).


## Principais contribuidores

<!-- CONTRIBUTORS:START -->
- [@Lucas-Cayres-Porto](https://github.com/Lucas-Cayres-Porto) — 1 contribuições
- [@Nicolas25vlad](https://github.com/Nicolas25vlad) — 1 contribuições
<!-- CONTRIBUTORS:END -->

> Atualizado automaticamente semanalmente pelo workflow de metadados do README.
