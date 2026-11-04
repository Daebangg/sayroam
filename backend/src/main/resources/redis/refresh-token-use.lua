-- KEYS[1]: 로그인별 hash. ARGV: mode(inspect/rotate/revoke), currentDigest, nextDigest
-- 반환: status(0 active, 1 invalid, 2 reused, 3 corrupt), userId 문자열, ttlMillis
local mode = ARGV[1]
if mode ~= 'inspect' and mode ~= 'rotate' and mode ~= 'revoke' then return {3, '', 0} end
local ttl = redis.call('PTTL', KEYS[1])
-- 쿠키에 최소 1초의 남은 유효기간을 전달할 수 없으면 갱신하지 않는다.
if ttl < 1000 then return {1, '', 0} end
if redis.call('HGET', KEYS[1], 'status') ~= 'active' then return {1, '', 0} end
local userId = redis.call('HGET', KEYS[1], 'userId')
local current = redis.call('HGET', KEYS[1], 'currentDigest')
if not userId or not current then return {3, '', 0} end
if current ~= ARGV[2] then
    if redis.call('HEXISTS', KEYS[1], 'used:' .. ARGV[2]) == 1 then
        redis.call('HSET', KEYS[1], 'status', 'revoked')
        redis.call('HDEL', KEYS[1], 'currentDigest')
        return {2, '', 0}
    end
    -- 식별자만 알거나 임의 토큰을 제출한 요청은 정상 로그인을 폐기하지 못한다.
    return {1, '', 0}
end
if mode == 'revoke' then
    redis.call('HSET', KEYS[1], 'status', 'revoked')
    redis.call('HDEL', KEYS[1], 'currentDigest')
    return {0, userId, ttl}
end
if mode == 'inspect' then return {0, userId, ttl} end
local rotations = tonumber(redis.call('HGET', KEYS[1], 'rotations'))
local limit = tonumber(redis.call('HGET', KEYS[1], 'maxRotations'))
if not rotations or not limit or ARGV[3] == '' or ARGV[3] == current then return {3, '', 0} end
if rotations >= limit then
    redis.call('HSET', KEYS[1], 'status', 'revoked')
    redis.call('HDEL', KEYS[1], 'currentDigest')
    return {1, '', 0}
end
-- 사용 이력은 절대 만료까지 유지한다. HSET은 기존 TTL을 연장하지 않는다.
redis.call('HSET', KEYS[1], 'used:' .. current, '1', 'currentDigest', ARGV[3],
    'rotations', tostring(rotations + 1))
return {0, userId, ttl}
