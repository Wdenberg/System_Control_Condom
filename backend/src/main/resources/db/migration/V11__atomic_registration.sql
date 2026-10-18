CREATE TABLE registration_requests (
    actor_id BIGINT NOT NULL REFERENCES app_users(id), request_id UUID NOT NULL,
    payload_hash VARCHAR(64) NOT NULL, response TEXT NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY(actor_id, request_id)
);
