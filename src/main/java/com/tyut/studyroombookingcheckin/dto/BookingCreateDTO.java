package com.tyut.studyroombookingcheckin.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BookingCreateDTO {

    @NotNull(message = "座位ID不能为空")
    private Long seatId;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer expectedMinutes;

    private String remark;
}