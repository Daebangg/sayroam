if redis.call('HGET', KEYS[1], 'requestId') == ARGV[1]
    and redis.call('HGET', KEYS[1], 'status') == 'pending' then
    return redis.call('DEL', KEYS[1])
end
return 0
