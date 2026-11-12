package com.tyut.studyroombookingcheckin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tyut.studyroombookingcheckin.vo.StudyCheckinVO;
import com.tyut.studyroombookingcheckin.vo.StudyStatisticsVO;

public interface StudyCheckinService {

    StudyCheckinVO startCheckin(Long userId, Long seatId, Long bookingId);

    StudyCheckinVO endCheckin(Long checkinId, Long userId);

    /**
     * 预约离座时同步结束该预约对应的进行中打卡
     */
    void endByBooking(Long bookingId);

    StudyStatisticsVO getStatistics(Long userId);

    Page<StudyCheckinVO> pageByUser(Long userId, long current, long size);
}