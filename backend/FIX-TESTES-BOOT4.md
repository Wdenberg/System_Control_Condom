# Correção dos testes para Spring Boot 4 + Testcontainers 2

O `compileJava` já estava passando. O erro seguinte era específico dos testes:

1. `AutoConfigureMockMvc` mudou de pacote no Spring Boot 4:
   `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc`
2. `PostgreSQLContainer` do Testcontainers 2.x não é genérico; a instanciação deve ser `new PostgreSQLContainer("postgres:16-alpine")`.
3. O módulo JDBC/PostgreSQL deve continuar em `org.testcontainers:testcontainers-postgresql:2.0.5` e o módulo JUnit em `org.testcontainers:testcontainers-junit-jupiter:2.0.5`.

Validação estrutural feita após a correção:
- 0 ocorrências do pacote antigo de `AutoConfigureMockMvc`.
- 0 ocorrências de `new PostgreSQLContainer<>(...)`.

Execute no ambiente com Docker:

```bash
./gradlew clean test --refresh-dependencies
```
