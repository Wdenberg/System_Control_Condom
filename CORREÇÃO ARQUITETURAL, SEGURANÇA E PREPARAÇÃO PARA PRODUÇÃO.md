# CORREÇÃO ARQUITETURAL, SEGURANÇA E PREPARAÇÃO PARA PRODUÇÃO

## 1. IDENTIDADE E RESPONSABILIDADES

Atue como um **Arquiteto de Software Sênior, Engenheiro Backend Java/Spring Boot, Engenheiro Frontend React/TypeScript, Especialista em Segurança de Aplicações e Engenheiro DevOps/SRE**.

Você está trabalhando em um sistema de controle de estacionamento condominial que utiliza reconhecimento automático de placas (LPR/ANPR), integração com câmeras Hikvision e acionamento automático de cancelas.

Sua missão é **analisar o código existente, implementar as correções identificadas na auditoria técnica e entregar uma versão verificável, segura e preparada para implantação em produção**.

Não se limite a elaborar recomendações. Implemente as mudanças necessárias no repositório.

**Princípios obrigatórios:**

1. Segurança antes de funcionalidades.
2. Integridade e consistência das regras de negócio.
3. Idempotência e controle de concorrência.
4. Separação de responsabilidades.
5. Compatibilidade com contratos existentes, salvo quando a segurança exigir alteração.
6. Testes automatizados representativos do comportamento real.
7. Observabilidade e rastreabilidade.
8. Configuração segura por padrão (fail-closed).
9. Ausência de abertura física não autorizada.
10. Evidências verificáveis de cada correção.

---

## 2. CONTEXTO TECNOLÓGICO

### Backend

- Java 21 LTS.
- Spring Boot.
- Spring Security.
- JWT e RBAC.
- Spring Data JPA/Hibernate.
- PostgreSQL.
- Redis.
- Flyway.
- Gradle.
- Testcontainers.
- Integração Hikvision ISAPI.
- Integração com controladores de cancela.

### Frontend

- React.
- TypeScript.
- Vite.
- React Router.
- Cliente HTTP centralizado.
- SSE para comunicação em tempo real.
- Vitest e Testing Library.

### Infraestrutura

- Docker e Docker Compose.
- Nginx.
- Prometheus e Grafana.
- Scripts de backup e restauração.
- Playwright para E2E.
- k6 para testes de carga.

**Importante:** antes de implementar, confirme no repositório as versões, dependências, serviços e contratos efetivamente utilizados. Não presuma que todos os componentes estão configurados corretamente.

---

# 3. ETAPA INICIAL — INSPEÇÃO DO REPOSITÓRIO

Antes de modificar código:

1. Leia `AGENT.md`, `SPEC_DRIVER_DEVELOPER.md`, ADRs e demais documentos aplicáveis, quando existirem.
2. Inspecione a estrutura real do frontend, backend, testes e infraestrutura.
3. Verifique as migrações Flyway e os contratos REST.
4. Identifique os fluxos completos de entrada, saída, autorização e abertura da cancela.
5. Localize os testes associados a cada componente.
6. Analise todas as dependências entre as correções propostas.
7. Registre o estado inicial do Git e preserve mudanças preexistentes.
8. Execute verificações iniciais quando o ambiente permitir.

**Não reescreva o sistema do zero. Preserve a arquitetura modular existente e refatore somente quando existir justificativa técnica concreta.**

Crie um arquivo `docs/REMEDIATION_PLAN.md` contendo os problemas confirmados, riscos, dependências, estratégia de correção e critérios de aceite.

Não considere automaticamente um achado anterior como confirmado: valide-o no código atual.

---

# 4. FASE 1 — CORREÇÕES CRÍTICAS DE SEGURANÇA

**Prioridade: BLOQUEANTE PARA PRODUÇÃO.**

## 4.1. Corrigir autenticação do webhook Hikvision

Arquivo principal:

`HikvisionWebhookSecurityService.java`

Problema identificado:

O mecanismo `enforceRemoteIp` utiliza o IP declarado no XML como evidência de identidade da câmera.

Esse campo pode ser falsificado.

### Implementação obrigatória

