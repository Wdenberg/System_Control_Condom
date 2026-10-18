# Fase 17 — Observabilidade

Implementada com Micrometer/Actuator, Prometheus e Grafana.

## Endpoints de gestão

- `GET http://localhost:8081/actuator/health`
- `GET http://localhost:8081/actuator/health/liveness`
- `GET http://localhost:8081/actuator/health/readiness`
- `GET http://localhost:8081/actuator/prometheus`
- `GET http://localhost:8081/actuator/metrics`

A porta de gestão fica separada da API principal.

## Métricas de negócio

- eventos Hikvision recebidos, duplicados, cooldown e falhas;
- latência de processamento ANPR;
- movimentos autorizados/negados por direção;
- decisões de acesso;
- operações de cancela solicitadas, sucesso, falhas e esgotadas;
- conexões SSE e clientes conectados.

Não são usados rótulos de alta cardinalidade como placa ou UUID.
