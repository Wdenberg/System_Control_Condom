# ADR 0015 — Tempo real da portaria com Server-Sent Events

- Status: Aceito
- Fase: 10

## Contexto

A portaria precisa receber imediatamente novos movimentos, eventos ANPR e mudanças da operação da cancela. Polling periódico aumenta o atraso e gera consultas desnecessárias.

## Decisão

Usar **Server-Sent Events (SSE)** entre o Spring Boot e o React.

O endpoint protegido é:

```text
GET /api/v1/realtime/events
```

O navegador usa `fetch()` com `Authorization: Bearer <JWT>` e consome o protocolo `text/event-stream`. Não usamos `EventSource` porque o `EventSource` nativo não permite enviar o header Authorization dessa arquitetura com JWT em `sessionStorage`.

Eventos publicados na Fase 10:

- `movement.created`
- `hikvision.received`
- `gate.updated`
- `connection`
- `heartbeat`

## Consistência

Os eventos gerados dentro de transações são entregues pelo listener na fase `AFTER_COMMIT`. Assim, um cliente não deve receber uma alteração de operação que acabou sendo rollbackada.

## Resiliência

O cliente React:

- mantém no máximo 50 eventos em memória;
- reconecta automaticamente;
- utiliza backoff de 1 s até 15 s;
- mantém atualização de fallback do dashboard a cada 60 s;
- faz a conexão voltar ao estado `DISCONNECTED` após logout.

O backend envia heartbeat a cada 15 s para detectar conexões quebradas.

## Segurança

A stream exige `MOVEMENT_READ`. O token JWT continua sendo validado pelo Spring Security. O endpoint não é público.

## Consequências

### Positivas

- baixa latência para a portaria;
- comunicação unidirecional simples;
- menor custo de infraestrutura que WebSocket para eventos de servidor → cliente;
- compatível com HTTP padrão e proxy reverso.

### Limitações

- SSE é unidirecional;
- ainda não existe replay persistente dos eventos após uma reconexão;
- a aplicação mantém os emitters em memória e, portanto, a versão atual pressupõe uma instância do backend.

Replay, Redis Pub/Sub e múltiplas instâncias podem ser tratados futuramente caso o sistema precise escalar horizontalmente.
