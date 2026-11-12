package com.tyut.studyroombookingcheckin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("t_study_goal")
public class StudyGoal implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String title;

    private Integer targetMinutes;

    private LocalDate startDate;

    private LocalDate endDate;

    private Integer completedMinutes;

    private Integer status;

    private LocalDateTime completeTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}