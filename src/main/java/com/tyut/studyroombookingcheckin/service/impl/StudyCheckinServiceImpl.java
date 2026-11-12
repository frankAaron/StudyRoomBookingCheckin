package com.tyut.studyroombookingcheckin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tyut.studyroombookingcheckin.cache.RankingCacheService;
import com.tyut.studyroombookingcheckin.common.BusinessException;
import com.tyut.studyroombookingcheckin.common.Constants;
import com.tyut.studyroombookingcheckin.common.ErrorCode;
import com.tyut.studyroombookingcheckin.entity.Building;
import com.tyut.studyroombookingcheckin.entity.Room;
import com.tyut.studyroombookingcheckin.entity.Seat;
import com.tyut.studyroombookingcheckin.entity.StudyCheckin;
import com.tyut.studyroombookingcheckin.mapper.BuildingMapper;
import com.tyut.studyroombookingcheckin.mapper.RoomMapper;
import com.tyut.studyroombookingcheckin.mapper.SeatMapper;
import com.tyut.studyroombookingcheckin.mapper.StudyCheckinMapper;
import com.tyut.studyroombookingcheckin.service.StudyCheckinService;
import com.tyut.studyroombookingcheckin.vo.StudyCheckinVO;
import com.tyut.studyroombookingcheckin.vo.StudyStatisticsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class StudyCheckinServiceImpl implements StudyCheckinService {

    /** 计入有效学习（连续打卡）的最小分钟数。 */
    private static final int MIN_VALID_MINUTES = 10;

    private final StudyCheckinMapper studyCheckinMapper;
    private final SeatMapper seatMapper;
    private final RoomMapper roomMapper;
    private final BuildingMapper buildingMapper;
    private final RankingCacheService rankingCacheService;

    @Override
    public StudyCheckinVO startCheckin(Long userId, Long seatId, Long bookingId) {
        long existing = studyCheckinMapper.selectCount(new LambdaQueryWrapper<StudyCheckin>()
                .eq(StudyCheckin::getUserId, userId)
                .eq(StudyCheckin::getStatus, Constants.CHECKIN_STATUS_STARTED));
        if (existing > 0) {
            throw new BusinessException("已有进行中的学习打卡");
        }
        StudyCheckin c = new StudyCheckin();
        c.setUserId(userId);
        c.setSeatId(seatId);
        c.setBookingId(bookingId);
        c.setStudyDate(LocalDate.now());
        c.setStartTime(LocalDateTime.now());
        c.setStatus(Constants.CHECKIN_STATUS_STARTED);
        c.setCreateTime(LocalDateTime.now());
        c.setUpdateTime(LocalDateTime.now());
        studyCheckinMapper.insert(c);
        return toVO(c);
    }

    @Override
    public StudyCheckinVO endCheckin(Long checkinId, Long userId) {
        StudyCheckin c = studyCheckinMapper.selectById(checkinId);
        if (c == null || !c.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.CHECKIN_NOT_EXIST);
        }
        if (c.getStatus() != Constants.CHECKIN_STATUS_STARTED) {
            throw new BusinessException("打卡已结束");
        }
        LocalDateTime now = LocalDateTime.now();
        c.setEndTime(now);
        c.setDurationMinutes((int) Duration.between(c.getStartTime(), now).toMinutes());
        c.setStatus(Constants.CHECKIN_STATUS_ENDED);
        c.setUpdateTime(now);
        studyCheckinMapper.updateById(c);
        rankingCacheService.evictAll();
        return toVO(c);
    }

    @Override
    public void endByBooking(Long bookingId) {
        StudyCheckin active = studyCheckinMapper.selectOne(new LambdaQueryWrapper<StudyCheckin>()
                .eq(StudyCheckin::getBookingId, bookingId)
                .eq(StudyCheckin::getStatus, Constants.CHECKIN_STATUS_STARTED)
                .orderByDesc(StudyCheckin::getId)
                .last("LIMIT 1"));
        if (active == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        active.setEndTime(now);
        active.setDurationMinutes((int) Duration.between(active.getStartTime(), now).toMinutes());
        active.setStatus(Constants.CHECKIN_STATUS_ENDED);
        active.setUpdateTime(now);
        studyCheckinMapper.updateById(active);
        rankingCacheService.evictAll();
    }

    @Override
    public StudyStatisticsVO getStatistics(Long userId) {
        StudyStatisticsVO vo = new StudyStatisticsVO();
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);
        LocalDate monthStart = today.with(TemporalAdjusters.firstDayOfMonth());

        // 一次按天聚合查询，替代逐天/逐周循环查询
        List<StudyStatisticsVO.DailyMinutesVO> allDaily =
                studyCheckinMapper.selectDailyMinutes(userId, LocalDate.of(2020, 1, 1), today);
        Map<LocalDate, Integer> minutesMap = new HashMap<>();
        int total = 0;
        for (StudyStatisticsVO.DailyMinutesVO d : allDaily) {
            int m = d.getMinutes() == null ? 0 : d.getMinutes();
            minutesMap.put(d.getDate(), m);
            total += m;
        }

        vo.setTodayMinutes(minutesMap.getOrDefault(today, 0));
        vo.setWeekMinutes(sumWithin(minutesMap, weekStart, weekEnd));
        vo.setMonthMinutes(sumWithin(minutesMap, monthStart, today));
        vo.setTotalMinutes(total);
        vo.setTotalDays(minutesMap.size());

        vo.setDailyMinutes(buildDailyMinutes(minutesMap, today));
        vo.setHeatmapDays(buildHeatmapDays(minutesMap, today));
        vo.setHeatmap(buildHeatmap(minutesMap, today));

        int current = calculateCurrentStreak(minutesMap, today);
        vo.setCurrentStreak(current);
        vo.setMaxStreak(calculateMaxStreak(minutesMap));
        return vo;
    }

    @Override
    public Page<StudyCheckinVO> pageByUser(Long userId, long current, long size) {
        LambdaQueryWrapper<StudyCheckin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StudyCheckin::getUserId, userId);
        wrapper.orderByDesc(StudyCheckin::getStartTime);
        Page<StudyCheckin> page = studyCheckinMapper.selectPage(new Page<>(current, size), wrapper);
        Page<StudyCheckinVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(toVOList(page.getRecords()));
        return voPage;
    }

    private int sumWithin(Map<LocalDate, Integer> map, LocalDate start, LocalDate end) {
        int sum = 0;
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            sum += map.getOrDefault(d, 0);
        }
        return sum;
    }

    private List<StudyStatisticsVO.DailyMinutesVO> buildDailyMinutes(Map<LocalDate, Integer> map, LocalDate today) {
        List<StudyStatisticsVO.DailyMinutesVO> result = new ArrayList<>(30);
        LocalDate start = today.minusDays(29);
        for (int i = 0; i < 30; i++) {
            LocalDate date = start.plusDays(i);
            StudyStatisticsVO.DailyMinutesVO d = new StudyStatisticsVO.DailyMinutesVO();
            d.setDate(date);
            d.setMinutes(map.getOrDefault(date, 0));
            result.add(d);
        }
        return result;
    }

    /**
     * 最近 52 周按天聚合，供热力图绝对刻度使用。
     */
    private List<StudyStatisticsVO.DailyMinutesVO> buildHeatmapDays(Map<LocalDate, Integer> map, LocalDate today) {
        List<StudyStatisticsVO.DailyMinutesVO> result = new ArrayList<>();
        LocalDate start = today.minusWeeks(52)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
        for (LocalDate d = start; !d.isAfter(today); d = d.plusDays(1)) {
            StudyStatisticsVO.DailyMinutesVO item = new StudyStatisticsVO.DailyMinutesVO();
            item.setDate(d);
            item.setMinutes(map.getOrDefault(d, 0));
            result.add(item);
        }
        return result;
    }

    /**
     * 按星期几聚合最近 52 周的学习时长：索引 0=周日 ... 6=周六。
     */
    private List<Integer> buildHeatmap(Map<LocalDate, Integer> map, LocalDate today) {
        List<Integer> heatmap = new ArrayList<>(7);
        for (int i = 0; i < 7; i++) {
            heatmap.add(0);
        }
        LocalDate start = today.minusWeeks(52).with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
        for (LocalDate d = start; !d.isAfter(today); d = d.plusDays(1)) {
            int idx = d.getDayOfWeek().getValue() % 7;
            heatmap.set(idx, heatmap.get(idx) + map.getOrDefault(d, 0));
        }
        return heatmap;
    }

    private int calculateCurrentStreak(Map<LocalDate, Integer> map, LocalDate today) {
        int streak = 0;
        LocalDate date = today;
        while (true) {
            Integer minutes = map.get(date);
            if (minutes == null || minutes < MIN_VALID_MINUTES) {
                break;
            }
            streak++;
            date = date.minusDays(1);
        }
        return streak;
    }

    private int calculateMaxStreak(Map<LocalDate, Integer> map) {
        int max = 0;
        int current = 0;
        LocalDate prev = null;
        for (Map.Entry<LocalDate, Integer> e : map.entrySet().stream()
                .filter(en -> en.getValue() != null && en.getValue() >= MIN_VALID_MINUTES)
                .sorted(Map.Entry.comparingByKey())
                .toList()) {
            if (prev != null && prev.plusDays(1).equals(e.getKey())) {
                current++;
            } else {
                current = 1;
            }
            max = Math.max(max, current);
            prev = e.getKey();
        }
        return max;
    }

    /**
     * 批量转换打卡记录，一次性加载座位/房间/楼栋，消除 N+1。
     */
    private List<StudyCheckinVO> toVOList(List<StudyCheckin> checkins) {
        List<StudyCheckinVO> result = new ArrayList<>();
        if (checkins == null || checkins.isEmpty()) {
            return result;
        }
        Set<Long> seatIds = new HashSet<>();
        for (StudyCheckin c : checkins) {
            if (c.getSeatId() != null) {
                seatIds.add(c.getSeatId());
            }
        }
        Map<Long, Seat> seatMap = new HashMap<>();
        if (!seatIds.isEmpty()) {
            for (Seat s : seatMapper.selectBatchIds(seatIds)) {
                seatMap.put(s.getId(), s);
            }
        }
        Set<Long> roomIds = new HashSet<>();
        for (Seat s : seatMap.values()) {
            if (s.getRoomId() != null) {
                roomIds.add(s.getRoomId());
            }
        }
        Map<Long, Room> roomMap = new HashMap<>();
        if (!roomIds.isEmpty()) {
            for (Room r : roomMapper.selectBatchIds(roomIds)) {
                roomMap.put(r.getId(), r);
            }
        }
        Set<Long> buildingIds = new HashSet<>();
        for (Room r : roomMap.values()) {
            if (r.getBuildingId() != null) {
                buildingIds.add(r.getBuildingId());
            }
        }
        Map<Long, Building> buildingMap = new HashMap<>();
        if (!buildingIds.isEmpty()) {
            for (Building b : buildingMapper.selectBatchIds(buildingIds)) {
                buildingMap.put(b.getId(), b);
            }
        }

        for (StudyCheckin c : checkins) {
            StudyCheckinVO vo = new StudyCheckinVO();
            vo.setId(c.getId());
            vo.setUserId(c.getUserId());
            vo.setSeatId(c.getSeatId());
            Seat s = seatMap.get(c.getSeatId());
            if (s != null) {
                vo.setSeatNo(s.getSeatNo());
                Room r = roomMap.get(s.getRoomId());
                if (r != null) {
                    vo.setRoomName(r.getName());
                    Building b = buildingMap.get(r.getBuildingId());
                    if (b != null) {
                        vo.setBuildingName(b.getName());
                    }
                }
            }
            vo.setStartTime(c.getStartTime());
            vo.setEndTime(c.getEndTime());
            vo.setDurationMinutes(c.getDurationMinutes());
            vo.setStatus(c.getStatus());
            result.add(vo);
        }
        return result;
    }

    private StudyCheckinVO toVO(StudyCheckin c) {
        return toVOList(List.of(c)).get(0);
    }
}
