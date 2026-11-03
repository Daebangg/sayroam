-- 이메일별 증명을 먼저 소비한다. DB 트랜잭션과 분리되며 실패 시 복원하지 않는다.
if redis.call('PTTL', KEYS[1]) <= 0 then return {1, 0} end
if redis.call('HGET', KEYS[1], 'purpose') ~= 'signup'
    or redis.call('HGET', KEYS[1], 'proofDigest') ~= ARGV[1] then
    return {2, 0}
end
local status = redis.call('HGET', KEYS[1], 'status')
if status == 'consumed' then return {3, 0} end
if status ~= 'available' then return {2, 0} end
local verifiedAt = tonumber(redis.call('HGET', KEYS[1], 'verifiedAtEpochMillis'))
if not verifiedAt or verifiedAt <= 0 then return {4, 0} end
redis.call('HSET', KEYS[1], 'status', 'consumed')
-- 기존 TTL을 유지해 동일 증명의 재사용을 구별한다. 원문은 저장하지 않는다.
return {0, verifiedAt}
