package com.tyut.studyroombookingcheckin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tyut.studyroombookingcheckin.vo.ViolationVO;

public interface ViolationService {

    /** 自动写入违规记录（同一预约同一类型只记一次）。 */
    void record(Long userId, Long bookingId, Long seatId, Integer type, String description, Integer penalty);

    Page<ViolationVO> page(long current, long size, Integer type, Integer status);

    void handle(Long id, Integer status);
}
