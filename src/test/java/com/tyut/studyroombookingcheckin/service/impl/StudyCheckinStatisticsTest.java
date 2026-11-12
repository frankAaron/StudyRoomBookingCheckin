package com.tyut.studyroombookingcheckin.service.impl;

import com.tyut.studyroombookingcheckin.cache.RankingCacheService;
import com.tyut.studyroombookingcheckin.mapper.BuildingMapper;
import com.tyut.studyroombookingcheckin.mapper.RoomMapper;
import com.tyut.studyroombookingcheckin.mapper.SeatMapper;
import com.tyut.studyroombookingcheckin.mapper.StudyCheckinMapper;
import com.tyut.studyroombookingcheckin.vo.StudyStatisticsVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 学习统计单元测试：聚合口径、连续/最长连续打卡、52 周热力图数据。
 */
class StudyCheckinStatisticsTest {

    private StudyCheckinMapper checkinMapper;
    private StudyCheckinServiceImpl service;

    @BeforeEach
    void setUp() {
        checkinMapper = mock(StudyCheckinMapper.class);
        service = new StudyCheckinServiceImpl(checkinMapper, mock(SeatMapper.class),
                mock(RoomMapper.class), mock(BuildingMapper.class), mock(RankingCacheService.class));
    }

    private StudyStatisticsVO.DailyMinutesVO day(LocalDate date, int minutes) {
        StudyStatisticsVO.DailyMinutesVO vo = new StudyStatisticsVO.DailyMinutesVO();
        vo.setDate(date);
        vo.setMinutes(minutes);
        return vo;
    }

    private StudyStatisticsVO getStatistics(Long userId, List<StudyStatisticsVO.DailyMinutesVO> data) {
        when(checkinMapper.selectDailyMinutes(eq(userId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(data);
        return service.getStatistics(userId);
    }

    @Test
    @DisplayName("今日/累计/连续打卡按一次按天聚合结果计算")
    void statisticsAggregatesFromSingleQuery() {
        LocalDate today = LocalDate.now();
        List<StudyStatisticsVO.DailyMinutesVO> data = new ArrayList<>();
        data.add(day(today, 120));
        data.add(day(today.minusDays(1), 60));
        data.add(day(today.minusDays(3), 45));

        StudyStatisticsVO vo = getStatistics(1L, data);

        assertThat(vo.getTodayMinutes()).isEqualTo(120);
        assertThat(vo.getTotalMinutes()).isEqualTo(225);
        assertThat(vo.getTotalDays()).isEqualTo(3);
        assertThat(vo.getCurrentStreak()).isEqualTo(2);
        assertThat(vo.getMaxStreak()).isEqualTo(2);
        // 只回源一次，杜绝热力图 52 周循环查询
        verify(checkinMapper, times(1)).selectDailyMinutes(eq(1L), any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    @DisplayName("最长连续打卡取历史区间最大值，当前连续中断时为 0")
    void maxStreakIndependentOfCurrentStreak() {
        LocalDate today = LocalDate.now();
        List<StudyStatisticsVO.DailyMinutesVO> data = new ArrayList<>();
        for (int i = 10; i <= 14; i++) {
            data.add(day(today.minusDays(i), 60));
        }

        StudyStatisticsVO vo = getStatistics(2L, data);

        assertThat(vo.getCurrentStreak()).isZero();
        assertThat(vo.getMaxStreak()).isEqualTo(5);
    }

    @Test
    @DisplayName("不足 10 分钟的打卡不计入连续天数")
    void shortSessionsNotCountedInStreak() {
        LocalDate today = LocalDate.now();
        List<StudyStatisticsVO.DailyMinutesVO> data = new ArrayList<>();
        data.add(day(today, 9));

        StudyStatisticsVO vo = getStatistics(3L, data);

        assertThat(vo.getTodayMinutes()).isEqualTo(9);
        assertThat(vo.getCurrentStreak()).isZero();
        assertThat(vo.getMaxStreak()).isZero();
    }

    @Test
    @DisplayName("热力图按天输出覆盖最近 52 周，且缺失日期补 0")
    void heatmapDaysCoverFiftyTwoWeeks() {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusWeeks(52).with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));

        StudyStatisticsVO vo = getStatistics(4L, new ArrayList<>(List.of(day(today, 100))));

        assertThat(vo.getHeatmapDays()).hasSize((int) (today.toEpochDay() - start.toEpochDay()) + 1);
        assertThat(vo.getHeatmapDays().get(vo.getHeatmapDays().size() - 1).getDate()).isEqualTo(today);
        assertThat(vo.getHeatmapDays().get(vo.getHeatmapDays().size() - 1).getMinutes()).isEqualTo(100);
        assertThat(vo.getDailyMinutes()).hasSize(30);
        assertThat(vo.getHeatmap()).hasSize(7);
    }
}
