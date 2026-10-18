-- Fase 12: o modelo existente de usuários já contém todos os dados necessários.
-- Esta migration cria índices auxiliares para as consultas administrativas.
CREATE INDEX IF NOT EXISTS idx_app_users_full_name ON app_users (full_name);
CREATE INDEX IF NOT EXISTS idx_app_users_created_at ON app_users (created_at);