1. Remover qualquer confiança na identidade declarada exclusivamente pelo payload XML.
2. Exigir autenticação confiável para todas as requisições de webhook.
3. Implementar token individual por câmera ou mecanismo equivalente de autenticação forte.
4. Armazenar credenciais de forma segura, sem expor valores sensíveis em logs.
5. Utilizar comparação segura de credenciais quando aplicável.
6. Validar a associação entre credencial autenticada e câmera cadastrada.
7. Permitir validação adicional do IP real da conexão somente quando houver cadeia de proxies confiável.
8. Rejeitar requisições com credencial ausente, vazia, inválida ou de outra câmera.
9. Não habilitar automaticamente a cancela quando a autenticação falhar.
10. Caso algum modelo Hikvision não suporte o mecanismo escolhido, documentar uma alternativa segura com gateway de integração, isolamento de rede e autenticação no gateway.

Nunca considerar o IP informado no XML como prova de identidade.

### Testes obrigatórios

- Webhook sem autenticação.
- Token vazio.
- Token inválido.
- Token pertencente a outra câmera.
- Payload com IP falsificado.
- Câmera desativada.
- Requisição válida.
- Tentativa de repetição de evento.
- Verificação de que requisições rejeitadas não criam movimentação nem comando físico.

**Critério de aceite:** nenhum payload não autenticado consegue produzir uma operação de cancela.

## 4.2. Implementar revogação efetiva de sessões JWT

Problema:

As permissões ficam incorporadas ao JWT e podem continuar válidas após alterações administrativas.

### Implementação

1. Criar estratégia persistente de revogação utilizando `tokenVersion`, `sessionVersion` ou sessões identificadas.
2. Validar o estado atual do usuário durante a autorização.
3. Invalidar acessos existentes quando ocorrer:
   - Desativação do usuário.
   - Alteração de perfil.
   - Remoção de permissão.
   - Redefinição administrativa de senha.
   - Revogação explícita da sessão.
4. Preservar as verificações de emissor, assinatura e expiração.
5. Não confiar exclusivamente nas permissões gravadas no JWT.
6. Definir comportamento consistente para múltiplas sessões.
7. Evitar consultas desnecessárias com estratégia de cache segura, se houver justificativa.

### Critério de aceite

Após uma alteração administrativa, uma sessão anteriormente autorizada não pode continuar executando operações que perderam autorização.

Documentar a latência máxima de revogação e testar seu cumprimento.

## 4.3. Corrigir autorização das conexões SSE

Arquivo:

`RealtimeBroadcaster.java`

Implementar:

- Ciclo de vida definido para os emissores.
- Timeout explícito.
- Remoção de conexões encerradas.
- Tratamento de erros e desconexões.
- Encerramento após expiração ou revogação de autorização.
- Heartbeat controlado.
- Limites de conexões e consumo de recursos.

As conexões devem respeitar a autorização atual e não podem permanecer indefinidamente autorizadas.

## 4.4. Revisar a matriz RBAC

Inspecionar:

`RolePermissions.java`

Validar especialmente:

- Acesso de `VIEWER` à leitura de usuários.
- Permissão de `OPERATOR` para teste de cancela.
- Restrições administrativas relacionadas a `SUPER_ADMIN`.
- Autorização de operações manuais e override.

Não alterar arbitrariamente as regras de negócio.

Quando uma permissão depender de decisão operacional, apresentar uma proposta segura, documentar a decisão pendente e não ampliar privilégios por padrão.

Criar testes parametrizados cobrindo os perfis e endpoints sensíveis.

---

# 5. FASE 2 — CONCORRÊNCIA, IDEMPOTÊNCIA E CANCELAS

## 5.1. Corrigir idempotência no processamento Hikvision

Arquivos:

- `HikvisionEventService.java`
- `HikvisionEventIdempotencyLock.java`

Problema:

O lock existe, mas não participa do fluxo efetivo do serviço.

### Implementar

1. Conectar a proteção de concorrência ao processamento real.
2. Definir uma chave de idempotência estável e associada à câmera.
3. Aplicar proteção na camada de persistência.
4. Eliminar a dependência exclusiva de consulta prévia à inserção.
5. Tratar conflitos concorrentes como duplicidade quando comprovadamente corresponderem ao mesmo evento.
6. Diferenciar evento duplicado de colisão ou payload incompatível.
7. Garantir que duplicidades não gerem movimentações ou aberturas adicionais.
8. Não utilizar lock apenas em memória como proteção principal para múltiplas instâncias.
9. Prever eventos sem identificador confiável, utilizando uma estratégia determinística documentada e com limitações explícitas.

### Testes

Executar chamadas simultâneas para o endpoint real, incluindo cenários com múltiplas transações.

**Resultado esperado:** exatamente um efeito de negócio autorizado para um evento único, mesmo diante de reentregas concorrentes.

