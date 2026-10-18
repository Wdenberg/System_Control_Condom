# ADR 0016 — Eventos em tempo real da portaria com SSE

- Status: Aceito
- Fase: 10

## Contexto

A portaria precisa receber novos movimentos ANPR, alterações de operações da cancela e eventos de integração sem aguardar polling.

## Decisão

O backend oferece uma stream SSE autenticada em:

```text
GET /api/v1/realtime/events
```

Os clientes precisam possuir `MOVEMENT_READ`.

Os eventos são publicados após o commit de transações quando originados em uma operação transacional. O broadcaster mantém emitters em memória e envia heartbeat periódico.

Eventos:

- `connection`
- `heartbeat`
- `movement.created`
- `hikvision.received`
- `gate.updated`

## Segurança

A stream usa o mesmo JWT da API e não é pública. O endpoint aceita apenas clientes autenticados com a permission operacional `MOVEMENT_READ`.

## Escopo de implantação

A implementação pressupõe uma única instância do backend. Para múltiplas instâncias, o broadcaster deverá ser complementado por um mecanismo distribuído como Redis Pub/Sub.
