# ADR — Revogação persistente de sessões

Status: implementada; integração e latência operacional pendentes.

Adotou-se session_version persistida com controle otimista de concorrência. Todas as sessões de uma conta são invalidadas ao mudar senha, role, atividade ou revogar explicitamente. PostgreSQL é consultado em cada autorização, e authorities são derivadas do cadastro/matriz atual. Sem cache para evitar janela de permissões antigas. Custo: leitura adicional por requisição; aceitável na operação de instância única. Tokens anteriores à migration exigem novo login.

Latência REST: próxima autorização após commit. SSE: próxima transmissão/heartbeat (15s em operação saudável) e sempre até a expiração/timeout do emissor. Teste integrado de encerramento foi adicionado, ainda não executado aqui. Chamadas em andamento não são retroativamente canceladas. Mantido Bearer/sessionStorage por compatibilidade, CSP e expiração/revalidação; não houve migração incompleta para cookies sem CSRF.
