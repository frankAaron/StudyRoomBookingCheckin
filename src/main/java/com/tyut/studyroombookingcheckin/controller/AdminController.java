package com.tyut.studyroombookingcheckin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tyut.studyroombookingcheckin.common.Result;
import com.tyut.studyroombookingcheckin.dto.AdminUserSaveDTO;
import com.tyut.studyroombookingcheckin.entity.AdminOperationLog;
import com.tyut.studyroombookingcheckin.security.RequireAdmin;
import com.tyut.studyroombookingcheckin.service.AdminService;
import com.tyut.studyroombookingcheckin.service.AdminUserService;
import com.tyut.studyroombookingcheckin.service.ViolationService;
import com.tyut.studyroombookingcheckin.vo.AdminUserVO;
import com.tyut.studyroombookingcheckin.vo.BookingVO;
import com.tyut.studyroombookingcheckin.vo.ViolationVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@RequireAdmin
public class AdminController {

    private final AdminService adminService;
    private final AdminUserService adminUserService;
    private final ViolationService violationService;

    @GetMapping("/violations")
    public Result<Page<ViolationVO>> pageViolationRecords(@RequestParam(defaultValue = "1") long current,
                                                          @RequestParam(defaultValue = "10") long size,
                                                          @RequestParam(required = false) Integer type,
                                                          @RequestParam(required = false) Integer status) {
        return Result.success(violationService.page(current, size, type, status));
    }

    @PutMapping("/violations/{id}/handle")
    public Result<Void> handleViolation(@PathVariable Long id, @RequestParam Integer status) {
        violationService.handle(id, status);
        return Result.success("处理成功", null);
    }

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

    /* ------------------------- 用户管理 ------------------------- */

    @GetMapping("/users")
    public Result<Page<AdminUserVO>> pageUsers(@RequestParam(defaultValue = "1") long current,
                                               @RequestParam(defaultValue = "10") long size,
                                               @RequestParam(required = false) String keyword,
                                               @RequestParam(required = false) Integer role,
                                               @RequestParam(required = false) Integer status) {
        return Result.success(adminUserService.pageUsers(current, size, keyword, role, status));
    }

    @PostMapping("/users")
    public Result<AdminUserVO> createUser(@Valid @RequestBody AdminUserSaveDTO dto) {
        return Result.success("用户已创建", adminUserService.createUser(dto));
    }

    @PutMapping("/users/{id}")
    public Result<AdminUserVO> updateUser(@PathVariable Long id,
                                          @Valid @RequestBody AdminUserSaveDTO dto) {
        return Result.success("已保存", adminUserService.updateUser(id, dto));
    }

    @DeleteMapping("/users/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        adminUserService.deleteUser(id);
        return Result.success("用户已删除", null);
    }

    @PutMapping("/users/{id}/status")
    public Result<Void> updateUserStatus(@PathVariable Long id, @RequestParam Integer status) {
        adminUserService.updateStatus(id, status);
        return Result.success("账号状态已更新", null);
    }

    @GetMapping("/users/logs")
    public Result<Page<AdminOperationLog>> pageUserLogs(@RequestParam(defaultValue = "1") long current,
                                                        @RequestParam(defaultValue = "10") long size) {
        return Result.success(adminUserService.pageLogs(current, size));
    }
}
