package com.tyut.studyroombookingcheckin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tyut.studyroombookingcheckin.common.Result;
import com.tyut.studyroombookingcheckin.security.SecurityContextHolder;
import com.tyut.studyroombookingcheckin.service.StudyCheckinService;
import com.tyut.studyroombookingcheckin.vo.StudyCheckinVO;
import com.tyut.studyroombookingcheckin.vo.StudyStatisticsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/checkins")
@RequiredArgsConstructor
public class StudyCheckinController {

    private final StudyCheckinService studyCheckinService;

    @PostMapping("/start")
    public Result<StudyCheckinVO> start(@RequestParam Long seatId,
                                         @RequestParam(required = false) Long bookingId) {
        Long userId = SecurityContextHolder.getUserId();
        return Result.success(studyCheckinService.startCheckin(userId, seatId, bookingId));
    }

    @PostMapping("/{id}/end")
    public Result<StudyCheckinVO> end(@PathVariable Long id) {
        Long userId = SecurityContextHolder.getUserId();
        return Result.success(studyCheckinService.endCheckin(id, userId));
    }

    @GetMapping("/statistics")
    public Result<StudyStatisticsVO> statistics() {
        Long userId = SecurityContextHolder.getUserId();
        return Result.success(studyCheckinService.getStatistics(userId));
    }

    @GetMapping("/mine")
    public Result<Page<StudyCheckinVO>> myCheckins(@RequestParam(defaultValue = "1") long current,
                                                   @RequestParam(defaultValue = "10") long size) {
        Long userId = SecurityContextHolder.getUserId();
        return Result.success(studyCheckinService.pageByUser(userId, current, size));
    }
}