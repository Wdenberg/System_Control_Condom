# Plano de remediação

Estado inicial: snapshot sem metadados Git acessíveis. Código e documentos preexistentes preservados em snapshot local; nenhum segredo incluído. Inspeção confirmou os achados abaixo.

| ID | Problema confirmado | Risco | Dependências / estratégia | Aceite |
|---|---|---|---|---|
| SEC-01 | Autenticação por IP do XML | Crítico | Credenciais distintas por câmera, identidade cadastrada, rejeição por padrão | Rejeitados sem efeitos; testes de token cruzado e origem |
| SEC-02 | JWT sem revogação | Alto | Versão persistida de sessão e autoridade atual; invalidação administrativa | Token antigo rejeitado na próxima requisição |
| SEC-03 | SSE sem prazo/revogação | Alto | Sessão validada, prazo, quotas e heartbeat | Expiração/revogação encerra conexão |
| SEC-04 | RBAC divergente da especificação | Alto | Restaurar menor privilégio de VIEWER/OPERATOR; catálogo único | Testes de perfis e endpoints |
| GATE-01 | Self-invocation e despacho sem tomada exclusiva | Alto | Claim transacional separado, lease, resultado UNKNOWN | Dois workers não enviam simultaneamente; timeout não repete cegamente |
| GATE-02 | Ocupação consolidada na autorização | Alto | Ciclo de passagem e reconciliação manual auditada | Autorização/abertura não altera ocupação confirmada |
| EVENT-01 | Lock não utilizado e publicação incompleta | Alto | Advisory lock real, conflito de payload e eventos após commit | Um efeito por evento; publicação em todos os fluxos |
| DATA-01 | active ignorado / cadastro parcial | Alto | Respeitar active; endpoint composto transacional/idempotente | Rollback integral e repetição segura |
| UI-01 | Sessão desatualizada e componentes acoplados | Médio | Revalidação, expiração, hooks e lazy routes | Menus/rotas atualizados e regressões |
| INFRA-01 | Proxy, sondas e métricas incompatíveis | Alto | Cadeia confiável; management somente rede interna | Compose, health e scrape verificáveis |
| INFRA-02 | Backups sem ambiente / imagens | Alto | Configuração explícita, arquivos temporários, retenção e restore isolado | Não declarar operacional antes de restore real |
| QA-01 | E2E divergente, k6 inválido, CI ausente | Alto | Contratos reais, cenários separados, lint/cobertura/CI | Resultados registrados sem simular aprovação |

Sequência: segurança; concorrência e passagem; integridade; frontend; infraestrutura; auditoria; regressões e documentação. Automação física permanece desabilitada por padrão. Nenhuma câmera ou cancela real será acionada.

Limitações iniciais: dependências nativas do frontend incompletas; cache Gradle ausente; socket Docker restrito. Serão tentadas instalações em diretórios graváveis e registradas evidências verdadeiras. A aprovação operacional do hardware e a validação de restauração isolada são critérios externos de liberação.

## Entrega por prioridade solicitada

### P0 — webhook, revogação e autorização
Implementados tokens distintos por câmera, eliminação do fallback XML, versão persistida de sessão/authorities atuais, encerramento SSE e menor privilégio. Arquivos: security/HikvisionWebhookSecurityService, PersistentSessionValidator, JwtTokenService, config/SecurityConfig, domain/user/User, RolePermissions, RealtimeBroadcaster, V9. Decisões nos ADR-WEBHOOK-AUTH e ADR-SESSION-REVOCATION. Unitários atuais passaram; na integração filtrada do usuário, revogação REST por reset/role/desativação, encerramento SSE por revogação e cinco perfis passaram. Próximo aceite: suíte completa de autenticação negativa, expiração/quotas SSE e endpoints administrativos.

### P1 — operação física e concorrência
Claim exclusivo com SKIP LOCKED, HTTP fora da transação, claim/lease/histórico, UNKNOWN sem retry cego, lock do movimento antes da operação na conclusão/reconciliação/retry. Comandos PENDING legados tornam-se UNKNOWN no upgrade. Autorização reserva passagem, ocupação só muda por observação explícita; histórico preservado. Override exige cadastro prévio e vincula recursos para reserva/reconciliação. Idempotência por câmera/UUID continua válida após troca de IP, com índice e verificação de legado em V12. Arquivos: GateDispatchTransactions/Worker, GateOperationService, MovementService/Movement, HikvisionEventService, V10/V12 e frontend/PassageReconciliation. Decisões nos ADR-GATE-IDEMPOTENCY e ADR-PASSAGE-CONFIRMATION. Unitários e cenários PostgreSQL de claim concorrente, lease, idempotência e aceitação sem ocupação passaram. Dois cenários de preparação/contrato foram corrigidos no teste, ainda sem nova execução integrada. Próximo aceite: reexecutar RemediationIntegrationTest, ampliar ensaios de falha e realizar inventário físico.

### P2 — integridade e frontend
active respeitado nos POSTs, transação única com chave de repetição persistida para cadastro completo, bloqueio de alteração de veículo/vaga com passagem pendente e de mudanças estruturais incompatíveis de vagas ocupadas/vinculadas. Sessão revalidada no retorno/foco e a cada 30s, expiração automática, rotas lazy e hooks de Dashboard/Portaria/cadastro. Arquivos: UnifiedRegistrationService/Controller/Request, CRUDs, V11, auth-context, App, hooks e api.ts. Após a instalação do usuário, Vitest/RTL passou com 276 testes em 42 arquivos; TypeScript e build passaram. Rollback, cadastro concorrente e flags active passaram no PostgreSQL da execução filtrada do usuário. Corrigidos foco do Modal e rótulo acessível da placa, além das expectativas de contrato/estado e mocks tipados. Próximo aceite: ampliar falhas intermediárias, validar recuperação após resposta perdida e E2E.

### P3 — operação, qualidade e release
Proxy confiável, redes explícitas, Nginx sem root, management restrito, dashboard montado, wrapper Gradle e npm ci, CI, E2E alinhado, k6 com preflight seguro, backup completo/restore isolado e auditoria administrativa. Arquivos por responsabilidade em validation/changed-files.txt. Compose e shell validados estruturalmente; containers isolados de testes executaram no terminal do usuário. Não houve startup da Compose de produção, scanner remoto, Playwright, restauração ou implantação. Próximo aceite: pipeline oficial e ensaios de operação.

Os quatro grupos têm implementação concreta; somente verificações efetivamente executadas são marcadas como aprovadas no relatório. A prontidão de produção permanece reprovada enquanto os critérios operacionais estiverem pendentes.
