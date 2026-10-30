if redis.call('HGET', KEYS[1], 'requestId') ~= ARGV[1]
    or redis.call('HGET', KEYS[1], 'status') ~= 'pending' then
    return {-1, -1}
end
local ttl = redis.call('PTTL', KEYS[1])
if ttl <= 0 then return {-1, -1} end
redis.call('HSET', KEYS[1], 'status', 'active')
return {ttl, math.max(0, redis.call('PTTL', KEYS[2]))}
