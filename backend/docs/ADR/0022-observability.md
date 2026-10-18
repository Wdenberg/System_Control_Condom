# ADR 0022 — Observabilidade com Actuator, Micrometer, Prometheus e Grafana

## Status

Aceito.

## Contexto

O sistema integra câmeras ANPR, regras de acesso, Redis, PostgreSQL, SSE e um futuro controlador físico de cancela. Falhas precisam ser detectáveis sem expor segredos ou dados de alta cardinalidade nas métricas.

## Decisão

- usar Spring Boot Actuator para health e endpoints de gestão;
- usar Micrometer para métricas;
- expor Prometheus em porta de gestão separada;
- usar logs estruturados ECS no console;
- propagar `X-Correlation-Id` para auditoria e MDC;
- usar Prometheus para retenção e scraping;
- usar Grafana para dashboards locais;
- usar apenas labels de baixa cardinalidade nas métricas customizadas.

## Consequências

Ganham-se diagnóstico, métricas operacionais e dashboards sem acoplamento do domínio a um produto de observabilidade. A retenção e segurança de Prometheus/Grafana precisam ser configuradas no ambiente de implantação.
