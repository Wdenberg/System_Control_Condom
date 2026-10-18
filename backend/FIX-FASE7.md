# Correção da Fase 7

Correções incluídas após a execução de `./gradlew test` no ambiente do projeto:

1. Spring Boot 4.1.1 usa Jackson 3 como biblioteca JSON padrão. Os imports de `ObjectMapper`, `JsonNode` e `TypeReference` foram migrados para `tools.jackson.*`.
2. `spring-boot-starter-jackson` foi declarado explicitamente no `build.gradle`.
3. `findAndRegisterModules()` foi removido dos testes, pois não faz parte da API Jackson 3 usada pelo projeto.
4. `SecurityFilterChain` foi importado de `org.springframework.security.web.SecurityFilterChain`.
5. `AuthenticationException` foi corrigido para `org.springframework.security.core.AuthenticationException`.
6. `MovementController` deixou de converter `MovementResult` duas vezes.
7. O suporte de teste MVC `spring-boot-starter-webmvc-test` foi declarado explicitamente.
8. A configuração Hikvision inclui `HIKVISION_ENTRY_HOST`, `HIKVISION_EXIT_HOST`, `HIKVISION_PORT`, `HIKVISION_USERNAME` e `HIKVISION_PASSWORD`, com a mesma credencial para as duas câmeras.

Execute no seu diretório que já contém o `gradlew`:

```bash
unzip -o garage-control-backend-fase-7-fixed-content.zip -d ~/Downloads/garage-control-backend-fase-7
cd ~/Downloads/garage-control-backend-fase-7
./gradlew clean test
```

Para os testes de integração, Docker precisa estar disponível para o Testcontainers.
