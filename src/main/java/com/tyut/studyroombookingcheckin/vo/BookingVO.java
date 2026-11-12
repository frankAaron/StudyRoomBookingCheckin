package com.tyut.studyroombookingcheckin.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class BookingVO {

    private Long id;
    private Long userId;
    private String userNickname;
    private Long seatId;
    private String seatNo;
    private Long roomId;
    private String roomName;
    private Long buildingId;
    private String buildingName;
    private LocalDate bookingDate;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer expectedMinutes;
    private Integer actualMinutes;
    private Integer status;
    private String checkinCode;
    private LocalDateTime checkinTime;
    private LocalDateTime checkoutTime;
    private String remark;
}