ALTER TABLE audit_logs
    ADD COLUMN IF NOT EXISTS outcome VARCHAR(16) NOT NULL DEFAULT 'SUCCESS',
    ADD COLUMN IF NOT EXISTS ip_address VARCHAR(45),
    ADD COLUMN IF NOT EXISTS user_agent VARCHAR(512),
    ADD COLUMN IF NOT EXISTS correlation_id VARCHAR(64),
    ADD COLUMN IF NOT EXISTS request_method VARCHAR(16),
    ADD COLUMN IF NOT EXISTS request_path VARCHAR(512);

ALTER TABLE audit_logs
    ADD CONSTRAINT ck_audit_logs_outcome CHECK (outcome IN ('SUCCESS', 'FAILURE'));

CREATE INDEX IF NOT EXISTS idx_audit_logs_action_created ON audit_logs(action, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_correlation ON audit_logs(correlation_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_ip_created ON audit_logs(ip_address, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_request_path ON audit_logs(request_path, created_at DESC);
