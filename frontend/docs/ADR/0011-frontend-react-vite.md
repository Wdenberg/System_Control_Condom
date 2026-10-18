# ADR 0011 — Frontend com React + Vite + TypeScript

- Status: Accepted
- Date: 2026-09-13

## Context

O sistema precisa de uma interface local para operação da garagem, consumindo a API Spring Boot em `localhost:8080`, com autenticação JWT e autorização por permission.

## Decision

Usar uma SPA com React + TypeScript, empacotada com Vite, e React Router para navegação.

A integração HTTP será centralizada em `src/services/api.ts`. Autenticação e estado do usuário ficam em `AuthProvider`. O frontend usa as permissions retornadas pelo backend para esconder/permitir rotas e ações, mas a segurança efetiva permanece no backend.

## Consequences

- desenvolvimento simples em `localhost:5173`;
- proxy `/api` para o Spring Boot durante desenvolvimento;
- tipagem explícita dos contratos da API;
- fácil evolução para realtime e CRUD;
- token em `sessionStorage` neste MVP local; produção deve avaliar HttpOnly cookie/BFF.
