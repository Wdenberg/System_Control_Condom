# Análise técnica — Frontend e Backend

Atualização: 2026-10-20. Avaliação do código fornecido e do estado após a remediação. A inspeção estática identifica desenho e riscos; resultados executados estão separados em [TEST_REPORT.md](TEST_REPORT.md). Implementação não implica homologação.

## Frontend

### Estrutura de código

**Pontos fortes:** React/TypeScript com páginas por função, cliente HTTP centralizado em `frontend/src/services/api.ts`, tipos de transporte em `types/api.ts` e funções de apresentação/validação reutilizáveis. Dashboard e Portaria compartilham `useOperationalDashboard`; o cadastro completo usa `useUnifiedRegistration`. Rotas usam carregamento sob demanda. O cadastro passa a enviar uma única operação transacional com identificador estável para repetição após falha de resposta. A reconciliação física tem componente próprio e exige observação/justificativa.

**Débitos e riscos:** os tipos TypeScript não são gerados a partir do contrato do backend; alterações exigem atualização coordenada e testes de contrato. Ainda há páginas CRUD com formulário, consulta e renderização no mesmo componente. O cliente concentra muitos recursos em um arquivo, tornando revisão e evolução mais difíceis. A próxima extração deve seguir módulos de negócio, sem criar abstrações genéricas prematuras. O ciclo de passagem depende do operador; a tela não constitui evidência automática de sensor.

### RBAC e sessão

**Pontos fortes:** `ProtectedRoute`, menus e ações usam permissions recebidas do backend. A exceção por nome SUPER_ADMIN foi removida; a autoridade efetiva vem da matriz central do servidor. A sessão é revalidada na abertura, retorno de foco/visibilidade e periodicamente; expiração e 401 encerram a sessão. Respostas antigas de `/me` não substituem uma sessão nova. Logout solicita revogação persistida e encerra imediatamente a sessão local.

**Débitos e riscos:** Bearer em sessionStorage continua acessível a JavaScript e, portanto, a uma eventual falha XSS. CSP reduz exposição, mas não elimina esse risco. Migração para cookie HttpOnly/BFF exige desenho completo de CSRF, CORS e logout; não foi feita parcialmente. Falhas de rede no logout podem impedir a revogação remota até expiração/administração. Ocultação de menu e cache local de permissões não protegem a API; mudanças administrativas só devem ser consideradas autorizadas pelo backend. A latência visual de atualização depende da revalidação.

### Infraestrutura

**Pontos fortes:** build com `npm ci`, imagem de execução Nginx sem root e porta interna 8080. O frontend substitui headers de encaminhamento e confia apenas no edge configurado. URLs opcionais de Grafana/Prometheus são argumentos explícitos de build; links localhost foram removidos da configuração padrão de produção.

**Débitos e riscos:** bundle, fallback da SPA, CSP, cache, proxy e SSE ainda precisam de ensaio real no Nginx. Endereços fixos de rede exigem coerência entre Compose, proxy e resolvedor do backend. URLs Vite são incorporadas no build e exigem nova imagem para alteração. Scans, SBOM e fixação de imagens por digest ainda não têm evidência de execução.

### Testes

**Pontos fortes:** suíte Vitest/React Testing Library cobre rotas, menus, formulários, cadastros, sessão e reconciliação, além de funções puras e cliente HTTP. Foram adicionadas regressões para expiração, revalidação, permissões, repetição segura e confirmação de controlador em resultado UNKNOWN. Cobertura tem configuração e limites iniciais explícitos. O smoke executado usa código real de armazenamento de sessão e cliente HTTP.

**Débitos e riscos:** os bindings foram disponibilizados pela instalação do usuário; 276 testes em 42 arquivos, checagem TypeScript e build passaram nesta revisão. Em 20/10, npm ci, lint, build e cobertura passaram com o provider fixado no lockfile: linhas 59,21%, statements 53,17%, branches 51,65% e funções 43,39%. CI usa a instalação pelo lockfile. O comando `lint` verifica TypeScript strict/noUnused; não é ESLint. Playwright passou com seis cenários Chromium contra API real, incluindo login, Portaria/SSE, RBAC e idempotência. As regressões corrigiram perda de foco no modal e o nome acessível da placa durante erro de validação.

## Backend

### Estrutura de código e integridade

**Pontos fortes:** monólito organizado em web, application, domain e infrastructure, com módulos de negócio identificáveis. DTOs e Bean Validation delimitam a API. XML Hikvision é convertido em representação interna; normalização, armazenamento e driver de cancela têm componentes próprios. PostgreSQL/Flyway preservam evolução de schema. O cadastro composto é uma transação única, com chave persistida de repetição e conflito de payload. Flags active agora são respeitadas.

O despacho separa claim transacional, chamada física fora da transação e conclusão com verificação de claim/lease. Locks e índices de reserva impedem autorizações concorrentes para veículo/vaga. Idempotência de evento usa câmera persistida, advisory lock e índice único, inclusive após mudança de IP. Autorização e HTTP 2xx não alteram ocupação: somente confirmação observada consolida entrada/saída. Override vincula recursos sob lock e não supera reservas ou conflitos físicos de ocupação.

