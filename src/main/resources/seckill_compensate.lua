local stockKey = KEYS[1]
local buyersKey = KEYS[2]
local userId = ARGV[1]

if redis.call('SREM', buyersKey, userId) == 1 then
    redis.call('INCR', stockKey)
    return 1
end
return 0
