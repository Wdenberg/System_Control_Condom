# Fases do Backend

## Fase 1 — Fundação

Java 21, Spring Boot, PostgreSQL, Flyway, Redis, Docker e testes base.

## Fase 2 — Autenticação + RBAC

JWT, usuários, roles e permissions.

## Fase 3 — Hikvision / ISAPI

Eventos ANPR, parser XML, UUID, idempotência, Redis cooldown, imagens e cadastro das câmeras.

## Fase 4 — Apartamentos + Vagas + Veículos

### Objetivo

Criar a base cadastral capaz de responder:

```text
placa → veículo → morador → apartamento → vaga
```

### Entregas

- CRUD de apartamentos.
- CRUD de moradores dentro de apartamentos.
- CRUD de vagas.
- Tipos de vaga: ASSIGNED, RENTED, EXTRA, VISITOR.
- Status de vaga: AVAILABLE, OCCUPIED, BLOCKED.
- CRUD de veículos.
- Relação veículo ↔ moradores.
- Atribuição opcional veículo ↔ vaga.
- Normalização e unicidade de placa.
- Validação de consistência apartamento/morador/vaga.
- RBAC usando as permissions existentes.
- Flyway V4.
- Testes unitários e de integração com PostgreSQL/Redis.

### Critério de aceite

1. Criar apartamento.
2. Criar morador no apartamento.
3. Criar vaga `ASSIGNED` para o apartamento.
4. Criar veículo com o morador e a vaga.
5. Consultar veículo pela placa e obter apartamento e vaga.
6. Impedir placa duplicada.
7. Impedir morador de outro apartamento no mesmo veículo.
8. Impedir vaga de outro apartamento.
9. Impedir dois veículos ativos na mesma vaga.
10. Garantir `401/403` conforme RBAC.

## Fase 5 — Movimentações + regras de acesso ✅

### Objetivo

Transformar uma leitura ANPR em uma decisão operacional de entrada/saída sem ainda acionar a cancela física.

### Entregas

- Movimento automático originado pela Hikvision.
- Entrada usa a direção cadastrada da câmera: .43 = ENTRY e .44 = EXIT.
- Resolução placa → veículo → morador/apartamento → vaga.
- Estado `inside_garage` do veículo.
- Estado `AVAILABLE/OCCUPIED` da vaga.
- Decisões explícitas de acesso.
- Lock pessimista para evitar duas entradas concorrentes na mesma vaga.
- Movimento manual com usuário autenticado e justificativa.
- Override somente com `MOVEMENT_OVERRIDE` e justificativa.
- Auditoria de movimentações manuais e overrides.
- API paginada de movimentos e auditoria.
- Flyway V5.
- Testes de regras, integração PostgreSQL/Redis e RBAC.

### Decisões principais

- Entrada de veículo conhecido exige veículo ativo, morador/apartamento ativo, vaga válida e vaga disponível.
- Veículo já dentro gera `DUPLICATE_ENTRY`.
- Saída exige veículo ativo e estado `inside_garage=true`.
- Saída sem entrada registrada gera `EXIT_WITHOUT_ENTRY`.
- Baixa confiança ANPR gera `LOW_CONFIDENCE` antes da decisão cadastral.
- Override pode autorizar um movimento negado, inclusive desconhecido, mas só altera ocupação quando o estado físico/lógico pode ser representado sem inconsistência.
- PostgreSQL é a fonte da verdade; Redis permanece como aceleração/cooldown.

### Critério de aceite

1. Entrada conhecida ocupa a vaga e marca o veículo como dentro.
2. Segunda entrada do mesmo veículo é negada como duplicada.
3. Vaga ocupada por outro veículo impede nova entrada.
4. Saída autorizada libera a vaga e marca o veículo como fora.
5. Veículo desconhecido gera movimento negado.
6. Baixa confiança gera `LOW_CONFIDENCE`.
7. Movimento manual exige autenticação, permission e justificativa.
8. Override exige `MOVEMENT_OVERRIDE` e registra auditoria.
9. Concorrência não permite duas ocupações na mesma vaga.
10. API de movimentos e auditoria respeita RBAC.

## Fase 6 — Controle da cancela

Integração com controlador/relé e comandos de abertura.

