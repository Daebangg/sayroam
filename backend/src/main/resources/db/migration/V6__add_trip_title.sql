-- 같은 지역의 여행을 목록에서 구분할 수 있도록 회원이 직접 붙이는 제목을 둔다.
-- 제목을 붙이지 않은 여행은 NULL이며, 화면이 기본 문구로 대신 표시한다.
ALTER TABLE trip
    ADD COLUMN title VARCHAR(20) NULL AFTER room_id;
