-- 여행 생성을 위해 trip을 정리한다.
-- 기간은 필수로 바꾸고, 회원이 붙이는 제목을 추가하고, 여러 지역을 표현할 수 없는 목적지를 제거한다.
-- 기간 없이 만들어진 기존 행은 만든 날 하루짜리 여행으로 채운다.
UPDATE trip
SET start_date = DATE(created_at),
    end_date = DATE(created_at)
WHERE start_date IS NULL
   OR end_date IS NULL;

ALTER TABLE trip
    ADD COLUMN title VARCHAR(20) NULL AFTER room_id,
    MODIFY COLUMN start_date DATE NOT NULL,
    MODIFY COLUMN end_date DATE NOT NULL,
    DROP COLUMN region;
