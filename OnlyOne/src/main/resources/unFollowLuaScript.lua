-- KEYS[1]: user:following:{followerId} - 关注者关注列表
-- KEYS[2]: user:followers:{followingId} - 被关注者粉丝列表
-- KEYS[3]: user:fansCount:{followingId} - 被关注者粉丝数
-- KEYS[4]: user:followingCount:{followerId} - 关注者关注数
-- ARGV[1]: followingId - 被关注者ID
-- ARGV[2]: followerId - 关注者ID
-- ARGV[3]: ttl - 过期时间（秒）

local userFollowingKey = KEYS[1]
local userFollowersKey = KEYS[2]
local fansCountKey = KEYS[3]
local followingCountKey = KEYS[4]
local followingId = ARGV[1]
local followerId = ARGV[2]
local ttl = tonumber(ARGV[3])

-- 设置默认TTL为7天（604800秒）
if ttl == nil or ttl <= 0 then
    ttl = 604800
end

-- 检查是否已关注
local isFollowing = redis.call('SISMEMBER', userFollowingKey, followingId)

if isFollowing == 0 then
    -- 未关注，返回-1表示无需操作
    return -1
end

-- 执行取消关注操作
-- 1. 从关注者的关注集合中移除followingId
redis.call('SREM', userFollowingKey, followingId)
-- 2. 从被关注者的粉丝集合中移除followerId
redis.call('SREM', userFollowersKey, followerId)

-- 3. 减少被关注者的粉丝数（确保不小于0）
local fansCount = redis.call('DECR', fansCountKey)
if fansCount < 0 then
    fansCount = 0
    redis.call('SET', fansCountKey, fansCount)
end

-- 4. 减少关注者的关注数（确保不小于0）
local followingCount = redis.call('DECR', followingCountKey)
if followingCount < 0 then
    followingCount = 0
    redis.call('SET', followingCountKey, followingCount)
end

-- 设置过期时间
redis.call('EXPIRE', userFollowingKey, ttl)
redis.call('EXPIRE', userFollowersKey, ttl)
redis.call('EXPIRE', fansCountKey, ttl)
redis.call('EXPIRE', followingCountKey, ttl)

-- 返回新的粉丝数
return fansCount

