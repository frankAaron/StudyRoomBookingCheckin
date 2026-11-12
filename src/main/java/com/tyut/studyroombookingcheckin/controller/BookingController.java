package com.tyut.studyroombookingcheckin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tyut.studyroombookingcheckin.common.Result;
import com.tyut.studyroombookingcheckin.dto.BookingCreateDTO;
import com.tyut.studyroombookingcheckin.security.SecurityContextHolder;
import com.tyut.studyroombookingcheckin.service.BookingService;
import com.tyut.studyroombookingcheckin.vo.BookingVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public Result<BookingVO> create(@Valid @RequestBody BookingCreateDTO dto) {
        Long userId = SecurityContextHolder.getUserId();
        return Result.success(bookingService.create(userId, dto));
    }

    @PostMapping("/checkin/code")
    public Result<BookingVO> checkinByCode(@RequestParam String code) {
        Long userId = SecurityContextHolder.getUserId();
        return Result.success(bookingService.checkinByCode(code, userId));
    }

    @PostMapping("/{id}/checkin")
    public Result<BookingVO> checkin(@PathVariable Long id) {
        Long userId = SecurityContextHolder.getUserId();
        return Result.success(bookingService.checkin(id, userId));
    }

    @PostMapping("/{id}/checkout")
    public Result<BookingVO> checkout(@PathVariable Long id) {
        Long userId = SecurityContextHolder.getUserId();
        return Result.success(bookingService.checkout(id, userId));
    }

    @PostMapping("/{id}/cancel")
    public Result<BookingVO> cancel(@PathVariable Long id) {
        Long userId = SecurityContextHolder.getUserId();
        return Result.success(bookingService.cancel(id, userId));
    }

    @GetMapping("/mine")
    public Result<Page<BookingVO>> myBookings(@RequestParam(defaultValue = "1") long current,
                                               @RequestParam(defaultValue = "10") long size,
                                               @RequestParam(required = false) Integer status) {
        Long userId = SecurityContextHolder.getUserId();
        return Result.success(bookingService.pageByUser(userId, current, size, status));
    }

    @GetMapping("/{id}")
    public Result<BookingVO> get(@PathVariable Long id) {
        return Result.success(bookingService.getById(id));
    }
}