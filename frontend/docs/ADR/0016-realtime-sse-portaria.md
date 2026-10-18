# ADR 0016 — Cliente de portaria em tempo real com SSE

- Status: Aceito
- Fase: 10

## Contexto

A interface precisa reagir imediatamente aos eventos operacionais sem polling agressivo.

## Decisão

Consumir `GET /api/v1/realtime/events` com `fetch()` e `Authorization: Bearer`, processando o protocolo `text/event-stream` com um parser SSE próprio.

A conexão reconecta automaticamente com backoff exponencial de 1 s até 15 s. O dashboard mantém um fallback de atualização de 60 s para correção de estado caso algum evento seja perdido.

## Consequências

O frontend não usa o `EventSource` nativo porque a arquitetura atual armazena o JWT em `sessionStorage` e necessita enviar o header `Authorization`.

A página `/portaria` apresenta o último acesso, fluxo de movimentos, alertas, câmeras, ocupação e intervenção manual.