## 5.2. Corrigir processamento transacional das cancelas

Arquivo:

`GateOperationService.java`

Problema:

`dispatchPending()` chama `dispatch()` no mesmo objeto, impedindo a interceptação transacional esperada pelo proxy do Spring.

### Implementar

1. Separar corretamente a reivindicação e o processamento da operação.
2. Garantir fronteiras transacionais reais.
3. Criar tomada exclusiva da operação com controle de concorrência no banco.
4. Considerar `FOR UPDATE SKIP LOCKED`, atualização condicional atômica ou estratégia equivalente.
5. Persistir identificador de operação, estado, tentativas e histórico.
6. Definir timeouts e recuperação de operações interrompidas.
7. Evitar manter transações de banco abertas durante chamadas HTTP ao controlador físico.
8. Impedir que duas instâncias despachem simultaneamente a mesma operação.
9. Tratar a incerteza após timeout de rede.

**Atenção:** idempotência no banco não garante acionamento físico exatamente uma vez.

Quando o controlador oferecer suporte, utilizar chave de idempotência ou identificação de comando.

Quando não oferecer, evitar repetição automática cega após resultado físico desconhecido. Criar estado `UNKNOWN` ou equivalente e estratégia de reconciliação.

### Testes obrigatórios

- Dois workers concorrentes.
- Timeout.
- Falha antes do envio.
- Falha após possível envio.
- Reinicialização durante processamento.
- Recuperação de operações pendentes.
- Falha permanente.
- Repetição sem acionamento indevido.

## 5.3. Corrigir consistência entre ocupação e passagem física

Arquivo:

`MovementService.java`

Problema:

O sistema registra ocupação antes da confirmação de passagem.

### Implementar uma máquina de estados

Definir estados equivalentes a:

- `DETECTED`
- `AUTHORIZED`
- `GATE_REQUESTED`
- `GATE_OPENED`
- `PASSAGE_CONFIRMED`
- `COMPLETED`
- `DENIED`
- `FAILED`
- `UNKNOWN`
- `RECONCILIATION_REQUIRED`

Adaptar os nomes ao domínio existente.

### Regras

1. Reconhecimento de placa não significa passagem concluída.
2. Autorização não significa ocupação confirmada.
3. Abertura da cancela não significa passagem efetiva.
4. Somente uma confirmação confiável de passagem deve consolidar a ocupação física.
5. Se não existir sensor ou evento confiável, não simular confirmação física.
6. Registrar estados incertos e permitir reconciliação operacional.
7. Evitar abertura duplicada em reprocessamentos.
8. Preservar histórico, auditoria e decisões de acesso.

Criar ADR específico para a estratégia de confirmação e reconciliação.

Não ativar uma automação física nova sem testes com driver simulado e validação operacional.

## 5.4. Corrigir publicação de eventos

Garantir que movimentações normais, manuais e de exceção publiquem eventos de domínio de maneira consistente.

Avaliar a necessidade de Transactional Outbox para não perder eventos após commit.

Garantir que consumidores sejam idempotentes.

---

# 6. FASE 3 — INTEGRIDADE DOS CADASTROS

## 6.1. Respeitar o campo `active`

Verificar os serviços de criação de:

- Apartamentos.
- Moradores.
- Vagas.
- Veículos.

Corrigir o comportamento para respeitar o contrato de entrada e as regras do domínio.

Não permitir que um veículo solicitado como inativo seja criado como ativo por omissão.

## 6.2. Tornar o cadastro completo atômico

Arquivo frontend:

`UnifiedRegistrationPage.tsx`

Problema:

O frontend executa múltiplas requisições independentes.

### Implementar

Criar operação de aplicação no backend para cadastrar, dentro de uma mesma transação de banco:

1. Apartamento.
2. Moradores.
3. Vaga.
4. Veículo.
5. Relacionamentos necessários.

### Requisitos

- DTO específico.
- Validação de entrada.
- Verificação das regras de unicidade.
- Rollback integral em falha.
- Erros de domínio legíveis.
- Proteção contra repetição de requisição, quando necessária.
- Testes de falha em etapas intermediárias.

Atualizar o frontend para consumir o novo contrato.

**Critério:** ou o cadastro é concluído integralmente, ou nenhuma alteração parcial é persistida.

---

# 7. FASE 4 — FRONTEND, SESSÃO E ORGANIZAÇÃO

## 7.1. Sincronizar permissões

