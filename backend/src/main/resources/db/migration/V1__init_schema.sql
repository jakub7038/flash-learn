-- FlashLearn Database Schema
-- Flyway migration V1 - initial schema

CREATE TABLE users (
                       id            BIGSERIAL PRIMARY KEY,
                       email         VARCHAR(255) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL,
                       created_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE decks (
                       id          BIGSERIAL PRIMARY KEY,
                       owner_id    BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                       title       VARCHAR(255) NOT NULL,
                       description TEXT,
                       is_public   BOOLEAN      NOT NULL DEFAULT FALSE,
                       created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
                       updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE flashcards (
                            id         BIGSERIAL PRIMARY KEY,
                            deck_id    BIGINT    NOT NULL REFERENCES decks(id) ON DELETE CASCADE,
                            question   TEXT      NOT NULL,
                            answer     TEXT      NOT NULL,
                            created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                            updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- SM-2 progress per user per flashcard (3NF: depends on composite key user+flashcard)
CREATE TABLE user_flashcard_progress (
                                         id               BIGSERIAL        PRIMARY KEY,
                                         user_id          BIGINT           NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                         flashcard_id     BIGINT           NOT NULL REFERENCES flashcards(id) ON DELETE CASCADE,
                                         ease_factor      DOUBLE PRECISION NOT NULL DEFAULT 2.50,
                                         interval_days    INTEGER          NOT NULL DEFAULT 1,
                                         repetitions      INTEGER          NOT NULL DEFAULT 0,
                                         next_review_date DATE             NOT NULL DEFAULT CURRENT_DATE,
                                         UNIQUE (user_id, flashcard_id)
);

CREATE TABLE study_sessions (
                                id             BIGSERIAL PRIMARY KEY,
                                user_id        BIGINT    NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                deck_id        BIGINT    NOT NULL REFERENCES decks(id) ON DELETE CASCADE,
                                started_at     TIMESTAMP NOT NULL DEFAULT NOW(),
                                finished_at    TIMESTAMP,
                                cards_reviewed INTEGER   NOT NULL DEFAULT 0
);

-- Result per flashcard per session (3NF: depends on session+flashcard)
CREATE TABLE study_session_results (
                                       id           BIGSERIAL PRIMARY KEY,
                                       session_id   BIGINT    NOT NULL REFERENCES study_sessions(id) ON DELETE CASCADE,
                                       flashcard_id BIGINT    NOT NULL REFERENCES flashcards(id) ON DELETE CASCADE,
                                       rating       INTEGER   NOT NULL CHECK (rating IN (0, 1, 2)),
                                       reviewed_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Marketplace: tracks cloned decks (original -> clone mapping)
CREATE TABLE deck_clones (
                             id               BIGSERIAL PRIMARY KEY,
                             original_deck_id BIGINT    NOT NULL REFERENCES decks(id) ON DELETE CASCADE,
                             cloned_deck_id   BIGINT    NOT NULL REFERENCES decks(id) ON DELETE CASCADE,
                             cloned_by        BIGINT    NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                             cloned_at        TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_decks_owner       ON decks(owner_id);
CREATE INDEX idx_decks_public      ON decks(is_public);
CREATE INDEX idx_flashcards_deck   ON flashcards(deck_id);
CREATE INDEX idx_progress_user     ON user_flashcard_progress(user_id);
CREATE INDEX idx_progress_review   ON user_flashcard_progress(next_review_date);
CREATE INDEX idx_sessions_user     ON study_sessions(user_id);
CREATE INDEX idx_clones_original   ON deck_clones(original_deck_id);