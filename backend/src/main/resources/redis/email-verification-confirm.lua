-- 동일 이메일의 상태 확인·시도 횟수 증가·코드 소비·가입용 증명 생성을 원자 처리한다.
local ttl = redis.call('PTTL', KEYS[1])
if ttl <= 0 then return {1, 0} end
if redis.call('HGET', KEYS[1], 'requestId') ~= ARGV[1] then return {2, 0} end
local status = redis.call('HGET', KEYS[1], 'status')
if status == 'verified' then return {5, 0} end
if status == 'locked' then return {4, 0} end
if status ~= 'active' then return {2, 0} end
local attempts = tonumber(redis.call('HGET', KEYS[1], 'failedAttempts') or '0')
if attempts >= tonumber(ARGV[4]) then return {4, 0} end
if redis.call('HGET', KEYS[1], 'codeDigest') ~= ARGV[2] then
    attempts = redis.call('HINCRBY', KEYS[1], 'failedAttempts', 1)
    if attempts >= tonumber(ARGV[4]) then
        redis.call('HSET', KEYS[1], 'status', 'locked')
        redis.call('HDEL', KEYS[1], 'codeDigest')
        return {4, 0}
    end
    return {3, 0}
end
local now = redis.call('TIME')
local verifiedAt = now[1] .. string.format('%03d', math.floor(tonumber(now[2]) / 1000))
redis.call('HSET', KEYS[2], 'proofDigest', ARGV[3], 'requestId', ARGV[1],
    'verifiedAtEpochMillis', verifiedAt, 'purpose', 'signup', 'status', 'available')
redis.call('PEXPIRE', KEYS[2], ARGV[5])
redis.call('HSET', KEYS[1], 'status', 'verified')
redis.call('HDEL', KEYS[1], 'codeDigest')
return {0, redis.call('PTTL', KEYS[2])}
