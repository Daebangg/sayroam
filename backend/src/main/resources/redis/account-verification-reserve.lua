-- 모든 목적에 걸친 회원/IP 제한과, 해당 회원·이메일·목적의 코드/증명 교체를 원자 처리한다.
local cooldown = redis.call('PTTL', KEYS[2])
local accountCount = tonumber(redis.call('GET', KEYS[3]) or '0')
local ipCount = tonumber(redis.call('GET', KEYS[4]) or '0')
local retry = math.max(0, cooldown)
if accountCount >= tonumber(ARGV[6]) then
    retry = math.max(retry, redis.call('PTTL', KEYS[3]))
end
if ipCount >= tonumber(ARGV[7]) then
    retry = math.max(retry, redis.call('PTTL', KEYS[4]))
end
if retry > 0 then return {1, retry} end
if redis.call('INCR', KEYS[3]) == 1 then redis.call('PEXPIRE', KEYS[3], ARGV[5]) end
if redis.call('INCR', KEYS[4]) == 1 then redis.call('PEXPIRE', KEYS[4], ARGV[5]) end
redis.call('SET', KEYS[2], ARGV[1], 'PX', ARGV[4])
redis.call('DEL', KEYS[1], KEYS[5])
redis.call('HSET', KEYS[1], 'requestId', ARGV[1], 'codeDigest', ARGV[2],
    'status', 'pending', 'failedAttempts', '0',
    'userId', ARGV[8], 'emailHash', ARGV[9], 'purpose', ARGV[10], 'authVersion', ARGV[11])
redis.call('PEXPIRE', KEYS[1], ARGV[3])
return {0, 0}
