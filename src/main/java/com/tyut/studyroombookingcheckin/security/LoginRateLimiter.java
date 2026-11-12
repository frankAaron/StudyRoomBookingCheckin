package com.tyut.studyroombookingcheckin.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 登录限流：基于 Redis 计数器，按「账号」与「IP」双维度限制失败频率。
 * 账号维度 5 次 / 10 分钟；IP 维度 20 次 / 10 分钟。
 * Redis 不可用时自动降级放行，不影响正常登录。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginRateLimiter {

    private static final int USER_MAX_FAIL = 5;
    private static final int IP_MAX_FAIL = 20;
    private static final Duration WINDOW = Duration.ofMinutes(10);

    private final StringRedisTemplate redisTemplate;

    public void checkBlocked(String username, String ip) {
        try {
            String userCount = redisTemplate.opsForValue().get(userKey(username));
            if (userCount != null && Integer.parseInt(userCount) >= USER_MAX_FAIL) {
                throw new IllegalStateException("登录失败次数过多，请 10 分钟后再试");
            }
            String ipCount = redisTemplate.opsForValue().get(ipKey(ip));
            if (ipCount != null && Integer.parseInt(ipCount) >= IP_MAX_FAIL) {
                throw new IllegalStateException("当前网络登录尝试过于频繁，请 10 分钟后再试");
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.warn("登录限流不可用，已降级: {}", e.getMessage());
        }
    }

    public void onFailure(String username, String ip) {
        try {
            increment(userKey(username));
            increment(ipKey(ip));
        } catch (Exception e) {
            log.warn("记录登录失败次数失败: {}", e.getMessage());
        }
    }

    public void onSuccess(String username, String ip) {
        try {
            redisTemplate.delete(userKey(username));
            redisTemplate.delete(ipKey(ip));
        } catch (Exception e) {
            log.warn("清理登录失败计数失败: {}", e.getMessage());
        }
    }

    private void increment(String key) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, WINDOW);
        }
    }

    private String userKey(String username) {
        return "login:fail:user:" + username;
    }

    private String ipKey(String ip) {
        return "login:fail:ip:" + ip;
    }
}
