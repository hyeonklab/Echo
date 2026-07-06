CREATE TABLE room_hidden (
    user_id     BIGINT NOT NULL,
    room_id     BIGINT NOT NULL,
    hidden_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, room_id),
    CONSTRAINT fk_room_hidden_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_room_hidden_room FOREIGN KEY (room_id) REFERENCES rooms (id) ON DELETE CASCADE
);

CREATE INDEX idx_room_hidden_room_id ON room_hidden (room_id);
