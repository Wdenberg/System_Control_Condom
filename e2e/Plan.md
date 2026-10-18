# Plano de Testes E2E — Controle de Garagem

## Visão Geral

Este documento define o plano de testes end-to-end (E2E) do sistema Controle de Garagem, cobrindo autenticação, fluxos operacionais, RBAC, segurança de webhooks e integração com hardware de cancela.

---

## Infraestrutura

| Componente | Tecnologia | Porta |
|---|---|---|
| Frontend | Vite + React 19 | 5173 |
| Backend | Spring Boot | 8080 |
| Banco | PostgreSQL 16 | 5433 |
| Cache | Redis 7 | 6380 |
| E2E Framework | Playwright 1.63.0 | — |
| Browser | Chromium (headless) | — |

**Arquivos de configuração:**
- `e2e/playwright.config.ts` — configuração do Playwright
- `e2e/package.json` — dependências e scripts
- `e2e/.env` — variáveis de ambiente de teste
- `docker-compose.e2e.yml` — infraestrutura Docker
- `e2e/scripts/run-e2e.sh` — script de execução completo

---

## Cenários de Teste

### 1. Autenticação e Autorização

| ID | Cenário | Status | Prioridade |
|---|---|---|---|
| AUTH-01 | Login com credenciais válidas → redireciona para Dashboard | ✅ Implementado | Alta |
| AUTH-02 | Login com credenciais inválidas → exibe erro | ❌ Não implementado | Alta |
| AUTH-03 | Acesso sem token → redireciona para Login | ❌ Não implementado | Alta |
| AUTH-04 | Logout → sessão encerrada, redireciona para Login | ❌ Não implementado | Alta |
| AUTH-05 | Token expirado → redireciona para Login | ❌ Não implementado | Média |

### 2. Fluxo Operacional (Portaria)

| ID | Cenário | Status | Prioridade |
|---|---|---|---|
| OPS-01 | Evento ANPR → movimentação registrada → SSE atualiza Portaria | ✅ Implementado | Alta |
| OPS-02 | Veículo com vaga → decisão AUTORIZADO | ✅ Implementado | Alta |
| OPS-03 | Veículo sem vaga → decisão DENIED | ✅ Implementado | Alta |
| OPS-04 | Placa desconhecida → decisão DENIED | ✅ Implementado | Alta |
| OPS-05 | Evento duplicado (UUID) → idempotência garantida | ✅ Implementado | Alta |
| OPS-06 | Cancela simulada (GATE_ENABLED=false) → nenhum hardware acionado | ✅ Implementado | Alta |
| OPS-07 | Cancela habilitada → acionamento físico verificado | ❌ Não implementado | Média |
| OPS-08 | Movimentação manual via UI → registro com justificativa | ❌ Não implementado | Alta |
| OPS-09 | Teste de cancela via UI → comando enviado | ❌ Não implementado | Média |

### 3. RBAC (Controle de Acesso por Função)

| ID | Cenário | Status | Prioridade |
|---|---|---|---|
| RBAC-01 | ATTENDANT não acessa páginas admin (Users, Roles, Audit) | ✅ Implementado | Alta |
| RBAC-02 | VIEWER não acessa páginas de escrita | ❌ Não implementado | Alta |
| RBAC-03 | OPERATOR acessa Portaria e Movimentações | ❌ Não implementado | Média |
| RBAC-04 | ADMIN acessa tudo exceto SUPER_ADMIN | ❌ Não implementado | Média |
| RBAC-05 | Permissão negada → exibe tela "Acesso negado" | ❌ Não implementado | Alta |

### 4. Segurança de Webhooks

| ID | Cenário | Status | Prioridade |
|---|---|---|---|
| SEC-01 | Token inválido → 401 Unauthorized | ✅ Implementado | Alta |
| SEC-02 | Token ausente → 401 Unauthorized | ❌ Não implementado | Alta |
| SEC-03 | Payload inválido → 400 Bad Request | ❌ Não implementado | Média |
| SEC-04 | UUID duplicado → previne movimentação dupla | ✅ Implementado | Alta |

### 5. CRUD — Gestão do Sistema

| ID | Cenário | Status | Prioridade |
|---|---|---|---|
| CRUD-01 | Criar, listar, editar, desativar apartamento | ❌ Não implementado | Alta |
| CRUD-02 | Criar, listar, editar, desativar morador | ❌ Não implementado | Alta |
| CRUD-03 | Criar, listar, editar, desativar veículo | ❌ Não implementado | Alta |
| CRUD-04 | Criar, listar, editar, desativar vaga | ❌ Não implementado | Alta |
| CRUD-05 | Criar, listar, editar, desativar usuário | ❌ Não implementado | Alta |
| CRUD-06 | Criar, listar, editar, desativar câmera | ❌ Não implementado | Média |

### 6. Relatórios e Auditoria

| ID | Cenário | Status | Prioridade |
|---|---|---|---|
| RPT-01 | Relatório de acessos com filtros de data | ❌ Não implementado | Média |
| RPT-02 | Relatório de ocupação | ❌ Não implementado | Média |
| RPT-03 | Relatório de operações da cancela | ❌ Não implementado | Média |
| RPT-04 | Exportação CSV | ❌ Não implementado | Baixa |
| RPT-05 | Auditoria — histórico de ações rastreável | ❌ Não implementado | Média |