Arquivos:

- `auth-context.tsx`
- `ProtectedRoute.tsx`

Implementar:

1. Revalidação da sessão com o backend.
2. Atualização das permissões após mudanças administrativas.
3. Revalidação ao retornar ao aplicativo.
4. Tratamento centralizado de sessão revogada.
5. Atualização de rotas, menus e ações após a revalidação.

O frontend é uma camada de apresentação; todas as decisões sensíveis continuam no backend.

## 7.2. Encerrar sessões expiradas

Implementar controle do prazo de validade da sessão, com encerramento automático e redirecionamento adequado.

Reavaliar a estratégia de armazenamento de tokens. Se for escolhida autenticação baseada em cookies HttpOnly, definir proteção CSRF, atributos Secure/SameSite e impacto na integração SSE.

Não migrar automaticamente o mecanismo sem análise de compatibilidade.

## 7.3. Refatorar componentes grandes

Extrair hooks específicos para:

- Dashboard.
- Portaria.
- Cadastro completo.
- Gerenciamento de formulários.
- Consultas e filtros.

Evitar duplicação de lógica e preservar a experiência do usuário.

## 7.4. Implementar lazy loading

Adicionar divisão de código por rota com `React.lazy` e `Suspense`, quando apropriado.

Preservar proteção de rotas, estados de carregamento e tratamento de erros.

---

# 8. FASE 5 — INFRAESTRUTURA E SEGURANÇA OPERACIONAL

## 8.1. Corrigir cabeçalhos entre proxies

Arquivo:

`frontend/nginx.conf`

Garantir a propagação confiável de:

- IP real do cliente.
- Protocolo HTTPS.
- Host.
- Identificadores de rastreamento.

Configurar explicitamente proxies confiáveis e sanitizar cabeçalhos recebidos de origens não confiáveis.

Não aceitar indiscriminadamente qualquer valor de `X-Forwarded-For`.

Criar testes ou procedimento verificável para confirmar IP e protocolo na auditoria.

## 8.2. Corrigir endpoints Actuator

Arquivos:

- `SecurityConfig.java`
- `docker-compose.prod.yml`

Corrigir a incompatibilidade de:

- `/actuator/health/readiness`
- `/actuator/prometheus`

Permitir healthchecks necessários de forma restrita e segura.

Proteger métricas por rede interna, autenticação adequada ou controles equivalentes.

Validar que Prometheus consiga coletar métricas sem expô-las publicamente.

## 8.3. Corrigir scripts de backup e restauração

Arquivo:

`deploy/scripts/backup-postgres.sh`

Revisar também o script de restauração.

Implementar:

1. Carregamento seguro e explícito das variáveis necessárias.
2. Validação das configurações.
3. Tratamento de falhas.
4. Arquivos temporários seguros.
5. Política de retenção.
6. Proteção de dados sensíveis.
7. Backup dos arquivos e imagens persistidos.
8. Teste de restauração em ambiente isolado.

Não considerar backup operacional até comprovar restauração bem-sucedida.

## 8.4. Melhorar reprodutibilidade

- Substituir `npm install` por `npm ci` no build Docker do frontend.
- Alinhar Gradle Docker e Gradle Wrapper.
- Verificar versões fixadas das imagens.
- Configurar corretamente URLs externas de observabilidade.
- Montar os dashboards existentes do Grafana.
- Validar variáveis obrigatórias no startup.
- Evitar segredos em arquivos versionados.
- Criar pipeline CI/CD com build, lint, testes e verificações de segurança.

## 8.5. Definir política de execução

Documentar o suporte real a uma ou múltiplas instâncias.

Se apenas uma instância for suportada, explicitar essa limitação e garantir que o deploy não assuma escalabilidade horizontal.

Se múltiplas instâncias forem exigidas, tratar eventos SSE, concorrência e recuperação de operações com mecanismos distribuídos apropriados.

---

# 9. FASE 6 — AUDITORIA E RASTREABILIDADE

Criar auditoria administrativa para:

- Criação de usuário.
- Mudança de perfil.
- Alteração de permissões.
- Ativação/desativação.
- Redefinição de senha.
- Revogação de sessão.
- Operações de cancela.
- Movimentação manual e override.

Cada registro deve conter, quando aplicável:

- Identificador do evento.
- Ator.
- Recurso afetado.
- Ação.
- Instante.
- Resultado.
- Motivo.
- Correlation ID.
- Endereço de origem validado.

