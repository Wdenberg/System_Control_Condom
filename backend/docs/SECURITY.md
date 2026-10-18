# Security Checklist — Garage Control

## Desenvolvimento local
- Nunca commitar `.env` ou segredos.
- Gerar `JWT_SECRET_BASE64` com `openssl rand -base64 32`.
- Manter `GATE_DRIVER=SIMULATED` e `GATE_ENABLED=false` sem hardware real.
- Preferir `HIKVISION_ENFORCE_REMOTE_IP=true` ou `HIKVISION_WEBHOOK_TOKEN`.

## Produção
- HTTPS obrigatório no proxy; então habilitar `SECURITY_HSTS_ENABLED=true`.
- Fixar `CORS_ALLOWED_ORIGINS` aos hosts reais do frontend.
- Se houver proxy confiável, usar `SECURITY_TRUST_FORWARDED_HEADERS=true` e `SERVER_FORWARD_HEADERS_STRATEGY=framework`.
- Não usar `SECURITY_RATE_LIMIT_FAIL_OPEN=true` sem uma justificativa operacional.
- Revisar limites de login e webhook.
- Não expor Actuator além de health.
- Trocar todas as credenciais padrão.
- Fazer backup e proteção dos volumes PostgreSQL/Redis.
