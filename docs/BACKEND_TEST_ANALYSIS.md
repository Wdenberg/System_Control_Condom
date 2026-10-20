# Análise dos testes de integração do backend — atualização 2026-10-20

## Resultado atual

`./gradlew clean test build --no-daemon --no-build-cache` passou em 20/10: **146 testes em 41 classes, zero falhas, erros ou testes ignorados**. RemediationIntegrationTest passou com **25/25**. PostgreSQL/Redis reais em containers isolados; JAR gerado. Evidência: [validation/backend-full-2026-10-20.json](validation/backend-full-2026-10-20.json).

A reexecução inicial dos cenários corrigidos revelou um defeito adicional de produção: a ausência de `controllerSettled` provocava 400 na desserialização do boolean primitivo, antes de chegar à regra de reconciliação. O DTO agora usa Boolean e só interpreta `Boolean.TRUE` como declaração de verificação. Testes integrados cobrem campo omitido, null e false com 409 para UNKNOWN e true com reconciliação auditada; EXHAUSTED aceita a omissão conforme o contrato. O E2E revelou também HTTP 500 na leitura de movimentações: o controller acessava Vehicle.residents depois de encerrar a transação. Listagem e detalhe agora montam MovementResult no serviço transacional; a nova regressão integrada passou. As duas falhas anteriores e ambas as correções de produção passaram na suíte completa final (25 testes na classe de remediação), e os seis cenários Chromium foram aprovados.

## Histórico — 2026-10-18

Execução enviada pelo usuário: `./gradlew test --tests RemediationIntegrationTest --info --no-build-cache -i`. O XML local confirma **24 testes, 22 aprovados, 2 falhas, nenhum erro de infraestrutura e nenhum teste ignorado**. O resumo anterior à correção está em `validation/backend-remediation-reported.json`, sem copiar corpos de requisição, tokens ou credenciais.

## Causas e correções

| Teste | Resultado observado | Causa confirmada | Correção no teste |
|---|---|---|---|
| `unknownOutcomeRequiresControllerVerificationBeforeReconciliation` | Esperava 200 em `/gates/operations/{id}/acknowledge`, recebeu 404 | Essa rota não existe em GateController nem no contrato. A declaração de verificação pertence a `/movements/{id}/passage-confirmation` | Testar 409 sem controllerSettled; confirmar que a reserva continua ativa e não há reenvio; enviar controllerSettled=true ao endpoint existente e validar FAILED, liberação da reserva e auditoria |
| `reconciledPassageCannotBeRetriedAdministratively` | Esperava 200 na reconciliação, recebeu 409 | Após apenas uma rejeição definitiva, a operação retorna a PENDING enquanto não atinge maxAttempts. O bloqueio de reconciliação é correto, inclusive com controllerSettled=true | Fixar o limite do cenário em três tentativas, consumir claims reais até EXHAUSTED e conferir o histórico. Antes do esgotamento, verificar 409. Depois, reconciliar e verificar que retry administrativo continua negado |

Em 18/10, somente a configuração e os cenários de teste foram alterados. Em 20/10, foi corrigida também a desserialização do campo opcional conforme descrito acima. Continuam vigentes: não cancelar comando PENDING/PROCESSING; não repetir UNKNOWN automaticamente; confirmação operacional auditada para resultado incerto. O relógio de elegibilidade da próxima tentativa é adiantado no banco isolado do teste para evitar sleeps; contagem e histórico são produzidos pelos serviços reais.

## Evidências positivas da execução enviada

- Docker Desktop foi acessado pelo Testcontainers; PostgreSQL e Redis iniciaram.
- Flyway aplicou V1–V12 em banco vazio, e o contexto JPA/Spring iniciou. Isso não valida upgrade de um banco com dados históricos.
- Os cenários de revogação por senha, role e desativação, encerramento SSE e os cinco perfis de acesso passaram.
- Concorrência do webhook, troca do IP sem duplicar UUID, claim exclusivo, lease expirada sem reenvio e bloqueio de UNKNOWN legado passaram.
- Cadastro transacional, rollback, repetição concorrente, flags active, proteção de vaga e separação entre aceitação de comando e ocupação passaram.
- Override de baixa confiança e rejeição de override para veículo desconhecido passaram.

Esses resultados se referem à classe filtrada. A execução não comprova toda a suíte do backend, a integração com controlador físico, restauração ou prontidão de produção.

## Mensagens secundárias

- O aviso de Mockito/Byte Buddy trata do carregamento dinâmico de agente na JVM; os testes chegaram às assertions HTTP. Ele não causou os dois resultados 404/409.
- As mensagens de Spring Data Redis indicam que os repositórios JPA não foram classificados como repositórios Redis; o log encontrou 12 JPA e zero Redis. Não justificam adicionar RedisHash às entidades JPA.
- Netty informou fallback para o resolvedor DNS do sistema no macOS. PostgreSQL e Redis conectaram e os cenários executaram; as duas falhas registradas são assertions de status HTTP.
- O aviso sobre PageImpl identifica um risco de estabilidade do JSON paginado. Uma futura correção deve preservar o contrato consumido pelo frontend.
- A reconexão Redis surge no final da execução, antes do encerramento do contexto Spring. Pela sequência temporal, é compatível com o encerramento dos containers antes do cliente Lettuce; não há falha de teste atribuída a Redis no relatório.
- `--no-build-cache` não força a recompilação de tarefas UP-TO-DATE. Use `--rerun-tasks` para a nova validação completa do cenário.

## Validação desta correção

O agente compilou **166 arquivos de produção e 41 arquivos de testes** com Java21 e os jars do classpath real do worker Gradle disponibilizado pela execução do usuário: logs `validation/backend-current-main-compile.log` e `validation/backend-current-test-compile.log`. Essa compilação usa as dependências atuais do projeto e substitui a limitação anterior a um subconjunto compilado com cache Maven de outras versões. Não equivale a executar o build Gradle ou a suíte integrada.

Com esse mesmo conjunto, o agente executou **68 testes unitários, todos aprovados**, via JUnit Launcher. Foram selecionados os testes independentes de containers/contexto integrado; o teste HTTP com servidor local também ficou fora por restrição de socket. Os arquivos e motivos estão em `validation/backend-current-unit-scope.txt`; nenhum teste foi removido da suíte oficial. Log `validation/backend-current-unit.log`. A regressão XXE recusa DOCTYPE no log e passa conforme esperado.

Na execução de 18/10, Docker era inacessível ao agente por permissão do socket. A reexecução então recomendada era:

```bash
./gradlew test --tests 'com.estacionamento.api.RemediationIntegrationTest' --rerun-tasks --no-build-cache
```

Essas pendências foram superadas pela execução autorizada de 20/10: 25/25 nessa classe e 146/146 na suíte completa. Upgrade com dados históricos, ensaios operacionais e hardware continuam fora dessa evidência.
