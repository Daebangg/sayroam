ALTER TABLE notification
    ADD COLUMN event_key VARCHAR(150) NULL,
    ADD COLUMN deleted_at DATETIME(6) NULL;

-- 기존 사건은 복원할 수 없으므로 알림 ID별로 구분한다. 이 접두사는 기존 데이터용으로 예약한다.
UPDATE notification
SET event_key = CONCAT('legacy:notification:', id);

-- 삭제 후에도 사건 식별값을 유지해 같은 수신자에게 동일 사건의 알림이 다시 생성되지 않게 한다.
ALTER TABLE notification
    MODIFY COLUMN event_key VARCHAR(150) NOT NULL,
    ADD CONSTRAINT uk_notification_receiver_event UNIQUE (receiver_id, event_key);
