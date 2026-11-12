package com.tyut.studyroombookingcheckin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tyut.studyroombookingcheckin.common.Result;
import com.tyut.studyroombookingcheckin.service.AdminService;
import com.tyut.studyroombookingcheckin.vo.BookingVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard() {
        return Result.success(adminService.getDashboard());
    }

    @GetMapping("/bookings")
    public Result<Page<BookingVO>> pageBookings(@RequestParam(defaultValue = "1") long current,
                                                @RequestParam(defaultValue = "10") long size,
                                                @RequestParam(required = false) Integer status,
                                                @RequestParam(required = false) Long roomId) {
        return Result.success(adminService.pageViolations(current, size, status));
    }

    @GetMapping("/utilization")
    public Result<Map<String, Object>> utilization() {
        return Result.success(adminService.getRoomUtilization());
    }

    @GetMapping("/user-stats")
    public Result<Map<String, Object>> userStats() {
        return Result.success(adminService.getUserStats());
    }
}