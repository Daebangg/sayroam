-- 기존 ENUM 값의 순서를 유지하고 가입 축하 알림을 추가한다. 기존 회원 알림은 소급 생성하지 않는다.
ALTER TABLE notification
    MODIFY COLUMN type ENUM('COMPANION_REQUEST', 'COMPANION_ACCEPTED', 'TRIP_INVITE', 'EXPENSE_ADDED', 'WELCOME') NOT NULL;

CREATE INDEX idx_notification_receiver_deleted_read_id
    ON notification (receiver_id, deleted_at, read_at, id);
