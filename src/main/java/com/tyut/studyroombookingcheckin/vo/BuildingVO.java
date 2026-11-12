package com.tyut.studyroombookingcheckin.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BuildingVO {

    private Long id;
    private String name;
    private String code;
    private String location;
    private String description;
    private Integer roomCount;
    private Integer availableSeats;
}