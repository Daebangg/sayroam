-- 친구 초대 링크는 여행마다 하나만 두고 이력을 남기지 않아 여행의 값으로 저장한다.
-- 링크가 없으면 두 칸 모두 NULL이고, 만료되면 새 코드와 만료 시각으로 바꾼다.
ALTER TABLE trip
    ADD COLUMN invitation_code VARCHAR(50) NULL,
    ADD COLUMN invitation_expires_at DATETIME(6) NULL,
    ADD CONSTRAINT uk_trip_invitation_code UNIQUE (invitation_code);

-- 여행 채팅은 여행의 속성이라 모든 여행이 채팅방을 가진다. 채팅방 없는 기존 여행에 방을 만들어 만든 회원을 넣는다.
ALTER TABLE room ADD COLUMN migrated_trip_id BIGINT NULL;

INSERT INTO room (name, created_at, updated_at, migrated_trip_id)
SELECT NULL, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), t.id FROM trip t WHERE t.room_id IS NULL;

INSERT INTO room_member (room_id, user_id, role, status, notification_muted, joined_at, created_at, updated_at)
SELECT r.id, t.created_by, 'MEMBER', 'JOINED', 0, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)
FROM room r JOIN trip t ON t.id = r.migrated_trip_id;

UPDATE trip t JOIN room r ON r.migrated_trip_id = t.id SET t.room_id = r.id;

ALTER TABLE room DROP COLUMN migrated_trip_id;

ALTER TABLE trip MODIFY COLUMN room_id BIGINT NOT NULL;

-- 초대는 여행이 가진다. 방 초대는 여행 초대로 대체되어 쓰지 않는다.
DROP TABLE room_invitation;
