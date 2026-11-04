-- KEYS는 회원·현재 이메일·목적별로 분리되며 값의 바인딩도 다시 확인한다.
if redis.call('PTTL', KEYS[1]) <= 0 then return {1, 0} end
if redis.call('HGET', KEYS[1], 'userId') ~= ARGV[6]
    or redis.call('HGET', KEYS[1], 'emailHash') ~= ARGV[7]
    or redis.call('HGET', KEYS[1], 'purpose') ~= ARGV[8]
    or redis.call('HGET', KEYS[1], 'authVersion') ~= ARGV[9]
    or redis.call('HGET', KEYS[1], 'requestId') ~= ARGV[1] then
    return {2, 0}
end
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
    'userId', ARGV[6], 'emailHash', ARGV[7], 'purpose', ARGV[8], 'authVersion', ARGV[9],
    'verifiedAtEpochMillis', verifiedAt, 'status', 'available')
redis.call('PEXPIRE', KEYS[2], ARGV[5])
redis.call('HSET', KEYS[1], 'status', 'verified')
redis.call('HDEL', KEYS[1], 'codeDigest')
return {0, redis.call('PTTL', KEYS[2])}
