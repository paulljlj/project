local stockKey = KEYS[1]
local buyersKey = KEYS[2]
local userId = ARGV[1]

local stock = redis.call('GET', stockKey)
if not stock then
    return 3
end
if tonumber(stock) <= 0 then
    return 1
end
if redis.call('SISMEMBER', buyersKey, userId) == 1 then
    return 2
end
redis.call('DECR', stockKey)
redis.call('SADD', buyersKey, userId)
local ttl = redis.call('PTTL', stockKey)
if ttl > 0 then
    redis.call('PEXPIRE', buyersKey, ttl)
end
return 0
