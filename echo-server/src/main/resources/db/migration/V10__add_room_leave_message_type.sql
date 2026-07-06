ALTER TABLE messages
    DROP CONSTRAINT chk_messages_message_type;

ALTER TABLE messages
    ADD CONSTRAINT chk_messages_message_type CHECK (message_type IN ('TEXT', 'IMAGE_ALBUM', 'ROOM_LEAVE'));
