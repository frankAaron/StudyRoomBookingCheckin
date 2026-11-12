package com.tyut.studyroombookingcheckin.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 基于 Redis SET NX 的简单分布式锁，用于保证多实例部署时定时任务只执行一次。
 * Redis 不可用时返回 false（跳过本轮任务），避免重复释放。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisLock {

    private final StringRedisTemplate redisTemplate;

    public boolean tryLock(String key, Duration ttl) {
        try {
            return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, "1", ttl));
        } catch (Exception e) {
            log.warn("获取分布式锁失败，跳过本轮任务: {}", e.getMessage());
            return false;
        }
    }

    public void unlock(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception ignored) {
            // TTL 自动释放
        }
    }
}
