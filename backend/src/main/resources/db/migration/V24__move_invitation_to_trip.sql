-- 친구 초대 링크는 여행마다 하나만 두고 이력을 남기지 않아 여행의 값으로 저장한다.
-- 링크가 없으면 두 칸 모두 NULL이고, 만료되면 새 코드와 만료 시각으로 바꾼다.
ALTER TABLE trip
    ADD COLUMN invitation_code VARCHAR(50) NULL,
    ADD COLUMN invitation_expires_at DATETIME(6) NULL,
    ADD CONSTRAINT uk_trip_invitation_code UNIQUE (invitation_code);
