# Correção da Fase 10 — teste do Dashboard

O `DashboardPage` passou a depender do `useRealtime()`. O teste de Dashboard é um teste unitário da página e não precisa inicializar `AuthProvider`/`RealtimeProvider`.

O teste agora mocka `useRealtime()` com estado desconectado. Os testes do cliente SSE e do status de realtime continuam cobrindo a infraestrutura de realtime separadamente.

Esperado:

```text
Test Files  6 passed (6)
Tests       12 passed (12)
```
