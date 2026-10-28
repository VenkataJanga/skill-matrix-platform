-- ============================================================
-- V14 — Password Reset Tokens (Development/Pilot Forgot Password)
-- Skill Matrix Platform
--
-- Mirrors the existing refresh_tokens pattern: only a SHA-256 hash
-- of the one-time reset token is stored. The raw token is generated
-- in-memory, sent to the user via email (or admin-assisted/DEV-only
-- channel), and never persisted or logged.
--
-- No existing migration (V01-V13) is modified.
-- ============================================================

CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    public_id          VARCHAR(36)  NOT NULL,
    user_id            BIGINT       NOT NULL,
    token_hash         VARCHAR(255) NOT NULL,
    expires_at         DATETIME     NOT NULL,
    used_at            DATETIME     NULL,
    requested_from_ip  VARCHAR(64),
    is_active          TINYINT(1)   NOT NULL DEFAULT 1,
    created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_password_reset_tokens        PRIMARY KEY (id),
    CONSTRAINT uq_password_reset_tokens_pubid  UNIQUE (public_id),
    CONSTRAINT fk_prt_user                     FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_prt_token_hash ON password_reset_tokens (token_hash);
CREATE INDEX idx_prt_user_active ON password_reset_tokens (user_id, is_active);

-- V14 complete
