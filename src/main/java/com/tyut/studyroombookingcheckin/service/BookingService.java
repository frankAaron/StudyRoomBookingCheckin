package com.tyut.studyroombookingcheckin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tyut.studyroombookingcheckin.dto.BookingCreateDTO;
import com.tyut.studyroombookingcheckin.vo.BookingVO;

public interface BookingService {

    BookingVO create(Long userId, BookingCreateDTO dto);

    BookingVO checkin(Long bookingId, Long userId);

    BookingVO checkout(Long bookingId, Long userId);

    BookingVO cancel(Long bookingId, Long userId);

    Page<BookingVO> pageByUser(Long userId, long current, long size, Integer status);

    Page<BookingVO> pageAdmin(long current, long size, Integer status, Long roomId);

    BookingVO getById(Long id);

    void autoReleaseExpired();

    void autoReleaseTimeout();
}