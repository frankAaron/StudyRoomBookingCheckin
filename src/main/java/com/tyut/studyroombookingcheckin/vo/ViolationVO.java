package com.tyut.studyroombookingcheckin.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ViolationVO {

    private Long id;
    private Long userId;
    private String nickname;
    private Long bookingId;
    private Long seatId;
    private String seatNo;
    private String roomName;
    private Integer type;
    private String typeName;
    private String description;
    private Integer penalty;
    private Integer status;
    private LocalDateTime createTime;
}
