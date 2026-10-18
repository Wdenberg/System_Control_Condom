# Preparação para produção

**NÃO APROVADO PARA PRODUÇÃO.** Correções implementadas com evidências parciais. Não houve deploy, teste de restauração, acesso à câmera real nem abertura de cancela física.

| ID | Problema | Severidade | Status | Validação / pendência |
|---|---|---|---|---|
| SEC-01 | Autenticação do webhook por câmera | Crítico | IMPLEMENTADO NÃO VALIDADO | Unitários passaram com dependências atuais; suíte HTTP negativa completa pendente |
| SEC-02 | Revogação JWT e authorities atuais | Alto | IMPLEMENTADO NÃO VALIDADO | Reset/role/desativação passaram na integração filtrada; suíte oficial completa pendente |
| SEC-03 | Expiração/revogação SSE | Alto | IMPLEMENTADO NÃO VALIDADO | Encerramento por revogação passou na integração filtrada; demais limites/expiração pendentes |
| SEC-04 | Menor privilégio RBAC | Alto | IMPLEMENTADO NÃO VALIDADO | Cinco perfis passaram nos endpoints filtrados; demais endpoints pendentes |
| GATE-01 | Claim exclusivo, lease e UNKNOWN | Alto | IMPLEMENTADO NÃO VALIDADO | Claim concorrente/lease passaram com PostgreSQL; dois cenários de reconciliação corrigidos aguardam nova execução |
| GATE-02 | Passagem confirmada e reservas | Alto | IMPLEMENTADO NÃO VALIDADO | Aceitação sem ocupação e override passaram; dois cenários corrigidos, inventário e hardware pendentes |
| EVENT-01 | Idempotência real e publicação | Alto | IMPLEMENTADO NÃO VALIDADO | Webhook concorrente e troca de IP passaram na integração filtrada; suíte completa pendente |
| DATA-01 | active e cadastro transacional | Alto | IMPLEMENTADO NÃO VALIDADO | Rollback/repetição concorrente/flags passaram com PostgreSQL; demais integrações pendentes |
| UI-01 | Sessão, hooks, rotas e cadastros | Médio | IMPLEMENTADO NÃO VALIDADO | Vitest: 276 testes/42 arquivos; TypeScript e build aprovados. E2E com API real pendente |
| INFRA-01 | Proxy, management e Docker | Alto | IMPLEMENTADO NÃO VALIDADO | Compose/shell estrutural aprovados; runtime/certificados/scrape pendentes |
| INFRA-02 | Backup e restauração de dados/imagens | Alto | IMPLEMENTADO NÃO VALIDADO | Scripts/isolamento/checksums; restauração real BLOQUEADA por Docker |
| QA-01 | E2E, k6, CI e release | Alto | IMPLEMENTADO NÃO VALIDADO | 68 unitários atuais aprovados; integração do usuário: 22/24. Correções prontas para reexecução; E2E/CI pendentes |

Critérios externos de liberação: executar build/suítes oficiais do backend e instalação limpa/cobertura do frontend na CI; validar migrations em banco vazio e cópia de dados existentes; executar E2E, proxy, sondas/scrape/dashboard; restaurar bundle e verificar hashes/contagens; registrar scanner/SBOM/digests das imagens; realizar inventário físico e comissionamento autorizado do controlador. Lint, build e testes do frontend já passaram localmente. Só então rever a classificação.

Limitações preservadas e explícitas: release de uma instância, SSE sem replay durável, Bearer em sessionStorage com CSP (sem migração parcial para cookie), ausência de sensor confirmado, necessidade de reconciliação de históricos e operações incertas, cooldown pode permanecer até TTL após rollback. O domínio mantém o mapeamento JPA existente; não houve reescrita geral de persistência. Provedor de cobertura precisa ser incorporado ao lockfile quando o registry estiver disponível; CI instala temporariamente uma versão exata. `lint` é checagem TypeScript strict/noUnused, não ESLint.
