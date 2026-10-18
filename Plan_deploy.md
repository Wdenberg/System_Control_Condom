# Plano de Deploy — Garage Control 1.0.0

## Arquivos que NÃO EXISTEM no projeto

| Arquivo | Referenciado por | Status |
|---|---|---|
| `PHASE19.md` | `deploy/scripts/validate-release.sh:23` | **MISSING** — script de validação vai falhar |
| `PRODUCTION.md` | `README-PHASE18.md:4` | **MISSING** — documentação de deploy ausente |
| `Agente.md` | Solicitado pelo usuário | **MISSING** — não existe no projeto |
| `Agente_Test.md` | Solicitado pelo usuário | **MISSING** — não existe no projeto |
| `Spac_Driver_Develope.md` | Solicitado pelo usuário | **MISSING** — não existe no projeto |

---

## Estrutura de Deploy: O que JÁ EXISTE e está completo

| Componente | Status | Arquivos |
|---|---|---|
| `docker-compose.prod.yml` | OK | Postgres, Redis, Backend, Frontend, Edge (nginx), Prometheus, Grafana |
| `backend/Dockerfile` | OK | Multi-stage build (Gradle → JRE 21) |
| `frontend/Dockerfile` | OK | Multi-stage build (Node → nginx) |
| `deploy/nginx/nginx.conf` | OK | TLS 1.2/1.3, HTTP→HTTPS redirect, HSTS, CSP, proxy para frontend |
| `deploy/scripts/healthcheck.sh` | OK | Verifica edge HTTPS + backend readiness |
| `deploy/scripts/backup-postgres.sh` | OK | pg_dump com gzip |
| `deploy/scripts/restore-postgres.sh` | OK | Restore com confirmação interativa |
| `deploy/scripts/validate-release.sh` | **BUG** | Referencia `PHASE19.md` que não existe |
| `.env.prod.example` | OK | Template completo com 76 variáveis |
| `docs/OPERATIONS-RUNBOOK.md` | OK | Comandos de init, log, backup, restore |
| `docs/INSTALLATION-CHECKLIST.md` | OK | Checklist de rede, câmeras, app |
| `docs/SECURITY-CHECKLIST.md` | OK | 13 itens de segurança |
| `docs/API-CONTRACT.md` | OK | Contrato Frontend↔Backend completo |
| `docs/RELEASE-1.0.0.md` | OK | Componentes e estado do release |
| `docs/OBSERVABILITY.md` | OK | Stack Actuator+Prometheus+Grafana |
| `load-test/hikvision.js` | OK | Teste de carga k6 para webhook |

---

## O que FALTA para o deploy

### 1. Arquivos obrigatórios ausentes

- **`.env.prod`** — Criar a partir de `.env.prod.example` com valores reais (senhas, IPs, domínio)
- **`deploy/certs/fullchain.pem`** e **`deploy/certs/privkey.pem`** — Certificados TLS para nginx. Sem eles o container `edge` não inicia
- **`PHASE19.md`** — O script `validate-release.sh` falha sem ele
- **`PRODUCTION.md`** — Documentação de deploy referenciada mas inexistente

### 2. Nginx Edge: rota webhook não separada

O `deploy/nginx/nginx.conf` roteia **tudo** para `frontend:80`. O frontend internamente proxya `/api/` para `backend:8080`. Isso funciona, mas o webhook da Hikvision (`POST /api/v1/integrations/hikvision/events`) passa pelo frontend nginx antes de chegar ao backend. Idealmente, uma rota separada no edge proxyaria `/api/v1/integrations/` diretamente ao backend para reduzir latência e eliminar dependência do container frontend no path do webhook.

### 3. Observabilidade em produção

- Prometheus e Grafana usam `profiles: ["observability"]` no compose prod — **não iniciam por padrão**
- Não há `ports:` mapeados para eles no compose prod (apenas `expose`)
- Não há rota nginx para acessar Grafana/Prometheus externamente
- Para acessar, só via `docker exec` ou adicionando port mapping + nginx location

### 4. Scripts de deploy ausentes

- Não existe um script `deploy.sh` ou `deploy-init.sh` que orquestre: copiar `.env.prod.example` → `.env.prod`, gerar certs (Let's Encrypt), validar, subir containers
- Não há automação de geração de certificados TLS (ex: certbot/Let's Encrypt)

### 5. Backup automático

- `backup-postgres.sh` existe mas não há cron job ou agendamento automático
- Não há `.gitkeep` no diretório `backups/` (funciona mas pode confundir)

### 6. Frontend build com variáveis de ambiente

- O `frontend/Dockerfile` faz `npm run build` mas não passa `ARG` ou `ENV` para Vite
- Em produção o `VITE_API_BASE_URL` precisa ser `/api` (já é o default do `.env.example`)
- Se precisar de valores diferentes, o Dockerfile precisa de `ARG VITE_API_BASE_URL=/api` e `ENV VITE_API_BASE_URL=$VITE_API_BASE_URL`

### 7. Validação do script `validate-release.sh`

O script referencia arquivos que não existem:
- `PHASE19.md` (linha 23) — **vai causar FAIL**
- Os demais arquivos referenciados existem

---

## Checklist para deploy funcional

| # | Item | Prioridade |
|---|---|---|
| 1 | Criar `.env.prod` com valores reais | **CRÍTICO** |
| 2 | Gerener/colocar certificados TLS em `deploy/certs/` | **CRÍTICO** |
| 3 | Criar `PHASE19.md` (ou ajustar `validate-release.sh`) | **CRÍTICO** |
| 4 | Criar `PRODUCTION.md` com guia de deploy | **MÉDIO** |
| 5 | Adicionar rota webhook direta no edge nginx (opcional) | **MÉDIO** |
| 6 | Automatizar geração de certificados (certbot) | **MÉDIO** |
| 7 | Adicionar cron de backup automático | **BAIXO** |
| 8 | Validar `VITE_API_BASE_URL` no Dockerfile do frontend | **BAIXO** |
