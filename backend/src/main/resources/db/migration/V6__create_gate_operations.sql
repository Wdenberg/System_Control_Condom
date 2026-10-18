CREATE TABLE gate_operations (
    id BIGSERIAL PRIMARY KEY,
    operation_key VARCHAR(64) NOT NULL UNIQUE,
    movement_id BIGINT NOT NULL UNIQUE REFERENCES movements(id),
    command VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL,
    driver VARCHAR(32) NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    last_error VARCHAR(2000),
    requested_at TIMESTAMPTZ NOT NULL,
    next_attempt_at TIMESTAMPTZ,
    sent_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_gate_operation_command CHECK (command IN ('OPEN')),
    CONSTRAINT ck_gate_operation_status CHECK (status IN ('PENDING', 'SENT', 'EXHAUSTED', 'DISABLED')),
    CONSTRAINT ck_gate_operation_attempt_count CHECK (attempt_count >= 0)
);

CREATE INDEX idx_gate_operations_pending ON gate_operations(status, next_attempt_at, created_at);
CREATE INDEX idx_gate_operations_created_at ON gate_operations(created_at DESC);
