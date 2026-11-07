-- 불일치 요청으로 다른 브라우저의 정상 상태를 지우지 않는다.
if redis.call('PTTL', KEYS[1]) <= 0 then return nil end
if redis.call('HGET', KEYS[1], 'provider') ~= ARGV[1] then return nil end
if redis.call('HGET', KEYS[1], 'browser') ~= ARGV[2] then return nil end
local payload = redis.call('HGET', KEYS[1], 'payload')
redis.call('DEL', KEYS[1])
return payload
