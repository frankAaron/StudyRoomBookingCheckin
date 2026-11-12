package com.tyut.studyroombookingcheckin.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StudyCheckinVO {

    private Long id;
    private Long userId;
    private Long seatId;
    private String seatNo;
    private String roomName;
    private String buildingName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer durationMinutes;
    private Integer status;
}