# Fase 15 — Segurança / Hardening

## Backend
- CORS configurável.
- Headers HTTP de segurança.
- CSP mínima na API.
- HSTS opcional.
- Rate limiting Redis para login e webhook Hikvision.
- Webhook Hikvision fail-closed por token ou IP.
- Comparação constant-time do token.
- Forwarded headers confiáveis somente quando explicitamente configurado.
- Actuator somente health.
- Erros sem stack trace/mensagem interna.
- Limite de cabeçalho HTTP.
- Runtime Docker não-root, `no-new-privileges`, `cap_drop=ALL` e root filesystem read-only.

## Frontend
- Nginx com CSP e headers de segurança.
- `X-Forwarded-For` substituído pelo IP real do proxy.

## Validação
Executar no ambiente do projeto:
- `./gradlew clean test`
- `npm test`
- `npm run build`
