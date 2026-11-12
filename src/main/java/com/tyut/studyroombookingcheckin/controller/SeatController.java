package com.tyut.studyroombookingcheckin.controller;

import com.tyut.studyroombookingcheckin.common.Result;
import com.tyut.studyroombookingcheckin.service.SeatService;
import com.tyut.studyroombookingcheckin.vo.SeatVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/seats")
@RequiredArgsConstructor
public class SeatController {

    private final SeatService seatService;

    @GetMapping
    public Result<List<SeatVO>> listByRoom(@RequestParam Long roomId) {
        return Result.success(seatService.listByRoom(roomId));
    }

    @GetMapping("/{id}")
    public Result<SeatVO> get(@PathVariable Long id) {
        return Result.success(seatService.getById(id));
    }

    @GetMapping("/qrcode/{qrcode}")
    public Result<SeatVO> getByQrcode(@PathVariable String qrcode) {
        return Result.success(seatService.getByQrcode(qrcode));
    }

    @PostMapping
    public Result<SeatVO> create(@RequestBody SeatVO vo) {
        return Result.success(seatService.create(vo));
    }

    @PostMapping("/batch")
    public Result<Void> batchCreate(@RequestParam Long roomId,
                                    @RequestParam(defaultValue = "6") int rows,
                                    @RequestParam(defaultValue = "10") int cols) {
        seatService.batchCreate(roomId, rows, cols);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<SeatVO> update(@PathVariable Long id, @RequestBody SeatVO vo) {
        return Result.success(seatService.update(id, vo));
    }

    @PutMapping("/{id}/status")
    public Result<SeatVO> changeStatus(@PathVariable Long id, @RequestParam Integer status) {
        return Result.success(seatService.changeStatus(id, status));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        seatService.delete(id);
        return Result.success();
    }
}