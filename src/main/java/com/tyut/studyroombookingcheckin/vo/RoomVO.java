package com.tyut.studyroombookingcheckin.vo;

import lombok.Data;

import java.time.LocalTime;

@Data
public class RoomVO {

    private Long id;
    private Long buildingId;
    private String buildingName;
    private String name;
    private String code;
    private Integer floor;
    private Integer totalSeats;
    private Integer availableSeats;
    private String equipment;
    private LocalTime openTime;
    private LocalTime closeTime;
    private Double utilizationRate;
}