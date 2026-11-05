-- 증명 원문은 저장하지 않는다. 회원 ID는 Lua 숫자로 변환하지 않아 BIGINT 정밀도를 보존한다.
if redis.call('PTTL', KEYS[1]) <= 0 then return {1, 0} end
if redis.call('HGET', KEYS[1], 'userId') ~= ARGV[2]
    or redis.call('HGET', KEYS[1], 'emailHash') ~= ARGV[3]
    or redis.call('HGET', KEYS[1], 'purpose') ~= ARGV[4]
    or redis.call('HGET', KEYS[1], 'authVersion') ~= ARGV[5]
    or redis.call('HGET', KEYS[1], 'proofDigest') ~= ARGV[1] then
    return {2, 0}
end
local status = redis.call('HGET', KEYS[1], 'status')
if status == 'consumed' then return {3, 0} end
if status ~= 'available' then return {2, 0} end
local verifiedAt = tonumber(redis.call('HGET', KEYS[1], 'verifiedAtEpochMillis'))
if not verifiedAt or verifiedAt <= 0 then return {4, 0} end
redis.call('HSET', KEYS[1], 'status', 'consumed')
-- TTL과 다이제스트를 유지해 같은 증명의 재사용을 구별한다.
return {0, verifiedAt}
