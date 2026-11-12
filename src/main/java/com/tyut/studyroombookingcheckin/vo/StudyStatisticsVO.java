package com.tyut.studyroombookingcheckin.vo;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class StudyStatisticsVO {

    private Integer todayMinutes;
    private Integer weekMinutes;
    private Integer monthMinutes;
    private Integer totalMinutes;
    private Integer totalDays;
    private Integer currentStreak;
    private Integer maxStreak;
    private List<DailyMinutesVO> dailyMinutes;
    /** 最近 52 周按天聚合数据，供日历热力图使用（绝对刻度分级） */
    private List<DailyMinutesVO> heatmapDays;
    private List<Integer> heatmap;

    @Data
    public static class DailyMinutesVO {
        private LocalDate date;
        private Integer minutes;
    }
}