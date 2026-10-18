ALTER TABLE app_users ADD COLUMN session_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE app_users ADD CONSTRAINT ck_user_session_version CHECK (session_version >= 0);
ALTER TABLE app_users ADD COLUMN row_version BIGINT NOT NULL DEFAULT 0;
