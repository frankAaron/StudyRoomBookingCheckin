package com.tyut.studyroombookingcheckin.controller;

import com.tyut.studyroombookingcheckin.common.Result;
import com.tyut.studyroombookingcheckin.dto.GoalCreateDTO;
import com.tyut.studyroombookingcheckin.security.SecurityContextHolder;
import com.tyut.studyroombookingcheckin.service.StudyGoalService;
import com.tyut.studyroombookingcheckin.vo.GoalVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/goals")
@RequiredArgsConstructor
public class StudyGoalController {

    private final StudyGoalService goalService;

    @PostMapping
    public Result<GoalVO> create(@Valid @RequestBody GoalCreateDTO dto) {
        Long userId = SecurityContextHolder.getUserId();
        return Result.success(goalService.create(userId, dto));
    }

    @PutMapping("/{id}")
    public Result<GoalVO> update(@PathVariable Long id, @RequestBody GoalCreateDTO dto) {
        Long userId = SecurityContextHolder.getUserId();
        return Result.success(goalService.update(id, userId, dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = SecurityContextHolder.getUserId();
        goalService.delete(id, userId);
        return Result.success();
    }

    @GetMapping("/mine")
    public Result<List<GoalVO>> mine() {
        Long userId = SecurityContextHolder.getUserId();
        return Result.success(goalService.listByUser(userId));
    }

    @GetMapping("/{id}")
    public Result<GoalVO> get(@PathVariable Long id) {
        return Result.success(goalService.getById(id));
    }
}