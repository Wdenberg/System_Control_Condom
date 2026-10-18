# Correção do build - Testcontainers

O `build.gradle` agora importa o BOM do Testcontainers 2.0.5:

```gradle
testImplementation platform('org.testcontainers:testcontainers-bom:2.0.5')
```

As dependências `testcontainers-junit-jupiter` e `testcontainers-postgresql` permanecem sem versão e passam a recebê-la pelo BOM.

Depois de substituir os arquivos no projeto local, execute:

```bash
./gradlew clean test --refresh-dependencies
```

A execução dos testes de integração requer Docker em funcionamento.
