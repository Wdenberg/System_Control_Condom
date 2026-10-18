# Webhook Hikvision

Contrato: POST /api/v1/integrations/hikvision/events, XML ou multipart, header X-Hikvision-Webhook-Token. Sem JWT de usuário.

Provisionar HIKVISION_ENTRY_WEBHOOK_TOKEN e HIKVISION_EXIT_WEBHOOK_TOKEN com valores diferentes, aleatórios e com pelo menos 32 caracteres. Em cadastro com outros nomes, configurar app.hikvision.webhook.tokens.<NOME-DA-CAMERA>. Chaves são normalizadas para maiúsculas. Nunca versionar ou logar os valores. O serviço resolve câmera habilitada por cadastro e compara apenas seu segredo em tempo constante. Um token de saída não autentica o XML declarado como entrada. Tokens vazios, legado compartilhado e origem declarada no XML são insuficientes.

Quando firmware não permite header: usar gateway de integração em rede isolada da câmera, permitir origem física conhecida apenas no gateway e configurar nele um segredo distinto por câmera para o backend. A câmera não deve acessar diretamente o endpoint público sem autenticação. Não implementar fallback para ipAddress do XML.

Idempotência: advisory lock transacional PostgreSQL por ID interno da câmera e ID externo, seguido de consulta por câmera e índice UNIQUE(camera_id, external_event_id) em V12, além da constraint legada por IP. Mudança de IP não transforma o mesmo UUID em evento novo. V12 recusa históricos com colisões, sem apagar dados; é necessária reconciliação auditada antes de migrar esses casos. UUID reutilizado com placa/instante diferentes retorna conflito. Retransmissão equivalente não recria movimento/cancela. Sem UUID, chave SHA-256 determinística do IP/instante/placa/XML; alterações textuais podem mudar a chave e essa limitação é preservada/documentada. Cooldown não substitui idempotência e eventos em cooldown continuam persistidos. Uma falha com rollback pode manter cooldown até o TTL; operação deve reenviar após essa janela, sem forçar abertura.

Arquivos de imagem gravados são removidos no rollback da transação; falhas de limpeza continuam exigindo rotina operacional de reconciliação. Não há coleta automática de URLs externas.

Testes: HikvisionWebhookSecurityServiceTest e RemediationIntegrationTest cobrem autenticação cruzada, câmera desativada, ausência de credencial, concorrência HTTP, ausência de efeito indevido e duplicidade. Execução integrada pendente conforme TEST_REPORT.md.
