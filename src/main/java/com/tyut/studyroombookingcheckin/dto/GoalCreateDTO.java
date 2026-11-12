package com.tyut.studyroombookingcheckin.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class GoalCreateDTO {

    @NotNull(message = "目标标题不能为空")
    private String title;

    @NotNull(message = "目标时长不能为空")
    private Integer targetMinutes;

    private LocalDate startDate;

    private LocalDate endDate;
}