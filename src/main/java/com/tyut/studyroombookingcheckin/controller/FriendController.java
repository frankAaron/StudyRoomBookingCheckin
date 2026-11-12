package com.tyut.studyroombookingcheckin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tyut.studyroombookingcheckin.common.Result;
import com.tyut.studyroombookingcheckin.security.SecurityContextHolder;
import com.tyut.studyroombookingcheckin.service.FriendService;
import com.tyut.studyroombookingcheckin.vo.RankingVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    @PostMapping("/request/{friendId}")
    public Result<Void> sendRequest(@PathVariable Long friendId) {
        friendService.sendRequest(SecurityContextHolder.getUserId(), friendId);
        return Result.success();
    }

    @PostMapping("/accept/{friendId}")
    public Result<Void> accept(@PathVariable Long friendId) {
        friendService.acceptRequest(SecurityContextHolder.getUserId(), friendId);
        return Result.success();
    }

    @PostMapping("/reject/{friendId}")
    public Result<Void> reject(@PathVariable Long friendId) {
        friendService.rejectRequest(SecurityContextHolder.getUserId(), friendId);
        return Result.success();
    }

    @DeleteMapping("/{friendId}")
    public Result<Void> unfriend(@PathVariable Long friendId) {
        friendService.unfriend(SecurityContextHolder.getUserId(), friendId);
        return Result.success();
    }

    @GetMapping
    public Result<List<RankingVO>> list() {
        return Result.success(friendService.listFriends(SecurityContextHolder.getUserId()));
    }

    @GetMapping("/requests")
    public Result<Page<RankingVO>> requests(@RequestParam(defaultValue = "1") long current,
                                              @RequestParam(defaultValue = "10") long size) {
        return Result.success(friendService.pageRequests(SecurityContextHolder.getUserId(), current, size));
    }
}