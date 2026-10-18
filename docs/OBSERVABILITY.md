# Observabilidade — Fase 17

## Objetivo

Adicionar diagnóstico operacional ao Garage Control sem expor segredos nem usar dados de alta cardinalidade nas métricas.

## Stack

- Spring Boot Actuator
- Micrometer
- Prometheus
- Grafana
- logs estruturados ECS
- `X-Correlation-Id` + MDC

## Endpoints

Backend API: `http://localhost:8080`

Management: `http://localhost:8081`

- `/actuator/health`
- `/actuator/health/liveness`
- `/actuator/health/readiness`
- `/actuator/prometheus`
- `/actuator/metrics`

## Serviços locais

```bash
docker compose --profile observability up -d --build
```

- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000`

## Segurança

A porta de management é separada da API. No Compose ela fica acessível somente pelo host local; Prometheus acessa o backend pela rede Docker.

Métricas não recebem labels de placa, UUID, IP de câmera ou usuário.
