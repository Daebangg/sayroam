-- 재전송된 메시지를 구분한다. 서버가 만드는 시스템 메시지는 값이 없으므로 NULL을 허용한다.
ALTER TABLE chat_message
    ADD COLUMN client_message_id VARCHAR(36) NULL AFTER sender_id,
    ADD CONSTRAINT uk_chat_message_sender_client UNIQUE (sender_id, client_message_id);
