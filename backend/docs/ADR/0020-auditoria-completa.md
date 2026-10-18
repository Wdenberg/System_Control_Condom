# ADR 0020 — Auditoria completa e contexto de requisição

## Status
Accepted

## Contexto
A auditoria já registrava ações sensíveis, mas não preservava de forma estruturada a origem e o contexto HTTP da operação. Para um sistema de controle de garagem é importante rastrear operador, ação, entidade, justificativa, IP, User-Agent, caminho, método e correlation id.

## Decisão
Evoluir `audit_logs` com `outcome`, `ip_address`, `user_agent`, `correlation_id`, `request_method` e `request_path`. Um filtro HTTP gera/preserva `X-Correlation-Id`, coloca o contexto em `AuditRequestContext` e `AuditService.record` o incorpora ao log.

A consulta de auditoria terá filtros por período, usuário, ação, entidade, entidade/ID, IP e correlation id. A exportação será CSV limitada a 50.000 registros por requisição.

## Consequências
- Rastreamento operacional e forense mais forte.
- Auditoria funciona tanto para ações HTTP quanto para ações internas, quando houver contexto de requisição.
- Eventos assíncronos sem requisição continuam válidos e terão contexto HTTP nulo.
- CSV não substitui controles de acesso; permanece protegido por `AUDIT_READ`.
