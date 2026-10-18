# ADR 0022 — E2E com Playwright

## Status

Accepted

## Contexto

Os testes unitários e de integração cobrem componentes e o backend. A Fase 16 precisa validar o comportamento completo: navegador, frontend, API, PostgreSQL, Redis, webhook Hikvision e SSE.

## Decisão

Usar `@playwright/test` 1.63.0 em projeto separado (`/e2e`). O ambiente sobe PostgreSQL, Redis e backend via Docker Compose; o frontend é iniciado pelo `webServer` do Playwright.

A suíte roda somente Chromium nesta fase para reduzir tempo e variabilidade. O hardware da cancela não participa; `GATE_ENABLED=false` mantém o teste seguro.

## Razões

- Testa a aplicação como usuário real.
- Isolamento de browser context.
- Auto-wait e assertions web-first.
- Trace, screenshot e vídeo em falhas.
- Integração real entre frontend e backend.