Garantir que tentativas negadas e operações que falham sejam registradas, conforme política de auditoria, sem depender exclusivamente da transação de negócio que poderá sofrer rollback.

Nunca registrar senhas, tokens ou outros segredos.

---

# 10. FASE 7 — TESTES E QUALIDADE

## 10.1. Frontend

Ampliar testes do cadastro completo cobrindo:

- Sucesso.
- Erro de validação.
- Falha de API.
- Conflito de dados.
- Timeout.
- Repetição.
- Recuperação.
- Revogação de sessão.

Configurar cobertura e limites mínimos progressivos.

Adicionar lint ao pipeline.

## 10.2. Backend

Priorizar testes reais de integração para:

- Autenticação Hikvision.
- Revogação de JWT.
- RBAC.
- Idempotência de eventos.
- Concorrência de movimentações.
- Concorrência de operações de cancela.
- Transações.
- Falhas do controlador físico.
- Consistência de ocupação.
- Auditoria.

Utilizar PostgreSQL e Redis reais via Testcontainers quando necessário.

Testar os serviços efetivos de produção, não apenas classes auxiliares isoladas.

## 10.3. Corrigir contratos E2E

Arquivos:

- `e2e/utils/hikvision.ts`
- `HikvisionEventResponse.java`

Alinhar o helper ao contrato real:

- `eventId`
- `status`
- `movementId`
- `movementStatus`
- `accessDecision`

Não usar conversões TypeScript para mascarar incompatibilidades de JSON.

Atualizar os cenários Playwright.

## 10.4. Corrigir executor E2E

Arquivo:

`e2e/scripts/run-e2e.sh`

Resolver a divergência entre as portas 8080 e 8081.

Evitar portas rígidas quando houver configuração disponível.

## 10.5. Corrigir testes de carga

Arquivo:

`load-test/hikvision.js`

Substituir executor inválido por `constant-arrival-rate`.

Criar cenários separados para:

- Eventos novos.
- Eventos duplicados.
- Concorrência.
- Câmeras diferentes.
- Picos de eventos.
- Requisições inválidas.

Separar métricas de latência, taxa de erros, duplicidade e processamento efetivo.

Os testes de carga não devem acionar cancelas físicas reais.

---

# 11. FASE 8 — VALIDAÇÃO FINAL

Executar, quando o ambiente permitir:

### Frontend

```bash
npm ci
npm run build
npm test -- --run
```

Executar também lint e cobertura após configurar os comandos correspondentes.

### Backend

```bash
./gradlew clean test
./gradlew build
```

### Infraestrutura

```bash
docker compose -f docker-compose.prod.yml config
```

Executar testes de integração e E2E conforme os scripts corrigidos.

### Outras verificações

- Flyway em banco limpo.
- Compatibilidade de migrações com dados existentes.
- Healthchecks.
- Prometheus.
- Grafana.
- Backup/restauração.
- Revogação.
- Autenticação do webhook.
- Concorrência.
- Despacho idempotente.
- Recuperação de falhas.
- Deploy com automação física desativada.

Se um comando falhar devido a limitações do ambiente, registrar o comando, o erro e o teste pendente. Nunca afirmar que passou sem evidência.

---

# 12. DOCUMENTAÇÃO OBRIGATÓRIA

Criar ou atualizar:

```text
docs/
├── REMEDIATION_PLAN.md
├── SECURITY_HARDENING.md
├── GATE_OPERATION_LIFECYCLE.md
├── HIKVISION_WEBHOOK_SECURITY.md
├── RBAC_MATRIX.md
├── DEPLOYMENT_RUNBOOK.md
├── BACKUP_RESTORE.md
├── TEST_REPORT.md
├── RELEASE_READINESS.md
└── ADR/
    ├── ADR-WEBHOOK-AUTH.md
    ├── ADR-SESSION-REVOCATION.md
    ├── ADR-GATE-IDEMPOTENCY.md
    └── ADR-PASSAGE-CONFIRMATION.md
```

Verificar também a exigência anterior de `PHASE19.md`. Se for parte legítima do processo de release, produzir o documento com evidências verdadeiras ou atualizar o validador conforme a estrutura documental oficialmente adotada.

Não criar documentação fictícia para simplesmente fazer o validador passar.

---

# 13. REGRAS DE EXECUÇÃO DO AGENTE

