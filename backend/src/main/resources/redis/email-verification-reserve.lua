-- 같은 이메일의 코드 교체와 발송 제한을 하나의 원자적 작업으로 처리한다.
local cooldown = redis.call('PTTL', KEYS[2])
local emailCount = tonumber(redis.call('GET', KEYS[3]) or '0')
local ipCount = tonumber(redis.call('GET', KEYS[4]) or '0')
local retry = 0
if cooldown > 0 then retry = cooldown end
if emailCount >= tonumber(ARGV[6]) then
    retry = math.max(retry, redis.call('PTTL', KEYS[3]))
end
if ipCount >= tonumber(ARGV[7]) then
    retry = math.max(retry, redis.call('PTTL', KEYS[4]))
end
if retry > 0 then return {1, retry} end
if redis.call('INCR', KEYS[3]) == 1 then redis.call('PEXPIRE', KEYS[3], ARGV[5]) end
if redis.call('INCR', KEYS[4]) == 1 then redis.call('PEXPIRE', KEYS[4], ARGV[5]) end
redis.call('SET', KEYS[2], ARGV[1], 'PX', ARGV[4])
redis.call('DEL', KEYS[1])
redis.call('HSET', KEYS[1], 'requestId', ARGV[1], 'codeDigest', ARGV[2], 'status', 'pending')
redis.call('PEXPIRE', KEYS[1], ARGV[3])
return {0, 0}
