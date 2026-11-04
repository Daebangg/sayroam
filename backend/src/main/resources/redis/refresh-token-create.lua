-- KEYS[1]: 로그인별 hash. ARGV: userId, tokenDigest, ttlMillis, maxRotations
if redis.call('EXISTS', KEYS[1]) ~= 0 then return 0 end
local now = redis.call('TIME')
local expiresAt = tonumber(now[1]) * 1000 + math.floor(tonumber(now[2]) / 1000) + tonumber(ARGV[3])
redis.call('HSET', KEYS[1], 'userId', ARGV[1], 'currentDigest', ARGV[2],
    'status', 'active', 'rotations', '0', 'maxRotations', ARGV[4],
    'expiresAtEpochMillis', string.format('%.0f', expiresAt))
redis.call('PEXPIRE', KEYS[1], ARGV[3])
return redis.call('PTTL', KEYS[1])
