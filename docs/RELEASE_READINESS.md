# Preparação para produção

**NÃO APROVADO PARA PRODUÇÃO.** Correções implementadas com evidências parciais. Não houve deploy, teste de restauração, acesso à câmera real nem abertura de cancela física.

| ID | Problema | Severidade | Status | Validação / pendência |
|---|---|---|---|---|
| SEC-01 | Autenticação do webhook por câmera | Crítico | IMPLEMENTADO NÃO VALIDADO | Unitários e integrações HTTP passaram na suíte completa (146 testes); firmware/provisionamento reais pendentes |
| SEC-02 | Revogação JWT e authorities atuais | Alto | IMPLEMENTADO NÃO VALIDADO | Reset/role/desativação passaram; suíte Gradle completa aprovada em 20/10 |
| SEC-03 | Expiração/revogação SSE | Alto | IMPLEMENTADO NÃO VALIDADO | Encerramento por revogação passou na suíte completa; demais limites/expiração e carga pendentes |
| SEC-04 | Menor privilégio RBAC | Alto | IMPLEMENTADO NÃO VALIDADO | Cinco perfis e demais testes de autorização da suíte passaram; revisão operacional pendente |
| GATE-01 | Claim exclusivo, lease e UNKNOWN | Alto | IMPLEMENTADO NÃO VALIDADO | Claim/lease e reconciliação passaram com PostgreSQL; RemediationIntegrationTest 25/25 |
| GATE-02 | Passagem confirmada e reservas | Alto | IMPLEMENTADO NÃO VALIDADO | Aceitação sem ocupação, override e reconciliação passaram; inventário e hardware pendentes |
| EVENT-01 | Idempotência real e publicação | Alto | IMPLEMENTADO NÃO VALIDADO | Webhook concorrente, troca de IP e publicação passaram na suíte completa |
| DATA-01 | active e cadastro transacional | Alto | IMPLEMENTADO NÃO VALIDADO | Rollback/repetição concorrente/flags e demais integrações da suíte passaram com PostgreSQL |
| UI-01 | Sessão, hooks, rotas e cadastros | Médio | IMPLEMENTADO NÃO VALIDADO | npm ci, TypeScript, build e 276 testes/42 arquivos aprovados; cobertura de linhas 59,21%. E2E Chromium 6/6 aprovado |
| INFRA-01 | Proxy, management e Docker | Alto | IMPLEMENTADO NÃO VALIDADO | Compose/shell estrutural aprovados; runtime/certificados/scrape pendentes |
| INFRA-02 | Backup e restauração de dados/imagens | Alto | IMPLEMENTADO NÃO VALIDADO | Scripts/isolamento/checksums; restauração real ainda não executada |
| QA-01 | E2E, k6, CI e release | Alto | IMPLEMENTADO NÃO VALIDADO | Backend completo 146/146 e cobertura frontend aprovados localmente; E2E 6/6 aprovado; CI hospedada/carga pendentes |

Critérios externos de liberação: reproduzir os builds/suítes locais aprovados na CI hospedada; validar upgrade das migrations em cópia de dados existentes (banco vazio passou); validar proxy e sondas/scrape/dashboard de produção; restaurar bundle e verificar hashes/contagens; registrar scanner/SBOM/digests das imagens; realizar inventário físico e comissionamento autorizado do controlador. Backend completo, instalação limpa, lint, build e cobertura do frontend passaram localmente em 20/10; ver TEST_REPORT.md. Só então rever a classificação.

Limitações preservadas e explícitas: release de uma instância, SSE sem replay durável, Bearer em sessionStorage com CSP (sem migração parcial para cookie), ausência de sensor confirmado, necessidade de reconciliação de históricos e operações incertas, cooldown pode permanecer até TTL após rollback. O domínio mantém o mapeamento JPA existente; não houve reescrita geral de persistência. Provedor de cobertura fixado no lockfile; CI usa npm ci, sem instalação temporária. `lint` é checagem TypeScript strict/noUnused, não ESLint.
