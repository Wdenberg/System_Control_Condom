# Release 1.0.0

## Estado

Release candidate / homologação.

## Componentes

- Backend: Java 21 + Spring Boot 4.1.1.
- Banco: PostgreSQL 16.
- Cache: Redis 7.
- Frontend: React + TypeScript + Vite.
- E2E: Playwright.
- Observabilidade: Actuator + Micrometer/Prometheus + Grafana.

## Físico

- Hikvision ENTRY: `192.168.15.43`.
- Hikvision EXIT: `192.168.15.44`.
- Cancela: `SIMULATED` até identificação do controlador.

## Não incluso como aceite automático

A execução de uma suíte local comprova software, não substitui homologação física das câmeras/cancela.
