CREATE TABLE room_read_states (
    room_id                 BIGINT NOT NULL REFERENCES rooms (id) ON DELETE CASCADE,
    user_id                 BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    last_read_message_id    BIGINT REFERENCES messages (id) ON DELETE SET NULL,
    updated_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (room_id, user_id)
);

CREATE INDEX idx_room_read_states_user_id ON room_read_states (user_id);