## Fase 7 — API operacional + Dashboard para React ✅

### Objetivo

Disponibilizar uma API agregada para o frontend React, evitando que o dashboard precise montar indicadores a partir de múltiplas chamadas de baixo nível.

### Entregas

- `GET /api/v1/dashboard/summary`.
- `GET /api/v1/dashboard/activity`.
- `GET /api/v1/dashboard/occupancy`.
- `GET /api/v1/dashboard/cameras`.
- `GET /api/v1/dashboard/alerts`.
- Indicadores de apartamentos, moradores, veículos, ocupação, movimentações e operações de cancela.
- Resolução diária usando `America/Recife` por padrão.
- Cache Redis com TTL configurável (5s por padrão).
- Fallback para PostgreSQL quando Redis estiver indisponível.
- Último evento recebido por câmera no painel.
- Alertas de movimentos negados e operações de cancela esgotadas.
- Limites de consulta para impedir cargas excessivas.
- RBAC via `DASHBOARD_READ`.
- Testes unitário e de integração do contrato do dashboard.

### Contrato

```text
GET /api/v1/dashboard/summary
GET /api/v1/dashboard/activity?limit=20
GET /api/v1/dashboard/occupancy
GET /api/v1/dashboard/cameras
GET /api/v1/dashboard/alerts?limit=20
```

### Critério de aceite

1. Usuário com `DASHBOARD_READ` carrega o resumo completo.
2. Usuário sem `DASHBOARD_READ` recebe `403`.
3. O resumo considera o dia do fuso `America/Recife`.
4. O dashboard continua funcionando quando Redis está indisponível.
5. Limites de `limit` são respeitados, com máximo de 50.
6. Ocupação exibe vaga, apartamento, placa e estado do veículo quando disponíveis.
7. Câmeras exibem o último evento ANPR persistido.
8. Alertas exibem apenas negativas relevantes e falhas de cancela.
9. O dashboard não executa regras de acesso nem aciona a cancela.

### Evolução prevista

Esta fase usa polling pelo frontend. SSE/WebSocket será implementado separadamente quando o contrato de eventos em tempo real estiver definido.

## Fase 10 — Portaria em tempo real ✅

- Endpoint SSE autenticado `/api/v1/realtime/events`.
- Eventos de movimentos, Hikvision e cancela.
- Entrega após commit transacional.
- Heartbeat configurável por `REALTIME_HEARTBEAT_INTERVAL_MS`.
- Cliente web conectado ao fluxo em tempo real.


## Fase 11 — Hardware de cancela

A camada de hardware agora está pronta para receber um controlador físico sem alterar o domínio. O driver `SIMULATED` permanece seguro para desenvolvimento; `HTTP` é um adaptador genérico até que o modelo/protocolo real seja definido. O backend não declara abertura física confirmada quando o equipamento não fornece feedback.

## Fase 12 — Gestão administrativa de usuários e RBAC

- API de usuários com leitura, criação, edição, ativação/desativação e reset de senha.
- API de catálogo de roles e permissions.
- Proteção `USER_READ`, `USER_WRITE` e `ROLE_READ`.
- Bloqueio de auto-desativação/demissão do próprio usuário.
- Garantia de ao menos um `SUPER_ADMIN` ativo.
- Roles continuam sendo um catálogo fixo (`RoleCode`) com permissions derivadas por `RolePermissions`.


## Fase 13 — Relatórios
Implementação de relatórios operacionais e exportação CSV.


## Fase 14 — Auditoria completa
Status: implementada.

## Fase 15 — Segurança / Hardening
- CORS e headers configuráveis
- rate limiting Redis
- webhook Hikvision fail-closed
- Actuator mínimo
- non-root Docker
- hardening Nginx/CSP

## Fase 15 — Segurança / Hardening
- segurança HTTP
- rate limiting
- webhook protegido
- Docker não-root
- Actuator mínimo
- CSP/Nginx

## Fase 17 — Observabilidade

- Actuator em porta de gestão separada;
- health/liveness/readiness;
- Micrometer + Prometheus;
- métricas customizadas do domínio;
- logs ECS + correlation ID;
- Prometheus/Grafana via Compose profile `observability`;
- página React de observabilidade.
