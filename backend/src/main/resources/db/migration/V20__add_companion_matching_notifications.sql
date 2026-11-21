-- 기존 ENUM 값과 순서를 유지하며 동행 후보 발견과 본인 모집 이동 대상을 추가한다.
ALTER TABLE notification
    MODIFY COLUMN type ENUM('COMPANION_REQUEST', 'COMPANION_ACCEPTED', 'TRIP_INVITE', 'EXPENSE_ADDED', 'WELCOME', 'COMPANION_MATCH_FOUND') NOT NULL,
    MODIFY COLUMN target_type ENUM('COMPANION_REQUEST', 'TRIP', 'ROOM_INVITATION', 'COMPANION_SEARCH');

CREATE INDEX idx_companion_matching_job_search_id ON companion_matching_job (search_id, id);
