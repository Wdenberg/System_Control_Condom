# ADR 0012 — Autenticação e permissões no frontend

- Status: Accepted
- Date: 2026-09-13

## Context

A API usa JWT e retorna o usuário autenticado com `RoleCode` e `Permission`. O frontend precisa esconder menus e bloquear navegação conforme essas permissions, sem substituir a autorização do backend.

## Decision

- O JWT de curta duração é recebido no login.
- Para este MVP local, a sessão é mantida em `sessionStorage`.
- A API recebe `Authorization: Bearer <token>`.
- A ocorrência de HTTP 401 limpa a sessão e retorna à tela de login.
- A role e as permissions retornadas pelo backend controlam menus e rotas no React.
- O backend continua sendo a autoridade de segurança e deve validar todas as permissions novamente.

## Security note

Para produção, deve-se avaliar um fluxo com cookie HttpOnly/Secure e BFF ou outro desenho que não exponha um token de longa duração ao JavaScript do navegador.