**Débitos e riscos:** o domínio ainda contém entidades/repositórios JPA e alguns serviços de aplicação dependem de DTO web/JdbcTemplate; as fronteiras desejadas pelo AGENTE.md não estão integralmente desacopladas. Não houve reescrita geral da persistência. MovementService ainda concentra decisões, reservas e reconciliação; a evolução deve extrair políticas explícitas com testes comportamentais. Contratos manuais e construtores de compatibilidade usados por testes aumentam manutenção.

O controlador HTTP genérico não oferece garantia comprovada de uma única abertura física. Timeout, interrupção, lease vencida ou resposta ambígua viram UNKNOWN, sem retry automático. A resolução exige operador/controlador, e histórico legado exige inventário. V12 interrompe a migração se encontrar colisões históricas de câmera/UUID, preservando os dados. V1–V12 passaram em banco vazio na suíte completa; upgrade de banco existente ainda precisa ser exercitado.

### RBAC e segurança

**Pontos fortes:** autorização por Permission com catálogo único `RolePermissions`; VIEWER perdeu USER_READ e OPERATOR perdeu GATE_TEST. O servidor impõe as regras independentemente da interface. JWT mantém validação criptográfica/emissor/tempo e acrescenta versão persistida de sessão, atividade de conta/role e autoridades atuais. Reset de senha, troca de role, desativação e revogação invalidam tokens anteriores. SSE revalida sessão antes de transmitir, possui prazo e quotas.

Webhook exige segredo distinto por câmera cadastrada/habilitada; IP declarado no XML não autentica. Configuração ausente rejeita acesso, e segredos compartilhados/curtos/placeholders são recusados. Payload XML tem limite e bloqueio de entidades externas. Operações administrativas e tentativas sensíveis têm auditoria sem corpos ou credenciais. Automação física habilitada requer aprovação operacional explícita de configuração.

**Débitos e riscos:** consultas persistidas de sessão/autoridade por requisição e por conexão SSE elevam carga; medir antes de otimizar, preservando a revogação. SSE é local à instância, sem replay durável, e clientes lentos ainda precisam de ensaio. Revogação não desfaz requisições já autorizadas em andamento. Segredo estático de webhook depende de TLS, provisionamento/rotação e capacidade real da câmera de enviar o header; se o firmware não o suportar, usar gateway autenticador, sem restaurar fallback por IP. A suíte completa de integrações passou em 20/10; expiração/quotas SSE e carga ainda precisam de ensaios específicos.

### Infraestrutura e operação

**Pontos fortes:** Java21, wrapper Gradle no build, runtime sem root, redes separadas e portas de banco/Redis/management sem publicação em produção. Cadeia de proxy explícita e Actuator negado na porta pública da aplicação. Health e métricas usam management interno. Backup inclui dump, imagens e manifesto de hashes; restore usa projeto/volumes isolados e verifica arquivos referenciados pelo banco. Runbooks e critérios de release registram os passos operacionais.

**Débitos e riscos:** somente uma instância é declarada suportada. Containers de teste e backend E2E iniciaram; Compose de produção, scrape/dashboard, restauração, scanner e recuperação continuam sem homologação. Backup exige pausa coordenada das escritas; não é snapshot online distribuído. Certificados precisam de permissões adequadas ao UID do Nginx e processo externo de renovação. Tags ainda precisam de digests/SBOM e evidências de vulnerabilidades. Dados brutos/imagens e auditoria exigem política operacional de retenção, acesso e capacidade de disco. Recuperação real e comportamento sob falta de disco/Redis/DB permanecem critérios de homologação.

### Testes

**Pontos fortes:** unitários de parser/XXE, placa, matriz de permissões, segredos, sessão, ciclo de passagem, driver e worker. Testcontainers/MockMvc foram ampliados para concorrência real, idempotência, rollback/repetição de cadastro, perfis, revogação, SSE, lease e reconciliação. O teste HTTP que depende de socket foi preservado apesar da restrição local. CI separa frontend, backend e E2E e não declara aprovação operacional automática.

**Débitos e riscos:** em 20/10, ./gradlew clean test build --no-daemon --no-build-cache passou: 146 testes em 41 classes, nenhum ignorado; RemediationIntegrationTest 25/25 com PostgreSQL/Redis. V1–V12 foram aplicadas em banco vazio e o JAR foi gerado. A reexecução identificou e corrigiu a desserialização do controllerSettled opcional; omissão/null/false preservam o bloqueio de UNKNOWN. A primeira execução E2E reproduziu erro 500 por leitura de moradores fora da transação; listagem/detalhe agora montam o DTO no serviço transacional, com regressão integrada. Corrigidos os seletores e a navegação/sincronização dos testes; seis cenários Chromium passaram. Upgrade com dados legados e restore continuam pendentes; Docker foi disponibilizado mediante execução autorizada fora do sandbox. As métricas e alertas precisam de carga e falhas simuladas em ambiente isolado; k6 contém preflight que exige alvo com cancela desabilitada e driver simulado. Ver BACKEND_TEST_ANALYSIS.md.

## Ordem de liberação

P0: executar regressões oficiais de autenticação de câmera, revogação e perfis. P1: validar concorrência PostgreSQL, recuperação de lease e operação incerta sem hardware real. P2: executar rollback/idempotência do cadastro, TypeScript/build/RTL e contrato frontend/backend. P3: pipeline completa, proxy/SSE/health, restauração isolada, scans e inventário/comissionamento autorizado. O estado atual e as evidências estão em [RELEASE_READINESS.md](RELEASE_READINESS.md); produção permanece não aprovada.
