# ADR-0018 — Administração de usuários e RBAC no frontend

## Status
Aceito

## Decisão
A interface de usuários usa os endpoints administrativos do backend e respeita `USER_READ`/`USER_WRITE`. A tela de roles usa `ROLE_READ` e trata as roles como catálogo fixo. O frontend nunca cria ou calcula permissions arbitrariamente.

A UI oculta `SUPER_ADMIN` para operadores administrativos sem essa role e não mostra ações administrativas de contas `SUPER_ADMIN` para `ADMIN`.
