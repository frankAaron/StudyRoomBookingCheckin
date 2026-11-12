package com.tyut.studyroombookingcheckin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class StudyCheckinServiceImpl implements StudyCheckinService {

    private final StudyCheckinMapper studyCheckinMapper;
    private final SeatMapper seatMapper;
    private final RoomMapper roomMapper;
    private final BuildingMapper buildingMapper;

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
        c.setDurationMinutes((int) java.time.Duration.between(c.getStartTime(), now).toMinutes());
        c.setStatus(Constants.CHECKIN_STATUS_ENDED);
        c.setUpdateTime(now);
        studyCheckinMapper.updateById(c);
        return toVO(c);
    }

    @Override
    public StudyStatisticsVO getStatistics(Long userId) {
        StudyStatisticsVO vo = new StudyStatisticsVO();
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);
        LocalDate monthStart = today.with(TemporalAdjusters.firstDayOfMonth());

        vo.setTodayMinutes(studyCheckinMapper.sumDailyMinutes(userId, today));
        vo.setWeekMinutes(studyCheckinMapper.sumRangeMinutes(userId, weekStart, weekEnd));
        vo.setMonthMinutes(studyCheckinMapper.sumRangeMinutes(userId, monthStart, today));
        Integer total = studyCheckinMapper.sumRangeMinutes(userId, LocalDate.of(2020, 1, 1), today);
        vo.setTotalMinutes(total != null ? total : 0);
        vo.setTotalDays(studyCheckinMapper.countStudyDays(userId));

        vo.setDailyMinutes(buildDailyMinutes(userId, today));
        vo.setHeatmap(buildHeatmap(userId, today));

        Integer current = calculateCurrentStreak(userId, today);
        vo.setCurrentStreak(current);
        return vo;
    }

    @Override
    public Page<StudyCheckinVO> pageByUser(Long userId, long current, long size) {
        LambdaQueryWrapper<StudyCheckin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StudyCheckin::getUserId, userId);
        wrapper.orderByDesc(StudyCheckin::getStartTime);
        Page<StudyCheckin> page = studyCheckinMapper.selectPage(new Page<>(current, size), wrapper);
        Page<StudyCheckinVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<StudyCheckinVO> records = new ArrayList<>();
        for (StudyCheckin c : page.getRecords()) {
            records.add(toVO(c));
        }
        voPage.setRecords(records);
        return voPage;
    }

    private List<StudyStatisticsVO.DailyMinutesVO> buildDailyMinutes(Long userId, LocalDate today) {
        List<StudyStatisticsVO.DailyMinutesVO> result = new ArrayList<>();
        LocalDate start = today.minusDays(29);
        for (int i = 0; i < 30; i++) {
            LocalDate date = start.plusDays(i);
            StudyStatisticsVO.DailyMinutesVO d = new StudyStatisticsVO.DailyMinutesVO();
            d.setDate(date);
            d.setMinutes(studyCheckinMapper.sumDailyMinutes(userId, date));
            result.add(d);
        }
        return result;
    }

    private List<Integer> buildHeatmap(Long userId, LocalDate today) {
        List<Integer> heatmap = new ArrayList<>();
        LocalDate end = today;
        LocalDate start = end.minusWeeks(52).with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
        for (int i = 0; i < 7; i++) {
            heatmap.add(0);
        }
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            int minutes = studyCheckinMapper.sumDailyMinutes(userId, d) != null
                    ? studyCheckinMapper.sumDailyMinutes(userId, d) : 0;
            heatmap.set(d.getDayOfWeek().getValue() % 7, heatmap.get(d.getDayOfWeek().getValue() % 7) + minutes);
        }
        return heatmap;
    }

    private Integer calculateCurrentStreak(Long userId, LocalDate today) {
        int streak = 0;
        LocalDate date = today;
        while (true) {
            Integer minutes = studyCheckinMapper.sumDailyMinutes(userId, date);
            if (minutes == null || minutes < 10) {
                break;
            }
            streak++;
            date = date.minusDays(1);
        }
        return streak;
    }

    private StudyCheckinVO toVO(StudyCheckin c) {
        StudyCheckinVO vo = new StudyCheckinVO();
        vo.setId(c.getId());
        vo.setUserId(c.getUserId());
        vo.setSeatId(c.getSeatId());
        Seat s = seatMapper.selectById(c.getSeatId());
        if (s != null) vo.setSeatNo(s.getSeatNo());
        if (s != null) {
            Room r = roomMapper.selectById(s.getRoomId());
            if (r != null) {
                vo.setRoomName(r.getName());
                Building b = buildingMapper.selectById(r.getBuildingId());
                if (b != null) vo.setBuildingName(b.getName());
            }
        }
        vo.setStartTime(c.getStartTime());
        vo.setEndTime(c.getEndTime());
        vo.setDurationMinutes(c.getDurationMinutes());
        vo.setStatus(c.getStatus());
        return vo;
    }
}