### 7. Mapa de Vagas

| ID | Cenário | Status | Prioridade |
|---|---|---|---|
| MAP-01 | Visualização grid com todas as vagas | ❌ Não implementado | Média |
| MAP-02 | Filtro por status (Disponível/Ocupada/Bloqueada) | ❌ Não implementado | Média |
| MAP-03 | Filtro por tipo (Própria/Alugada/Extra/Visitante) | ❌ Não implementado | Média |
| MAP-04 | Busca por vaga, apartamento ou morador | ❌ Não implementado | Baixa |

### 8. Observabilidade

| ID | Cenário | Status | Prioridade |
|---|---|---|---|
| OBS-01 | Página de observabilidade carrega dados do servidor | ❌ Não implementado | Baixa |
| OBS-02 | Health check endpoint acessível | ❌ Não implementado | Baixa |

---

## Resumo de Cobertura

| Categoria | Implementados | Total | Cobertura |
|---|---|---|---|
| Autenticação | 1 | 5 | 20% |
| Fluxo Operacional | 6 | 9 | 67% |
| RBAC | 1 | 5 | 20% |
| Webhook Security | 2 | 4 | 50% |
| CRUD | 0 | 6 | 0% |
| Relatórios | 0 | 5 | 0% |
| Mapa de Vagas | 0 | 4 | 0% |
| Observabilidade | 0 | 2 | 0% |
| **Total** | **10** | **40** | **25%** |

---

## Prioridades de Implementação

### Fase 1 — Crítico (Alta prioridade)

1. **AUTH-02** — Login com credenciais inválidas
2. **AUTH-03** — Acesso sem token
3. **AUTH-04** — Logout
4. **RBAC-02** — VIEWER sem acesso admin
5. **RBAC-05** — Tela "Acesso negado"
6. **CRUD-01** — Apartamento (criar, listar, editar, desativar)
7. **CRUD-03** — Veículo (criar, listar, editar, desativar)
8. **CRUD-05** — Usuário (criar, listar, editar, desativar)
9. **OPS-08** — Movimentação manual via UI
10. **SEC-02** — Token ausente

### Fase 2 — Importante (Média prioridade)

11. **AUTH-05** — Token expirado
12. **OPS-07** — Cancela habilitada
13. **OPS-09** — Teste de cancela via UI
14. **RBAC-03** — OPERATOR acessa Portaria
15. **RBAC-04** — ADMIN acessa tudo
16. **CRUD-02** — Morador
17. **CRUD-04** — Vaga
18. **CRUD-06** — Câmera
19. **SEC-03** — Payload inválido
20. **RPT-01** — Relatório de acessos
21. **RPT-02** — Relatório de ocupação
22. **RPT-03** — Relatório de cancela
23. **RPT-05** — Auditoria
24. **MAP-01** — Visualização grid
25. **MAP-02** — Filtro por status
26. **MAP-03** — Filtro por tipo

### Fase 3 — Desejável (Baixa prioridade)

27. **RPT-04** — Exportação CSV
28. **MAP-04** — Busca
29. **OBS-01** — Observabilidade
30. **OBS-02** — Health check

---

## Melhorias de Infraestrutura

| ID | Melhoria | Prioridade | Status |
|---|---|---|---|
| INFRA-01 | CI/CD pipeline (GitHub Actions) | Alta | ❌ Não implementado |
| INFRA-02 | Integrar scripts E2E no package.json do frontend | Alta | ❌ Não implementado |
| INFRA-03 | Thresholds de cobertura no Vitest | Média | ❌ Não implementado |
| INFRA-04 | Cross-browser testing (Firefox, WebKit) | Média | ❌ Não implementado |
| INFRA-05 | MSW para mocks de API em testes de integração | Média | ❌ Não implementado |
| INFRA-06 | Testes visuais (screenshot comparison) | Baixa | ❌ Não implementado |

---

## Como Executar

### Pré-requisitos
- Docker e Docker Compose instalados
- Node.js 20+
- Playwright e Chromium instalados (`npx playwright install chromium`)

### Executar E2E
```bash
cd e2e
bash scripts/run-e2e.sh
```

### Executar apenas os testes (se infra já estiver rodando)
```bash
cd e2e
npx playwright test
```

### Executar unit tests (frontend)
```bash
cd frontend
npm test
```

### Gerar relatório HTML do Playwright
```bash
cd e2e
npx playwright show-report
```

---

## Fixtures e Helpers

| Arquivo | Função |
|---|---|
| `e2e/utils/api-client.ts` | `loginApi()`, `authorizedRequest()`, `seedParkingScenario()` |
| `e2e/utils/hikvision.ts` | `buildAnprXml()`, `sendHikvisionEvent()` |
| `e2e/utils/test-data.ts` | `uniqueSuffix()`, `uniquePlate()`, `uniqueApartment()`, `e2eUserPassword()` |

---

## Notas

- O gateway de cancela está desabilitado nos testes (`GATE_ENABLED=false`)
- Rate limiting está desabilitado nos testes
- O Playwright inicia automaticamente o frontend via `webServer` config
- Todos os testes rodam em Chromium headless com 1 worker (sem paralelismo)
- Trace, screenshot e video são salvos apenas em caso de falha
