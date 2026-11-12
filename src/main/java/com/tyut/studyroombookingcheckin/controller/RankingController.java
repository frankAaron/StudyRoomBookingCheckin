package com.tyut.studyroombookingcheckin.controller;

import com.tyut.studyroombookingcheckin.common.Result;
import com.tyut.studyroombookingcheckin.security.SecurityContextHolder;
import com.tyut.studyroombookingcheckin.service.RankingService;
import com.tyut.studyroombookingcheckin.vo.RankingVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ranking")
@RequiredArgsConstructor
public class RankingController {

    private final RankingService rankingService;

    @GetMapping("/daily")
    public Result<List<RankingVO>> daily(@RequestParam(defaultValue = "10") int topN) {
        return Result.success(rankingService.getDailyRanking(topN));
    }

    @GetMapping("/weekly")
    public Result<List<RankingVO>> weekly(@RequestParam(defaultValue = "10") int topN) {
        return Result.success(rankingService.getWeeklyRanking(topN));
    }

    @GetMapping("/monthly")
    public Result<List<RankingVO>> monthly(@RequestParam(defaultValue = "10") int topN) {
        return Result.success(rankingService.getMonthlyRanking(topN));
    }

    @GetMapping("/all")
    public Result<List<RankingVO>> all(@RequestParam(defaultValue = "10") int topN) {
        return Result.success(rankingService.getAllTimeRanking(topN));
    }

    @GetMapping("/my/daily")
    public Result<RankingVO> myDaily() {
        return Result.success(rankingService.getMyDailyRank(SecurityContextHolder.getUserId()));
    }

    @GetMapping("/my/weekly")
    public Result<RankingVO> myWeekly() {
        return Result.success(rankingService.getMyWeeklyRank(SecurityContextHolder.getUserId()));
    }

    @GetMapping("/my/monthly")
    public Result<RankingVO> myMonthly() {
        return Result.success(rankingService.getMyMonthlyRank(SecurityContextHolder.getUserId()));
    }
}