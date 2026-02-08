-- KEYS[1]: userLikeKey (love:user:{userId}:{loveTypeId})
-- KEYS[2]: loveTypeLikeKey (love:type:{loveTypeId}:{entityId})
-- KEYS[3]: countKey (love:count:{loveTypeId}:{entityId})
-- ARGV[1]: entityId
-- ARGV[2]: userId
-- ARGV[3]: TTL (过期时间，单位秒)
-- Lua脚本：点赞操作 - 修正版

local userKey = KEYS[1]
local typeKey = KEYS[2]  -- 变量名统一为 typeKey
local countKey = KEYS[3]
local entityId = ARGV[1]
local userId = ARGV[2]
local ttl = ARGV[3]  -- 统一使用小写 ttl

-- 执行点赞操作
redis.call('SADD', userKey, entityId)
redis.call('SADD', typeKey, userId)
local count = redis.call('INCR', countKey)

-- 设置过期时间
redis.call('EXPIRE', userKey, ttl)
redis.call('EXPIRE', typeKey, ttl)
redis.call('EXPIRE', countKey, ttl)

-- 返回新的点赞数
return count
