-- 단일 키에서 NX 저장과 TTL을 원자 처리한다. 기존 상태를 연장하지 않는다.
if redis.call('EXISTS', KEYS[1]) == 1 then return 0 end
redis.call('HSET', KEYS[1], 'provider', ARGV[1], 'browser', ARGV[2], 'payload', ARGV[3])
redis.call('PEXPIRE', KEYS[1], ARGV[4])
return 1
