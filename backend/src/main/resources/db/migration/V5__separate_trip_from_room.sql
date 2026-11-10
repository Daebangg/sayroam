-- 혼자 가는 여행은 방 없이 존재하므로 만든 회원을 기록하고 room_id는 NULL을 허용한다.
-- 동행 매칭 출처는 companion_request.room_id로 알 수 있어 source_companion_request_id를 제거한다.
ALTER TABLE trip
    DROP FOREIGN KEY fk_trip_companion_request,
    DROP COLUMN source_companion_request_id,
    ADD COLUMN created_by BIGINT NOT NULL AFTER id,
    MODIFY COLUMN room_id BIGINT NULL,
    ADD CONSTRAINT fk_trip_created_by FOREIGN KEY (created_by) REFERENCES users (id);
