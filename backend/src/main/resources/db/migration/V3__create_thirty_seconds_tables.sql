CREATE TABLE thirty_seconds_cards (
    id BIGSERIAL PRIMARY KEY,
    external_id VARCHAR(50) UNIQUE NOT NULL,
    difficulty VARCHAR(20) NOT NULL,
    words TEXT NOT NULL
);

CREATE TABLE thirty_seconds_games (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    mode VARCHAR(20) NOT NULL,
    player_count INTEGER NOT NULL,
    total_score INTEGER,
    winning_player_name VARCHAR(100),
    started_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP
);

CREATE TABLE thirty_seconds_rounds (
    id BIGSERIAL PRIMARY KEY,
    game_id UUID NOT NULL REFERENCES thirty_seconds_games(id) ON DELETE CASCADE,
    round_number INTEGER NOT NULL,
    player_name VARCHAR(100),
    prompt TEXT NOT NULL,
    score INTEGER
);

CREATE INDEX idx_thirty_seconds_games_user_id ON thirty_seconds_games(user_id);
CREATE INDEX idx_thirty_seconds_rounds_game_id ON thirty_seconds_rounds(game_id);