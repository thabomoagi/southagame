CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(100) UNIQUE NOT NULL
);

CREATE TABLE questions (
    id BIGSERIAL PRIMARY KEY,
    external_id VARCHAR(50) UNIQUE,
    category_id BIGINT REFERENCES categories(id),
    prompt TEXT NOT NULL,
    difficulty VARCHAR(20) NOT NULL,
    era VARCHAR(20),
    explanation TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE options (
    id BIGSERIAL PRIMARY KEY,
    question_id BIGINT NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    option_text TEXT NOT NULL,
    is_correct BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE attempts (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    difficulty VARCHAR(20) NOT NULL,
    started_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    score INTEGER,
    total_questions INTEGER,
    correct_count INTEGER
);

CREATE TABLE attempt_questions (
    id BIGSERIAL PRIMARY KEY,
    attempt_id UUID NOT NULL REFERENCES attempts(id) ON DELETE CASCADE,
    question_id BIGINT NOT NULL REFERENCES questions(id),
    position INTEGER NOT NULL,
    CONSTRAINT uq_attempt_questions_attempt_question UNIQUE (attempt_id, question_id)
);

CREATE TABLE attempt_answers (
    id BIGSERIAL PRIMARY KEY,
    attempt_id UUID NOT NULL REFERENCES attempts(id) ON DELETE CASCADE,
    question_id BIGINT NOT NULL REFERENCES questions(id),
    selected_option_id BIGINT REFERENCES options(id),
    is_correct BOOLEAN NOT NULL,
    time_taken_ms INTEGER,
    points_earned INTEGER
);

CREATE INDEX idx_attempts_user_id ON attempts(user_id);
CREATE INDEX idx_attempt_questions_attempt_id ON attempt_questions(attempt_id);
CREATE INDEX idx_attempt_answers_attempt_id ON attempt_answers(attempt_id);