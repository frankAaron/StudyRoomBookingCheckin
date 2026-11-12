package com.tyut.studyroombookingcheckin.service;

import com.tyut.studyroombookingcheckin.vo.BuildingVO;

import java.util.List;

public interface BuildingService {

    List<BuildingVO> listAll();

    BuildingVO getById(Long id);

    BuildingVO create(BuildingVO vo);

    BuildingVO update(Long id, BuildingVO vo);

    void delete(Long id);
}