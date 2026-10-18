ALTER TABLE vehicles
    ADD COLUMN inside_garage BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE movements (
    id BIGSERIAL PRIMARY KEY,
    vehicle_id BIGINT REFERENCES vehicles(id),
    camera_id BIGINT REFERENCES cameras(id),
    parking_space_id BIGINT REFERENCES parking_spaces(id),
    hikvision_event_id BIGINT UNIQUE REFERENCES hikvision_events(id),
    operator_user_id BIGINT REFERENCES app_users(id),
    plate VARCHAR(20) NOT NULL,
    direction VARCHAR(16) NOT NULL,
    source VARCHAR(32) NOT NULL,
    status VARCHAR(16) NOT NULL,
    access_decision VARCHAR(40) NOT NULL,
    confidence_level INTEGER,
    event_time TIMESTAMPTZ NOT NULL,
    justification VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_movements_direction CHECK (direction IN ('ENTRY', 'EXIT')),
    CONSTRAINT ck_movements_source CHECK (source IN ('HIKVISION', 'MANUAL', 'MANUAL_OVERRIDE')),
    CONSTRAINT ck_movements_status CHECK (status IN ('AUTHORIZED', 'DENIED', 'OVERRIDDEN')),
    CONSTRAINT ck_movements_confidence CHECK (confidence_level IS NULL OR confidence_level BETWEEN 0 AND 100),
    CONSTRAINT ck_movements_plate CHECK (plate ~ '^[A-Z0-9]{7}$')
);

CREATE INDEX idx_movements_event_time ON movements(event_time DESC);
CREATE INDEX idx_movements_plate ON movements(plate, event_time DESC);
CREATE INDEX idx_movements_vehicle ON movements(vehicle_id, event_time DESC);
CREATE INDEX idx_movements_status_decision ON movements(status, access_decision);
CREATE INDEX idx_movements_camera ON movements(camera_id, event_time DESC);

CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES app_users(id),
    action VARCHAR(80) NOT NULL,
    entity_type VARCHAR(80) NOT NULL,
    entity_id VARCHAR(80),
    justification VARCHAR(1000),
    details VARCHAR(2000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at DESC);
CREATE INDEX idx_audit_logs_user ON audit_logs(user_id, created_at DESC);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);

CREATE INDEX idx_vehicles_inside_garage ON vehicles(inside_garage);
