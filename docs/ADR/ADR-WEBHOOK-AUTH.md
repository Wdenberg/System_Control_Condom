# ADR — Autenticação do webhook por câmera

Status: implementada; validação integrada pendente.

IP declarado em XML é controlado pelo remetente. Fallback por esse campo foi removido. Cada câmera habilitada usa segredo distinto externo ao banco/código, associado ao nome cadastrado; mínimo 32 caracteres e comparação em tempo constante. Segredo compartilhado legado não é aceito. Firmware incompatível requer gateway autenticado e isolamento físico, sem reduzir a segurança do backend. Rotação de secrets exige atualização coordenada de gateway/câmera e restart. Não há período automático aceitando token antigo.
