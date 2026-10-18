CREATE TABLE apartments (
    id BIGSERIAL PRIMARY KEY,
    number VARCHAR(20) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE residents (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(160) NOT NULL,
    apartment_id BIGINT NOT NULL REFERENCES apartments(id),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_residents_apartment ON residents(apartment_id);
CREATE INDEX idx_residents_active ON residents(active);

CREATE TABLE parking_spaces (
    id BIGSERIAL PRIMARY KEY,
    number VARCHAR(20) NOT NULL UNIQUE,
    apartment_id BIGINT REFERENCES apartments(id),
    type VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'AVAILABLE',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_parking_spaces_type CHECK (type IN ('ASSIGNED', 'RENTED', 'EXTRA', 'VISITOR')),
    CONSTRAINT ck_parking_spaces_status CHECK (status IN ('AVAILABLE', 'OCCUPIED', 'BLOCKED')),
    CONSTRAINT ck_assigned_space_apartment CHECK (type <> 'ASSIGNED' OR apartment_id IS NOT NULL)
);

CREATE INDEX idx_parking_spaces_apartment ON parking_spaces(apartment_id);
CREATE INDEX idx_parking_spaces_status ON parking_spaces(status);
CREATE INDEX idx_parking_spaces_active ON parking_spaces(active);

CREATE TABLE vehicles (
    id BIGSERIAL PRIMARY KEY,
    plate VARCHAR(7) NOT NULL UNIQUE,
    brand VARCHAR(80),
    model VARCHAR(80),
    color VARCHAR(40),
    parking_space_id BIGINT REFERENCES parking_spaces(id),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_vehicles_plate CHECK (plate ~ '^[A-Z0-9]{7}$')
);

CREATE INDEX idx_vehicles_parking_space ON vehicles(parking_space_id);
CREATE INDEX idx_vehicles_active ON vehicles(active);
CREATE UNIQUE INDEX uk_vehicles_active_parking_space
    ON vehicles(parking_space_id)
    WHERE parking_space_id IS NOT NULL AND active = TRUE;

CREATE TABLE vehicle_residents (
    vehicle_id BIGINT NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
    resident_id BIGINT NOT NULL REFERENCES residents(id),
    PRIMARY KEY (vehicle_id, resident_id)
);

CREATE INDEX idx_vehicle_residents_resident ON vehicle_residents(resident_id);
