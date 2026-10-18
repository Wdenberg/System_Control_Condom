# ADR 0020 — Interface de auditoria

## Status
Accepted

## Decisão
A tela `/audit` passa a consultar a API de auditoria com filtros de período, usuário, ação, entidade, ID, IP e correlation id, além de paginação e exportação CSV.

A interface mostra operador, data/hora, resultado, ação, entidade, origem HTTP, correlation id e contexto. O frontend usa `AUDIT_READ` para proteger a navegação e depende do backend para a autorização definitiva.
