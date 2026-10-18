# ADR 0022 — Observabilidade com Actuator, Micrometer, Prometheus e Grafana

## Status

Aceito.

## Contexto

O sistema possui integrações ANPR, Redis, PostgreSQL, SSE e um adaptador de cancela. Precisamos detectar falhas de integração, latência e carga sem adicionar ferramentas ao domínio.

## Decisão

- Spring Boot Actuator para health e management;
- Micrometer para métricas;
- Prometheus para scraping e armazenamento local;
- Grafana para dashboards;
- ECS JSON para logs do console;
- `X-Correlation-Id` propagado para auditoria e MDC;
- management server separado na porta 8081;
- nenhum label de alta cardinalidade nas métricas customizadas.

## Consequências

O diagnóstico passa a ser observável sem expor a API operacional. Prometheus/Grafana são opcionais via Docker Compose profile. Para produção, a porta de management deve permanecer em rede administrativa e Grafana deve usar credenciais fortes.
