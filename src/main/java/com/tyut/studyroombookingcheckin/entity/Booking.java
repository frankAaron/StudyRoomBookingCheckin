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
@TableName("t_booking")
public class Booking implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long seatId;

    private Long roomId;

    private Long buildingId;

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

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}