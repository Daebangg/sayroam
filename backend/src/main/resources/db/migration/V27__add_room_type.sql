ALTER TABLE room
    ADD COLUMN room_type ENUM('TRIP', 'COMPANION') NULL AFTER id;

-- 여행에 연결된 방은 TRIP, 요청에만 연결된 방은 COMPANION으로 분류한다.
-- 연결이 남아 있지 않은 과거 방은 기존 여행 채팅과 같은 TRIP으로 보존한다.
UPDATE room r
    LEFT JOIN trip t ON t.room_id = r.id
    LEFT JOIN companion_request cr ON cr.room_id = r.id
SET r.room_type = CASE
    WHEN t.id IS NOT NULL THEN 'TRIP'
    WHEN cr.id IS NOT NULL THEN 'COMPANION'
    ELSE 'TRIP'
END;

ALTER TABLE room
    MODIFY COLUMN room_type ENUM('TRIP', 'COMPANION') NOT NULL;
