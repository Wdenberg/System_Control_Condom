# Correções do teste de integração

A Fase 7 falhava na criação do contexto porque o Spring Boot 4 exige o módulo
Flyway específico para PostgreSQL: `org.flywaydb:flyway-database-postgresql`.

Também foi corrigido `DashboardServiceTest` para usar `eq(...)` nos argumentos
enum quando os demais argumentos usam matchers Mockito.

Os testes de Testcontainers continuam usando Testcontainers 2.0.5 e
`PostgreSQLContainer` não genérico.
