package com.tyut.studyroombookingcheckin.vo;

import lombok.Data;

import java.time.LocalDate;

@Data
public class GoalVO {

    private Long id;
    private String title;
    private Integer targetMinutes;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer completedMinutes;
    private Integer progressPercent;
    private Integer status;
}