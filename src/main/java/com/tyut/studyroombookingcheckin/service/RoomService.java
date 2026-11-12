package com.tyut.studyroombookingcheckin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tyut.studyroombookingcheckin.vo.RoomVO;

import java.util.List;

public interface RoomService {

    List<RoomVO> listByBuilding(Long buildingId);

    RoomVO getById(Long id);

    RoomVO create(RoomVO vo);

    RoomVO update(Long id, RoomVO vo);

    void delete(Long id);

    Page<RoomVO> page(long current, long size, Long buildingId);
}