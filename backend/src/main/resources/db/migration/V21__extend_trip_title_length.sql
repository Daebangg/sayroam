-- 영어 기본 이름("Trip to {목적지}")까지 담을 수 있게 여행 이름을 30자로 늘린다
ALTER TABLE trip
    MODIFY COLUMN title VARCHAR(30) NULL;
