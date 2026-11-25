-- 수신 모집은 기존 companion_search_id를 사용한다. 이전 행의 발신 모집은 추정하지 않는다.
ALTER TABLE companion_request
    ADD COLUMN sender_search_id BIGINT NULL,
    ADD COLUMN first_search_id BIGINT GENERATED ALWAYS AS (LEAST(sender_search_id, companion_search_id)) STORED,
    ADD COLUMN second_search_id BIGINT GENERATED ALWAYS AS (GREATEST(sender_search_id, companion_search_id)) STORED,
    ADD CONSTRAINT uk_companion_request_pair UNIQUE (first_search_id, second_search_id),
    ADD CONSTRAINT fk_companion_request_sender_search FOREIGN KEY (sender_search_id) REFERENCES companion_search (id),
    ADD INDEX idx_companion_request_sender (sender_id);

ALTER TABLE companion_request DROP INDEX uk_companion_request_sender_receiver_search;

ALTER TABLE notification
    MODIFY COLUMN type ENUM('COMPANION_REQUEST', 'COMPANION_ACCEPTED', 'TRIP_INVITE', 'EXPENSE_ADDED',
        'WELCOME', 'COMPANION_MATCH_FOUND', 'COMPANION_REJECTED') NOT NULL;
