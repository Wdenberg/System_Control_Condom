# Fase 16 — E2E

A Fase 16 valida o backend dentro de um fluxo completo de sistema. Não adiciona regras de negócio; garante compatibilidade com autenticação, APIs operacionais, webhook Hikvision, movimentos e SSE.

O ambiente de E2E usa PostgreSQL e Redis descartáveis, webhook token dedicado e `GATE_ENABLED=false`.
