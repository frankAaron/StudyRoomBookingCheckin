package com.tyut.studyroombookingcheckin.vo;

import lombok.Data;

@Data
public class SeatVO {

    private Long id;
    private Long roomId;
    private String seatNo;
    private Integer row;
    private Integer col;
    private Integer status;
    private Long occupiedUserId;
    private String qrcode;
}