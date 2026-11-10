-- 여행은 기간을 정해야 만들 수 있다. 일차 배정이 항상 기간 안에서 이루어지도록 날짜 미정 여행을 없앤다.
-- 기간 없이 만들어진 기존 행은 만든 날 하루짜리 여행으로 채운다.
UPDATE trip
SET start_date = DATE(created_at),
    end_date = DATE(created_at)
WHERE start_date IS NULL
   OR end_date IS NULL;

ALTER TABLE trip
    MODIFY COLUMN start_date DATE NOT NULL,
    MODIFY COLUMN end_date DATE NOT NULL;
