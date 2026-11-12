package com.tyut.studyroombookingcheckin.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tyut.studyroombookingcheckin.vo.RankingVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 排行榜缓存。
 * <p>
 * 日/周/月/总榜分别独立 key，读取时命中即返回；未命中回源并写回，列表为空同样缓存，
 * 防止空结果反复打库（缓存穿透）。回源时使用 Redis 互斥锁避免击穿。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RankingCacheService {

    private static final String DAILY = "ranking:daily:";
    private static final String WEEKLY = "ranking:weekly:";
    private static final String MONTHLY = "ranking:monthly:";
    private static final String ALL = "ranking:all";
    private static final String LOCK = "lock:ranking:";
    private static final long LOCK_TTL_SECONDS = 5L;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public List<RankingVO> getDaily(String date, Supplier<List<RankingVO>> loader) {
        return getOrLoad(DAILY + date, Duration.ofMinutes(5), loader);
    }

    public List<RankingVO> getWeekly(String weekStart, Supplier<List<RankingVO>> loader) {
        return getOrLoad(WEEKLY + weekStart, Duration.ofMinutes(10), loader);
    }

    public List<RankingVO> getMonthly(String month, Supplier<List<RankingVO>> loader) {
        return getOrLoad(MONTHLY + month, Duration.ofMinutes(30), loader);
    }

    public List<RankingVO> getAllTime(Supplier<List<RankingVO>> loader) {
        return getOrLoad(ALL, Duration.ofHours(1), loader);
    }

    /** 学习时长发生变化后清理排行榜缓存（日/周/月/总榜）。 */
    public void evictAll() {
        try {
            Set<String> keys = redisTemplate.keys("ranking:*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.warn("清理排行榜缓存失败: {}", e.getMessage());
        }
    }

    private List<RankingVO> getOrLoad(String key, Duration ttl, Supplier<List<RankingVO>> loader) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json != null) {
                return objectMapper.readValue(json, new TypeReference<List<RankingVO>>() {
                });
            }
        } catch (Exception e) {
            log.warn("读取排行榜缓存失败，回源数据库: {}", e.getMessage());
        }

        String lockKey = LOCK + key;
        boolean locked = false;
        try {
            locked = Boolean.TRUE.equals(redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, "1", Duration.ofSeconds(LOCK_TTL_SECONDS)));
            if (!locked) {
                Thread.sleep(80);
                String json = redisTemplate.opsForValue().get(key);
                if (json != null) {
                    return objectMapper.readValue(json, new TypeReference<List<RankingVO>>() {
                    });
                }
                return loader.get();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return loader.get();
        } catch (Exception e) {
            log.warn("排行榜缓存锁不可用: {}", e.getMessage());
        }

        try {
            List<RankingVO> list = loader.get();
            try {
                redisTemplate.opsForValue().set(key,
                        objectMapper.writeValueAsString(list == null ? new ArrayList<>() : list), ttl);
            } catch (Exception e) {
                log.warn("写入排行榜缓存失败: {}", e.getMessage());
            }
            return list == null ? new ArrayList<>() : list;
        } finally {
            if (locked) {
                try {
                    redisTemplate.delete(lockKey);
                } catch (Exception ignored) {
                    // TTL 自动释放
                }
            }
        }
    }
}
