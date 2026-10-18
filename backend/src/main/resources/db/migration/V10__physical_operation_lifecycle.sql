ALTER TABLE gate_operations DROP CONSTRAINT ck_gate_operation_status;
ALTER TABLE gate_operations ADD CONSTRAINT ck_gate_operation_status CHECK (status IN ('PENDING','PROCESSING','SENT','EXHAUSTED','DISABLED','UNKNOWN'));
ALTER TABLE gate_operations ADD COLUMN claim_token VARCHAR(64), ADD COLUMN lease_until TIMESTAMPTZ, ADD COLUMN row_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE movements ADD COLUMN passage_state VARCHAR(32) NOT NULL DEFAULT 'RECONCILIATION_REQUIRED',
    ADD COLUMN reservation_active BOOLEAN NOT NULL DEFAULT FALSE, ADD COLUMN passage_confirmed_at TIMESTAMPTZ;
UPDATE movements SET passage_state='DENIED' WHERE status='DENIED';
ALTER TABLE movements ADD CONSTRAINT ck_passage_state CHECK (passage_state IN ('AUTHORIZED','GATE_REQUESTED','COMMAND_ACCEPTED','COMPLETED','DENIED','FAILED','UNKNOWN','RECONCILIATION_REQUIRED'));
CREATE UNIQUE INDEX uk_pending_vehicle_passage ON movements(vehicle_id) WHERE reservation_active=TRUE AND vehicle_id IS NOT NULL;
CREATE UNIQUE INDEX uk_pending_space_entry ON movements(parking_space_id) WHERE reservation_active=TRUE AND direction='ENTRY' AND parking_space_id IS NOT NULL;
CREATE TABLE gate_operation_attempts (
    id BIGSERIAL PRIMARY KEY, operation_id BIGINT NOT NULL REFERENCES gate_operations(id), claim_token VARCHAR(64) NOT NULL UNIQUE,
    outcome VARCHAR(16) NOT NULL, started_at TIMESTAMPTZ NOT NULL, finished_at TIMESTAMPTZ, details VARCHAR(1000)
);
CREATE INDEX idx_gate_attempt_operation ON gate_operation_attempts(operation_id, started_at);

-- Never replay a legacy queued physical command during rollout.
UPDATE gate_operations SET status='UNKNOWN',next_attempt_at=NULL,last_error='Legacy pending command requires operator reconciliation after migration' WHERE status='PENDING';
UPDATE movements m SET passage_state='UNKNOWN' FROM gate_operations g
WHERE g.movement_id=m.id AND g.status='UNKNOWN' AND m.status IN ('AUTHORIZED','OVERRIDDEN');
