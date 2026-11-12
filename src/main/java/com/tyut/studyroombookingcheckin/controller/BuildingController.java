package com.tyut.studyroombookingcheckin.controller;

import com.tyut.studyroombookingcheckin.common.Result;
import com.tyut.studyroombookingcheckin.service.BuildingService;
import com.tyut.studyroombookingcheckin.vo.BuildingVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/buildings")
@RequiredArgsConstructor
public class BuildingController {

    private final BuildingService buildingService;

    @GetMapping
    public Result<List<BuildingVO>> list() {
        return Result.success(buildingService.listAll());
    }

    @GetMapping("/{id}")
    public Result<BuildingVO> get(@PathVariable Long id) {
        return Result.success(buildingService.getById(id));
    }

    @PostMapping
    public Result<BuildingVO> create(@RequestBody BuildingVO vo) {
        return Result.success(buildingService.create(vo));
    }

    @PutMapping("/{id}")
    public Result<BuildingVO> update(@PathVariable Long id, @RequestBody BuildingVO vo) {
        return Result.success(buildingService.update(id, vo));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        buildingService.delete(id);
        return Result.success();
    }
}