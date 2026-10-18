# ADR 0013 — Dashboard com atualização periódica

- Status: Accepted
- Date: 2026-09-13

## Context

O backend já usa Redis para cache do dashboard. A interface de portaria precisa apresentar alterações sem exigir WebSocket nesta primeira versão.

## Decision

O frontend chama os endpoints de dashboard em paralelo ao carregar a página e repete a leitura a cada 10 segundos. A atualização manual continua disponível.

## Consequences

- implementação simples e resiliente;
- adequado para a primeira versão operacional;
- a Fase 10 poderá trocar/acompanhar este polling com SSE/WebSocket para eventos em tempo real.
