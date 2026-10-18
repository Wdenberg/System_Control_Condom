# Segurança implementada

P0: autenticação do webhook exige segredo distinto (mínimo 32 caracteres) associado ao nome da câmera cadastrada e habilitada. IP do XML é apenas identificação declarada. O segredo compartilhado legado e `enforceRemoteIp` não autenticam requisições. Segredos ausentes resultam em rejeição; valores repetidos, curtos ou placeholders CHANGE_ME impedem configuração. Em APP_ENVIRONMENT=production, banco com credencial padrão e câmera habilitada sem segredo impedem startup. XML limitado a 1 MiB e parser com bloqueio de DTD/entidades externas.

JWT: migration V9 acrescenta session_version e row_version. Tokens antigos sem versão são recusados. Assinatura, emissor e expiração são preservados. Na próxima autorização após commit, a versão/atividade do usuário e a atividade da role são lidas no PostgreSQL. Authorities são calculadas da matriz atual; não se confia nas authorities gravadas no token. Desativação, troca de role, redefinição de senha e POST /api/v1/auth/revoke invalidam todas as sessões desse usuário. Requisições já autorizadas/em andamento não são retroativamente desfeitas. Não há cache de autorização.

SSE: prazo máximo de 15 minutos e nunca além da expiração do JWT; 200 conexões globais, 3 por usuário. Cada transmissão revalida sessão/permissão; heartbeat a cada 15 segundos em operação normal fecha sessões revogadas mesmo sem novos eventos. Timeout e callbacks removem conexões. O scheduler tem duas threads para o despacho HTTP não ocupar sozinho o heartbeat. A latência operacional de encerramento e comportamento com clientes lentos precisam de validação integrada.

RBAC: VIEWER não lê usuários, OPERATOR não testa cancela; somente driver simulado aceita a operação de teste pela aplicação. Hardware HTTP exige GATE_HARDWARE_APPROVED=true no startup, além de GATE_ENABLED=true. Ambos ficam desabilitados no exemplo de produção. Não foram alterados equipamentos ou secrets locais.

Auditoria: ações administrativas guardam ator e alvo; tentativas HTTP sensíveis processadas pela cadeia de segurança têm registro independente em REQUIRES_NEW, incluindo falhas/negações. Nenhum corpo, senha, token ou hash de senha é registrado. Falha de persistência da auditoria é erro observável, não sucesso silencioso.

Proxy: apenas o peer IP configurado é confiável para o resolvedor de IP. Edge substitui cabeçalhos recebidos; frontend aceita real_ip somente do edge. Management não tem porta pública e a API de usuário nega /actuator/**. Health e scrape são liberados somente na porta management, em redes internas.

Ver TEST_REPORT.md e RELEASE_READINESS.md: implementação não equivale a homologação de produção.

Rejeições antecipadas de payload/tamanho pelo edge ou filtro de limite são registradas no acesso HTTP, sem ler o corpo para auditoria. Credenciais são verificadas antes de processamento de negócio.
