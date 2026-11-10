-- KEYS: normalized email hash, server-observed IP hash (same cluster slot).
-- ARGV: CHECK/FAILURE/SUCCESS, emailMaxFailures, windowMillis, blockMillis, ipMaxFailures.
-- Password results linearize here: even an already-running successful check
-- cannot issue tokens after another result has activated a block.
local mode = ARGV[1]
if mode ~= 'CHECK' and mode ~= 'FAILURE' and mode ~= 'SUCCESS' then error('Invalid login attempt mode') end
local clock = redis.call('TIME')
local now = tonumber(clock[1]) * 1000 + math.floor(tonumber(clock[2]) / 1000)
local remaining = 0
local limits = {tonumber(ARGV[2]), tonumber(ARGV[5] or ARGV[2])}
local emailAttemptsLeft = limits[1]
for index, key in ipairs(KEYS) do
    local untilValue = redis.call('HGET', key, 'blockedUntil')
    if untilValue then
        local untilTime = tonumber(untilValue)
        if not untilTime then error('Invalid login attempt state') end
        if untilTime > now then remaining = math.max(remaining, untilTime - now)
        else redis.call('DEL', key) end
    end
    local failures = tonumber(redis.call('HGET', key, 'failures') or '0')
    if not failures then error('Invalid login failure count') end
    if index == 1 then emailAttemptsLeft = limits[1] - failures end
end
-- Blocked requests never increment counts or extend expiration.
if remaining > 0 then return {remaining, 0} end
if mode == 'CHECK' then return {0, math.max(0, emailAttemptsLeft)} end
if mode == 'SUCCESS' then
    -- An attacker cannot reset IP-wide failures by logging into their own account.
    redis.call('DEL', KEYS[1])
    return {0, limits[1]}
end
for index, key in ipairs(KEYS) do
    local count = redis.call('HINCRBY', key, 'failures', 1)
    if index == 1 then emailAttemptsLeft = limits[1] - count end
    if count == 1 then redis.call('PEXPIRE', key, ARGV[3]) end
    if count >= limits[index] then
        redis.call('HSET', key, 'blockedUntil', string.format('%.0f', now + tonumber(ARGV[4])))
        redis.call('PEXPIRE', key, ARGV[4])
        remaining = math.max(remaining, tonumber(ARGV[4]))
    end
end
if remaining > 0 then return {remaining, 0} end
return {0, math.max(0, emailAttemptsLeft)}
