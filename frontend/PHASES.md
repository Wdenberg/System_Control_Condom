# Fases do Frontend

## Fase 8 — React + TypeScript + Dashboard

Base da aplicação, autenticação, navegação protegida e dashboard.

## Fase 9 — CRUD operacional

Apartamentos, moradores, vagas e veículos.

## Fase 10 — Portaria em tempo real ✅

### Objetivo

Entregar uma interface de operação de portaria que reage aos eventos do backend sem depender de polling como mecanismo principal.

### Entregas

- conexão SSE autenticada;
- reconexão com backoff;
- indicador de conectividade em tempo real;
- eventos `movement.created`, `hikvision.received` e `gate.updated`;
- atualização imediata do dashboard após eventos relevantes;
- página dedicada `/portaria`;
- último acesso em destaque;
- fluxo de acessos em tempo real;
- câmeras, alertas e ocupação;
- movimentação manual com justificativa;
- fallback periódico do dashboard;
- testes do parser SSE e estado visual da conexão.

### Critério de aceite

1. Usuário autenticado com `MOVEMENT_READ` conecta à stream.
2. Usuário sem `MOVEMENT_READ` recebe 403.
3. Um novo movimento publicado pelo backend aparece na portaria.
4. Evento Hikvision não derruba a conexão.
5. Falha de conexão inicia reconexão automática.
6. Heartbeat mantém a conexão viva.
7. Logout encerra a stream.
8. Dashboard continua tendo atualização de fallback.


## Fase 11 — integração física preparada

O frontend mantém a portaria preparada para diagnóstico e teste do driver da cancela, sem assumir um fabricante/protocolo.

## Fase 12 — Gestão de usuários e RBAC

- Tela de usuários com busca e filtros.
- Criação, edição, ativação/desativação e reset de senha.
- Tela de roles e permissions efetivas.
- Proteção por `USER_READ`, `USER_WRITE` e `ROLE_READ`.


## Fase 13 — Relatórios
Implementação de relatórios operacionais e exportação CSV.


## Fase 14 — Auditoria completa
Status: implementada.

## Fase 17 — Observabilidade

- tela `/observability`;
- status do backend e conexões SSE;
- links para Prometheus e Grafana;
- atualização automática a cada 15 segundos.
