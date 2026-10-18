# ADR 0015 — Compatibilidade Spring Boot 4 / Jackson 3 e build

- Status: Accepted
- Date: 2026-09-13

## Contexto

O projeto usa Spring Boot 4.1.1. A linha 4.x adota Jackson 3 como biblioteca JSON padrão, com mudança de namespace de `com.fasterxml.jackson.*` para `tools.jackson.*` para os módulos de core/databind. A aplicação da Fase 7 ainda possuía imports de Jackson 2 e duas inconsistências de compilação.

## Decisão

1. Usar `tools.jackson.core.type.TypeReference` e `tools.jackson.databind.ObjectMapper`.
2. Usar `tools.jackson.databind.JsonNode` nos testes.
3. Não usar `findAndRegisterModules()`, removido da API moderna; o `ObjectMapper` configurado pelo Spring Boot permanece como dependência injetada.
4. Declarar `spring-boot-starter-jackson` explicitamente no build.
5. Corrigir o import de `SecurityFilterChain` para `org.springframework.security.web.SecurityFilterChain`.
6. Corrigir `AuthenticationException` para `org.springframework.security.core.AuthenticationException`.
7. Corrigir os controllers de movimentação para não converter duas vezes `MovementResult`.
8. Manter `spring-boot-starter-webmvc-test` como suporte explícito aos testes MVC.

## Consequências

O backend fica alinhado ao modelo de JSON do Spring Boot 4 e elimina os erros de compilação apresentados pela Fase 7. A aplicação evita a dependência de APIs Jackson 2 depreciadas no ecossistema Spring Boot 4.
