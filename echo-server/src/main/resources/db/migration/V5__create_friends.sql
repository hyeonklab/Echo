CREATE TABLE friends (
    owner_user_id   BIGINT NOT NULL,
    friend_user_id  BIGINT NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (owner_user_id, friend_user_id),
    CONSTRAINT fk_friends_owner_user FOREIGN KEY (owner_user_id) REFERENCES users (id),
    CONSTRAINT fk_friends_friend_user FOREIGN KEY (friend_user_id) REFERENCES users (id),
    CONSTRAINT chk_friends_not_self CHECK (owner_user_id <> friend_user_id)
);

CREATE INDEX idx_friends_owner_user_id ON friends (owner_user_id);
