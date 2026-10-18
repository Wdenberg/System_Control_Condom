CREATE TABLE cameras (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    ip_address VARCHAR(45) NOT NULL UNIQUE,
    direction VARCHAR(16) NOT NULL,
    vendor VARCHAR(32) NOT NULL,
    model VARCHAR(100) NOT NULL,
    channel_id INTEGER NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_cameras_direction CHECK (direction IN ('ENTRY', 'EXIT'))
);

CREATE TABLE hikvision_events (
    id BIGSERIAL PRIMARY KEY,
    camera_id BIGINT REFERENCES cameras(id),
    source_ip_address VARCHAR(45) NOT NULL,
    remote_ip_address VARCHAR(45),
    external_event_id VARCHAR(128) NOT NULL,
    hikvision_uuid VARCHAR(128),
    event_type VARCHAR(32) NOT NULL,
    event_state VARCHAR(32),
    event_description VARCHAR(128),
    channel_id INTEGER,
    channel_name VARCHAR(128),
    mac_address VARCHAR(32),
    occurred_at TIMESTAMPTZ NOT NULL,
    received_at TIMESTAMPTZ NOT NULL,
    plate VARCHAR(20),
    original_plate VARCHAR(32),
    country_code VARCHAR(16),
    line_number INTEGER,
    capture_direction VARCHAR(16),
    confidence_level INTEGER,
    plate_type VARCHAR(32),
    plate_color VARCHAR(32),
    license_bright INTEGER,
    dangmark VARCHAR(16),
    two_wheel_vehicle VARCHAR(16),
    three_wheel_vehicle VARCHAR(16),
    plate_char_believe VARCHAR(512),
    vehicle_type VARCHAR(64),
    detect_dir INTEGER,
    detect_type INTEGER,
    alarm_data_type INTEGER,
    vehicle_index INTEGER,
    vehicle_color_depth INTEGER,
    vehicle_color VARCHAR(64),
    vehicle_length INTEGER,
    vehicle_logo_recog INTEGER,
    vehicle_sub_logo_recog INTEGER,
    vehicle_model INTEGER,
    vehicle_list_name VARCHAR(64),
    plate_size INTEGER,
    picture_count INTEGER,
    is_data_retransmission BOOLEAN NOT NULL DEFAULT FALSE,
    payload_hash VARCHAR(64) NOT NULL,
    processing_status VARCHAR(24) NOT NULL,
    processing_error VARCHAR(2000),
    raw_xml TEXT NOT NULL,
    CONSTRAINT uk_hikvision_event_source_external UNIQUE (source_ip_address, external_event_id),
    CONSTRAINT ck_hikvision_processing_status CHECK (processing_status IN ('RECEIVED', 'COOLDOWN', 'DUPLICATE', 'REJECTED', 'ERROR'))
);

CREATE INDEX idx_hikvision_events_occurred_at ON hikvision_events (occurred_at DESC);
CREATE INDEX idx_hikvision_events_plate ON hikvision_events (plate);
CREATE INDEX idx_hikvision_events_camera ON hikvision_events (camera_id, occurred_at DESC);
CREATE INDEX idx_hikvision_events_status ON hikvision_events (processing_status);

CREATE TABLE hikvision_pictures (
    id BIGSERIAL PRIMARY KEY,
    hikvision_event_id BIGINT NOT NULL REFERENCES hikvision_events(id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    type VARCHAR(64) NOT NULL,
    data_type INTEGER,
    pic_recog_mode INTEGER,
    absolute_time VARCHAR(32),
    picture_id VARCHAR(128),
    picture_url VARCHAR(1000),
    plate_x INTEGER,
    plate_y INTEGER,
    plate_width INTEGER,
    plate_height INTEGER,
    storage_path VARCHAR(1000),
    content_type VARCHAR(128),
    content_size BIGINT,
    content_sha256 VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_hikvision_picture_event_pid_filename UNIQUE (hikvision_event_id, picture_id, file_name)
);

CREATE INDEX idx_hikvision_pictures_event ON hikvision_pictures (hikvision_event_id);

INSERT INTO cameras (name, ip_address, direction, vendor, model, channel_id, enabled, created_at, updated_at)
VALUES
    ('CAM-ENTRADA', '192.168.15.43', 'ENTRY', 'HIKVISION', 'iDS-2CD7A46G0/P-IZHS', 1, TRUE, NOW(), NOW()),
    ('CAM-SAIDA', '192.168.15.44', 'EXIT', 'HIKVISION', 'iDS-2CD7A46G0/P-IZHS', 1, TRUE, NOW(), NOW())
ON CONFLICT (ip_address) DO NOTHING;
