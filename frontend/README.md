# Garage Control Frontend — Fase 9

Frontend React + TypeScript + Vite integrado ao backend Spring Boot.

## Entregas
- autenticação JWT e RBAC herdados da Fase 8;
- dashboard operacional;
- CRUD de apartamentos;
- CRUD de moradores dentro do contexto do apartamento;
- CRUD de vagas;
- CRUD de veículos;
- busca e filtros client-side;
- tratamento de erros 401/403/409 e mensagens de validação;
- desativação por `active` em vez de exclusão física;
- TypeScript estrito.

## Backend esperado
`http://localhost:8080`

## Desenvolvimento
```bash
npm install
npm run dev
```

## Testes
```bash
npm test
npm run build
```

A Fase 9 usa somente endpoints que já existem no backend da Fase 7. Não foi introduzido endpoint DELETE porque o domínio preserva histórico com `active=false`.

## Fase 10 — Portaria em tempo real

A aplicação mantém uma conexão SSE autenticada com o backend e atualiza Dashboard/Portaria após eventos operacionais. O proxy Nginx está configurado com buffering desativado para preservar a entrega imediata do stream.


## Fase 11

A portaria passa a mostrar o estado do driver da cancela e, para usuários com `GATE_TEST`, oferece teste operacional. Em modo `SIMULATED` o botão permanece bloqueado para evitar confundir simulação com acionamento físico.

### Fase 12

A rota `/users` agora consome a API administrativa de usuários e `/roles` exibe o catálogo de roles e permissions do backend.

## Fase 15 — Segurança / Hardening
O Nginx aplica CSP, X-Frame-Options, Referrer-Policy, Permissions-Policy e outros headers de segurança. O proxy substitui o `X-Forwarded-For` pelo IP do cliente observado pelo Nginx.

## Observabilidade

A rota `/observability` apresenta o estado básico do backend e links para Prometheus/Grafana configurados por `VITE_PROMETHEUS_URL` e `VITE_GRAFANA_URL`.
