# Observabilidade

## Endpoints internos

O servidor de gestão fica separado da API em `MANAGEMENT_PORT` (padrão 8081).

- `/actuator/health`
- `/actuator/health/liveness`
- `/actuator/health/readiness`
- `/actuator/prometheus`
- `/actuator/metrics`

No Docker Compose, a porta de gestão não é publicada para a rede externa; Prometheus acessa o backend pela rede interna.

## Logs

O console usa ECS JSON por padrão (`LOG_STRUCTURED_FORMAT=ecs`). O `correlationId` recebido ou gerado é colocado no MDC e aparece nos logs estruturados.

## Métricas

As métricas padrão do Spring Boot/Micrometer são complementadas por métricas do domínio. Labels são de baixa cardinalidade; placa e UUID não são usados como labels.

Principais métricas:

- `garage_hikvision_events_received_total`
- `garage_hikvision_events_duplicate_total`
- `garage_hikvision_events_cooldown_total`
- `garage_hikvision_events_failed_total`
- `garage_hikvision_processing_seconds`
- `garage_movements_authorized_total{direction=...}`
- `garage_movements_denied_total{direction=...}`
- `garage_movement_decisions_total{decision=...}`
- `garage_gate_operations_requested_total`
- `garage_gate_operations_succeeded_total`
- `garage_gate_operations_failed_total`
- `garage_gate_operations_exhausted_total`
- `garage_realtime_connected_clients`
- `garage_realtime_connections_total`
- `garage_realtime_disconnects_total`

## Prometheus/Grafana

Use:

```bash
docker compose --profile observability up -d --build
```

- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000`

As credenciais do Grafana devem ser fornecidas por `.env`.
