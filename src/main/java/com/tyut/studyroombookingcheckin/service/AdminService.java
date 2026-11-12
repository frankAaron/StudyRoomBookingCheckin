package com.tyut.studyroombookingcheckin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tyut.studyroombookingcheckin.vo.BookingVO;

import java.util.Map;

public interface AdminService {

    Map<String, Object> getDashboard();

    Page<BookingVO> pageViolations(long current, long size, Integer type);

    Map<String, Object> getRoomUtilization();

    Map<String, Object> getUserStats();
}