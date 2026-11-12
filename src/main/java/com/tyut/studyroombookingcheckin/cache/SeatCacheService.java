package com.tyut.studyroombookingcheckin.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tyut.studyroombookingcheckin.entity.Seat;
import com.tyut.studyroombookingcheckin.mapper.SeatMapper;
import com.tyut.studyroombookingcheckin.vo.SeatVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 座位实时状态缓存。
 * <p>
 * 存储结构：Redis Hash，key={@code seat:room:{roomId}}，field=seatId，value=SeatVO JSON。
 * 写入使用 HSET 保证单个座位更新原子性；读取走 Hash，未命中时回源数据库并回填。
 * 防穿透：空房间写入独立的空值标记 key（短 TTL）。
 * 防击穿：回源前抢占 Redis 互斥锁，未抢到的线程短暂等待后回源，避免全部打到数据库。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeatCacheService {

    public static final String ROOM_KEY_PREFIX = "seat:room:";
    private static final String EMPTY_KEY_PREFIX = "seat:room:empty:";
    private static final String LOCK_KEY_PREFIX = "lock:seat:room:";
    private static final long TTL_SECONDS = 1800L;
    private static final long EMPTY_TTL_SECONDS = 60L;
    private static final long LOCK_TTL_SECONDS = 5L;

    private final StringRedisTemplate redisTemplate;
    private final SeatMapper seatMapper;
    private final ObjectMapper objectMapper;

    public List<SeatVO> getRoomSeats(Long roomId) {
        String key = ROOM_KEY_PREFIX + roomId;
        try {
            if (Boolean.TRUE.equals(redisTemplate.hasKey(EMPTY_KEY_PREFIX + roomId))) {
                return new ArrayList<>();
            }
            Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
            if (!entries.isEmpty()) {
                return parse(entries);
            }
            if (!acquireLock(roomId)) {
                sleepQuietly(80);
                entries = redisTemplate.opsForHash().entries(key);
                if (!entries.isEmpty()) {
                    return parse(entries);
                }
                // 未抢到锁且缓存仍为空：直接回源，保证可用性
                return loadFromDb(roomId);
            }
            try {
                entries = redisTemplate.opsForHash().entries(key);
                if (!entries.isEmpty()) {
                    return parse(entries);
                }
                List<SeatVO> seats = loadFromDb(roomId);
                writeAll(roomId, seats);
                return seats;
            } finally {
                releaseLock(roomId);
            }
        } catch (Exception e) {
            log.warn("座位缓存不可用，降级为数据库查询: {}", e.getMessage());
            return loadFromDb(roomId);
        }
    }

    /** 座位状态变更后原子写入缓存（不存在则忽略，等待下次回源）。 */
    public void syncSeat(Seat seat) {
        if (seat == null || seat.getRoomId() == null) {
            return;
        }
        try {
            String key = ROOM_KEY_PREFIX + seat.getRoomId();
            if (!Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
                return;
            }
            redisTemplate.opsForHash().put(key, String.valueOf(seat.getId()), serialize(toVO(seat)));
        } catch (Exception e) {
            log.warn("同步座位缓存失败，删除对应缓存等待回源: {}", e.getMessage());
            evictRoom(seat.getRoomId());
        }
    }

    public void evictRoom(Long roomId) {
        if (roomId == null) {
            return;
        }
        try {
            redisTemplate.delete(ROOM_KEY_PREFIX + roomId);
            redisTemplate.delete(EMPTY_KEY_PREFIX + roomId);
        } catch (Exception e) {
            log.warn("清理座位缓存失败: {}", e.getMessage());
        }
    }

    private void writeAll(Long roomId, List<SeatVO> seats) {
        try {
            String key = ROOM_KEY_PREFIX + roomId;
            if (seats.isEmpty()) {
                redisTemplate.opsForValue().set(EMPTY_KEY_PREFIX + roomId, "1", Duration.ofSeconds(EMPTY_TTL_SECONDS));
                return;
            }
            redisTemplate.delete(EMPTY_KEY_PREFIX + roomId);
            Map<String, String> map = new LinkedHashMap<>();
            for (SeatVO vo : seats) {
                map.put(String.valueOf(vo.getId()), serialize(vo));
            }
            redisTemplate.opsForHash().putAll(key, map);
            long ttl = TTL_SECONDS + ThreadLocalRandom.current().nextLong(300);
            redisTemplate.expire(key, Duration.ofSeconds(ttl));
        } catch (Exception e) {
            log.warn("回填座位缓存失败: {}", e.getMessage());
        }
    }

    private boolean acquireLock(Long roomId) {
        try {
            return Boolean.TRUE.equals(redisTemplate.opsForValue()
                    .setIfAbsent(LOCK_KEY_PREFIX + roomId, "1", Duration.ofSeconds(LOCK_TTL_SECONDS)));
        } catch (Exception e) {
            return false;
        }
    }

    private void releaseLock(Long roomId) {
        try {
            redisTemplate.delete(LOCK_KEY_PREFIX + roomId);
        } catch (Exception ignored) {
            // 锁 TTL 自动释放
        }
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private List<SeatVO> loadFromDb(Long roomId) {
        List<Seat> seats = seatMapper.selectList(new LambdaQueryWrapper<Seat>()
                .eq(Seat::getRoomId, roomId));
        seats.sort(Comparator
                .comparing((Seat s) -> s.getSeatRow() == null ? 0 : s.getSeatRow())
                .thenComparing(s -> s.getCol() == null ? 0 : s.getCol()));
        List<SeatVO> list = new ArrayList<>(seats.size());
        for (Seat s : seats) {
            list.add(toVO(s));
        }
        return list;
    }

    private List<SeatVO> parse(Map<Object, Object> entries) throws Exception {
        List<SeatVO> list = new ArrayList<>(entries.size());
        for (Object value : entries.values()) {
            list.add(objectMapper.readValue(String.valueOf(value), SeatVO.class));
        }
        list.sort(Comparator
                .comparing((SeatVO s) -> s.getRow() == null ? 0 : s.getRow())
                .thenComparing(s -> s.getCol() == null ? 0 : s.getCol()));
        return list;
    }

    private String serialize(SeatVO vo) throws Exception {
        return objectMapper.writeValueAsString(vo);
    }

    private SeatVO toVO(Seat s) {
        SeatVO vo = new SeatVO();
        vo.setId(s.getId());
        vo.setRoomId(s.getRoomId());
        vo.setSeatNo(s.getSeatNo());
        vo.setRow(s.getSeatRow());
        vo.setCol(s.getCol());
        vo.setStatus(s.getStatus());
        vo.setOccupiedUserId(s.getOccupiedUserId());
        vo.setQrcode(s.getQrcode());
        return vo;
    }
}
