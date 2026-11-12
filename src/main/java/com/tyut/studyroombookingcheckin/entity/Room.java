package com.tyut.studyroombookingcheckin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@TableName("t_room")
public class Room implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long buildingId;

    private String name;

    private String code;

    private Integer floor;

    private Integer totalSeats;

    private Integer availableSeats;

    private String equipment;

    private LocalTime openTime;

    private LocalTime closeTime;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}