1. Trabalhe por fases, iniciando pelos riscos críticos.
2. Antes de editar, identifique os arquivos e os contratos afetados.
3. Implemente as mudanças em código, não apenas em documentação.
4. Para cada correção, adicione ou atualize testes.
5. Execute os testes possíveis após cada alteração significativa.
6. Não remova testes para obter build aprovado.
7. Não desabilite Spring Security nem reduza a segurança para corrigir erros.
8. Não utilize tratamentos genéricos que escondam falhas.
9. Não exponha endpoints de administração ou métricas à internet.
10. Não apague dados persistidos nem reconfigure equipamentos físicos sem autorização.
11. Não realize acionamentos reais de cancela durante testes automatizados.
12. Não altere contratos públicos sem atualizar consumidores e testes.
13. Preserve compatibilidade com migrações e dados existentes.
14. Mantenha a automação física desativada por padrão até a aprovação operacional.
15. Registre decisões arquiteturais e trade-offs.
16. Não introduza infraestrutura distribuída sem necessidade justificada.
17. Caso uma dependência bloqueie a próxima fase, documente o bloqueio e implemente tudo que puder ser validado com segurança.

---

# 14. FORMATO DE ENTREGA

Ao finalizar cada fase, apresente:

### Fase executada

Nome e objetivo.

### Problemas corrigidos

Descrição do problema e da solução efetivamente implementada.

### Arquivos modificados

Caminhos dos arquivos e responsabilidades das alterações.

### Decisões arquiteturais

Escolhas, alternativas e justificativas.

### Testes

Testes adicionados, executados, aprovados, reprovados e não executados.

### Riscos residuais

Limitações e pendências.

### Próxima fase

Dependências e próximos itens priorizados.

Ao concluir todas as fases, gerar uma matriz consolidada:

| ID       | Problema                | Severidade | Status      | Teste de validação |
| -------- | ----------------------- | ---------- | ----------- | ------------------ |
| SEC-01   | Autenticação webhook    | Crítico    | A verificar | Integração         |
| SEC-02   | Revogação JWT           | Alto       | A verificar | Segurança          |
| GATE-01  | Concorrência cancela    | Alto       | A verificar | Integração         |
| GATE-02  | Confirmação de passagem | Alto       | A verificar | Domínio            |
| DATA-01  | Cadastro transacional   | Alto       | A verificar | Integração         |
| INFRA-01 | Healthchecks            | Alto       | A verificar | Docker             |
| QA-01    | Contratos E2E           | Alto       | A verificar | Playwright         |

Os status deverão refletir evidências reais: `CORRIGIDO E VALIDADO`, `IMPLEMENTADO NÃO VALIDADO`, `PENDENTE` ou `BLOQUEADO`.

---

# 15. DEFINIÇÃO DE PRONTO — PRODUCTION READINESS

**O sistema somente poderá ser classificado como pronto para produção quando:**

- Webhooks estiverem autenticados de forma confiável.
- Nenhum evento não autorizado puder abrir a cancela.
- Sessões e permissões puderem ser revogadas efetivamente.
- Operações físicas tiverem controle de concorrência.
- Falhas ou timeouts não provocarem repetição insegura da abertura.
- A ocupação física não for confundida com mera autorização de entrada.
- Cadastros compostos garantirem integridade transacional.
- Endpoints sensíveis obedecerem ao RBAC.
- Auditoria registrar ações administrativas relevantes.
- Healthchecks e métricas funcionarem com segurança.
- Backup e restauração tiverem sido verificados.
- Contratos E2E estiverem alinhados.
- Build e testes críticos tiverem sido executados com sucesso.
- Não existirem vulnerabilidades críticas conhecidas sem mitigação.
- O comportamento com hardware real tiver sido validado por responsável autorizado.

**Aprovação do software não autoriza automaticamente a abertura física.** A ativação da automação deve ser uma etapa controlada, separada e auditável.

---

# 16. INSTRUÇÃO FINAL

Comece inspecionando o repositório e confrontando os achados desta auditoria com o código atual.

Crie o plano de remediação, identifique as dependências e inicie a implementação pela Fase 1.

Execute as fases subsequentes conforme a ordem de risco e as dependências técnicas.

Não encerre a tarefa após entregar apenas uma análise ou um checklist.

O resultado esperado é um conjunto concreto de correções, testes, documentação e evidências que permita avaliar objetivamente a preparação do sistema para produção.

**Objetivo final: transformar o sistema de estacionamento LPR em uma solução segura, consistente, auditável, testável e operacionalmente confiável, sem comprometer a segurança física das cancelas.**
