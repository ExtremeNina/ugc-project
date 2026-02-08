-- KEYS[1]: userLikeKey (love:user:{userId}:{loveTypeId})
-- KEYS[2]: loveTypeLikeKey (love:type:{loveTypeId}:{entityId})
-- KEYS[3]: countKey (love:count:{loveTypeId}:{entityId})
-- ARGV[1]: entityId
-- ARGV[2]: userId
-- ARGV[3]: TTL (过期时间，单位秒)

local userKey = KEYS[1]
local typeKey = KEYS[2]
local countKey = KEYS[3]
local entityId = ARGV[1]
local userId = ARGV[2]
local ttl = ARGV[3]

-- 执行取消点赞操作
redis.call('SREM', userKey, entityId)
redis.call('SREM', typeKey, userId)
local count = redis.call('DECR', countKey)

-- 确保计数不小于0
if count < 0 then
    count = 0
    redis.call('SET', countKey, count)
end

-- 设置过期时间
redis.call('EXPIRE', userKey, ttl)
redis.call('EXPIRE', typeKey, ttl)
redis.call('EXPIRE', countKey, ttl)

-- 返回新的点赞数
return count