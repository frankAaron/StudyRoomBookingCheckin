package com.tyut.studyroombookingcheckin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tyut.studyroombookingcheckin.dto.GoalCreateDTO;
import com.tyut.studyroombookingcheckin.vo.GoalVO;

import java.util.List;

public interface StudyGoalService {

    GoalVO create(Long userId, GoalCreateDTO dto);

    GoalVO update(Long id, Long userId, GoalCreateDTO dto);

    void delete(Long id, Long userId);

    List<GoalVO> listByUser(Long userId);

    GoalVO getById(Long id);

    void refreshGoalProgress(Long userId);
}