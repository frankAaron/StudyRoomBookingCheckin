package com.tyut.studyroombookingcheckin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tyut.studyroombookingcheckin.common.Result;
import com.tyut.studyroombookingcheckin.service.RoomService;
import com.tyut.studyroombookingcheckin.vo.RoomVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @GetMapping
    public Result<List<RoomVO>> listByBuilding(@RequestParam(required = false) Long buildingId) {
        if (buildingId != null) {
            return Result.success(roomService.listByBuilding(buildingId));
        }
        Page<RoomVO> page = roomService.page(1, Integer.MAX_VALUE, null);
        return Result.success(page.getRecords());
    }

    @GetMapping("/page")
    public Result<Page<RoomVO>> page(@RequestParam(defaultValue = "1") long current,
                                     @RequestParam(defaultValue = "10") long size,
                                     @RequestParam(required = false) Long buildingId) {
        return Result.success(roomService.page(current, size, buildingId));
    }

    @GetMapping("/{id}")
    public Result<RoomVO> get(@PathVariable Long id) {
        return Result.success(roomService.getById(id));
    }

    @PostMapping
    public Result<RoomVO> create(@RequestBody RoomVO vo) {
        return Result.success(roomService.create(vo));
    }

    @PutMapping("/{id}")
    public Result<RoomVO> update(@PathVariable Long id, @RequestBody RoomVO vo) {
        return Result.success(roomService.update(id, vo));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        roomService.delete(id);
        return Result.success();
    }
}