package com.tyut.studyroombookingcheckin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("t_seat")
public class Seat implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long roomId;

    private String seatNo;

    @TableField("seat_row")
    private Integer row;

    @TableField("seat_col")
    private Integer col;

    private String qrcode;

    private Integer status;

    private Long occupiedUserId;

    private LocalDateTime occupiedTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}