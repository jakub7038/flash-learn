-- FlashLearn migration V2
-- Tabela blacklisty tokenów JWT (potrzebna do FL-22 logout)

CREATE TABLE revoked_tokens (
                                id         BIGSERIAL    PRIMARY KEY,
                                token      TEXT         NOT NULL UNIQUE,
                                revoked_at TIMESTAMP    NOT NULL DEFAULT NOW(),
                                expires_at TIMESTAMP    NOT NULL
);

CREATE INDEX idx_revoked_tokens_token      ON revoked_tokens(token);
CREATE INDEX idx_revoked_tokens_expires_at ON revoked_tokens(expires_at);