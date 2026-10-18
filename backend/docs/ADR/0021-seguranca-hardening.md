# ADR 0021 — Segurança e Hardening

> Atualização: as decisões de autenticação, sessão e ocupação deste ADR foram substituídas pelos ADRs de remediação em `docs/ADR/ADR-WEBHOOK-AUTH.md`, `ADR-SESSION-REVOCATION.md` e `ADR-PASSAGE-CONFIRMATION.md` na raiz do projeto.


## Status
Accepted

## Contexto
O sistema recebe eventos de dispositivos físicos e expõe APIs autenticadas para a operação da garagem. Antes da produção precisamos reduzir superfícies de ataque sem acoplar segurança ao domínio.

## Decisões
1. JWT continua stateless e o CSRF permanece desabilitado porque a API usa Bearer Token e não sessão/cookie de autenticação.
2. CORS é configurável e nunca usa `*` em conjunto com credenciais.
3. O Actuator expõe somente health por HTTP.
4. Login e webhook Hikvision recebem rate limiting Redis com fail-closed por padrão.
5. O webhook Hikvision exige token válido ou whitelist de IP. Sem um mecanismo de proteção, o endpoint recusa a requisição.
6. Tokens são comparados em tempo constante.
7. Cabeçalhos `X-Forwarded-*` não são confiáveis por padrão; precisam ser explicitamente habilitados atrás de proxy confiável.
8. O container de runtime do backend executa como usuário não-root.
9. Frontend servido por Nginx aplica CSP e headers de segurança.
10. Mensagens/stack traces internos não são expostos por respostas de erro do Spring Boot.

## Consequências
- A configuração local precisa incluir `HIKVISION_ENFORCE_REMOTE_IP=true` ou `HIKVISION_WEBHOOK_TOKEN`.
- Em produção, HSTS deve ser habilitado somente quando o acesso for HTTPS.
- O uso de Redis torna o rate limiter dependente de Redis quando `SECURITY_RATE_LIMIT_FAIL_OPEN=false`.
