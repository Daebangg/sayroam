-- 방장 여부로 판단하는 기능이 없고 여행을 만든 회원은 trip.created_by가 가진다.
ALTER TABLE room_member DROP COLUMN role;

-- 여행 방은 이름 없이 여행 이름을 보여 주고, 이름 있는 방을 만드는 API는 없앴다.
ALTER TABLE room DROP COLUMN name;
