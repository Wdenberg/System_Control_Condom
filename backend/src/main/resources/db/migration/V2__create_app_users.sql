CREATE TABLE app_users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(80) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(160) NOT NULL,
    role_id BIGINT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_app_users_username UNIQUE (username),
    CONSTRAINT fk_app_users_role FOREIGN KEY (role_id) REFERENCES roles (id)
);

CREATE INDEX idx_app_users_role_id ON app_users (role_id);
CREATE INDEX idx_app_users_active ON app_users (active);
