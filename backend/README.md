# Garage Control Backend — Fase 7

Backend do Sistema de Controle de Garagem LPR.

## Stack

- Java 21 LTS
- Spring Boot 4.1.1
- Spring Security + JWT
- PostgreSQL 16
- Flyway
- Redis 7
- Docker / Docker Compose
- JPA / Hibernate
- JUnit 5 / AssertJ / Mockito / Testcontainers

## Fase 7 — API operacional + Dashboard

A fase adiciona uma camada agregadora para o frontend React consumir o estado operacional da garagem sem duplicar regras de negócio no frontend.

### Endpoints

```text
GET /api/v1/dashboard/summary
GET /api/v1/dashboard/activity?limit=20
GET /api/v1/dashboard/occupancy
GET /api/v1/dashboard/cameras
GET /api/v1/dashboard/alerts?limit=20
```

Todos exigem `DASHBOARD_READ`.

### Resumo

O endpoint `/summary` retorna:

```text
Apartamentos ativos
Moradores ativos
Veículos ativos
Veículos dentro/fora
Vagas totais/livres/ocupadas/bloqueadas
Movimentos do dia
Entradas e saídas
Autorizados e negados
Operações de cancela pendentes/esgotadas
```

O dia operacional usa `America/Recife` por padrão.

### Cache

As respostas agregadas são cacheadas no Redis por 5 segundos por padrão. Se Redis ficar indisponível, a aplicação consulta o PostgreSQL e continua respondendo.

```env
DASHBOARD_ZONE_ID=America/Recife
DASHBOARD_CACHE_TTL=PT5S
```

### Ocupação

`/dashboard/occupancy` expõe o estado de cada vaga e, quando disponível, a placa do veículo associado e se ele está dentro da garagem.

### Câmeras

`/dashboard/cameras` expõe as duas câmeras cadastradas e o último evento ANPR persistido:

```text
192.168.15.43 → ENTRY
192.168.15.44 → EXIT
```

### Alertas

O endpoint `/dashboard/alerts` agrega movimentos negados relevantes e operações de cancela com status `EXHAUSTED`.

### Tempo real

Esta fase usa polling pelo frontend. SSE/WebSocket será implementado em uma fase dedicada quando o contrato de eventos em tempo real estiver fechado.

## Execução

```bash
cp .env.example .env
docker compose up -d --build
```

Health:

```bash
curl http://localhost:8080/actuator/health
```

Testes:

```bash
./gradlew test
```

## Nota de compatibilidade — Spring Boot 4

Este projeto usa Spring Boot 4.1.1. A linha 4.x adota Jackson 3 como biblioteca JSON padrão, portanto os imports do código devem usar `tools.jackson.*` em vez de `com.fasterxml.jackson.*` (exceto os módulos de annotations que mantêm o namespace antigo). A dependência `spring-boot-starter-jackson` é declarada explicitamente no build.

A configuração Hikvision também expõe `HIKVISION_ENTRY_HOST`, `HIKVISION_EXIT_HOST`, `HIKVISION_PORT`, `HIKVISION_USERNAME` e `HIKVISION_PASSWORD` para as duas câmeras.

## Fase 10 — Realtime

A API expõe `GET /api/v1/realtime/events` como SSE autenticado por JWT e permission `MOVEMENT_READ`. O stream publica `movement.created`, `hikvision.received`, `gate.updated`, `connection` e `heartbeat`.


## Fase 11 — Controle de cancela preparado para hardware

A Fase 11 mantém o controle de hardware desacoplado do domínio.

Drivers disponíveis:

- `SIMULATED`: recomendado enquanto o controlador físico não foi identificado.
- `HTTP`: adaptador genérico com URL distinta para entrada/saída, Basic ou Bearer.

O endpoint `GET /api/v1/gates/status` informa se cada direção está configurada e distingue modo simulado de adaptador de hardware. O sistema não finge confirmação física: `SENT` significa que o driver aceitou o comando.

Configuração segura inicial:

```env
GATE_ENABLED=false
GATE_DRIVER=SIMULATED
```

### Fase 12

A administração de usuários foi adicionada em `/api/v1/users`. O catálogo de roles e suas permissions fica em `/api/v1/roles`. Roles continuam fixas no código para evitar divergência entre autorização, JWT e matriz `RolePermissions`.

## Fase 15 — Segurança / Hardening

A aplicação agora usa CORS configurável, headers de segurança, rate limiting Redis para login/webhook, webhook Hikvision fail-closed, Actuator mínimo e container runtime não-root. Consulte `PHASE15.md` e `docs/ADR/0021-seguranca-hardening.md`.

## Fase 15 — Segurança / Hardening

Consulte `PHASE15.md` e `docs/SECURITY.md`. O hardening inclui rate limiting, proteção reforçada do webhook Hikvision, headers HTTP, Actuator mínimo e runtime Docker não-root.

## Observabilidade

Use `docker compose --profile observability up -d --build` para subir Prometheus e Grafana localmente. O backend expõe a gestão em `8081`, enquanto a API operacional continua em `8080`.
