package com.tyut.studyroombookingcheckin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tyut.studyroombookingcheckin.common.BusinessException;
import com.tyut.studyroombookingcheckin.dto.GoalCreateDTO;
import com.tyut.studyroombookingcheckin.entity.StudyGoal;
import com.tyut.studyroombookingcheckin.mapper.StudyCheckinMapper;
import com.tyut.studyroombookingcheckin.mapper.StudyGoalMapper;
import com.tyut.studyroombookingcheckin.service.StudyGoalService;
import com.tyut.studyroombookingcheckin.vo.GoalVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StudyGoalServiceImpl implements StudyGoalService {

    private final StudyGoalMapper goalMapper;
    private final StudyCheckinMapper checkinMapper;

    @Override
    public GoalVO create(Long userId, GoalCreateDTO dto) {
        StudyGoal g = new StudyGoal();
        g.setUserId(userId);
        g.setTitle(dto.getTitle());
        g.setTargetMinutes(dto.getTargetMinutes());
        g.setStartDate(dto.getStartDate() != null ? dto.getStartDate() : LocalDate.now());
        g.setEndDate(dto.getEndDate() != null ? dto.getEndDate() : LocalDate.now().plusDays(30));
        g.setCompletedMinutes(0);
        g.setStatus(0);
        g.setCreateTime(LocalDateTime.now());
        g.setUpdateTime(LocalDateTime.now());
        goalMapper.insert(g);
        refreshGoalProgress(userId);
        return toVO(g);
    }

    @Override
    public GoalVO update(Long id, Long userId, GoalCreateDTO dto) {
        StudyGoal g = goalMapper.selectById(id);
        if (g == null || !g.getUserId().equals(userId)) {
            throw new BusinessException("学习目标不存在");
        }
        g.setTitle(dto.getTitle());
        g.setTargetMinutes(dto.getTargetMinutes());
        g.setEndDate(dto.getEndDate());
        g.setUpdateTime(LocalDateTime.now());
        goalMapper.updateById(g);
        refreshGoalProgress(userId);
        return toVO(g);
    }

    @Override
    public void delete(Long id, Long userId) {
        StudyGoal g = goalMapper.selectById(id);
        if (g == null || !g.getUserId().equals(userId)) {
            throw new BusinessException("学习目标不存在");
        }
        goalMapper.deleteById(id);
    }

    @Override
    public List<GoalVO> listByUser(Long userId) {
        List<StudyGoal> goals = goalMapper.selectList(new LambdaQueryWrapper<StudyGoal>()
                .eq(StudyGoal::getUserId, userId)
                .orderByDesc(StudyGoal::getCreateTime));
        refreshGoalProgress(userId);
        List<GoalVO> vos = new ArrayList<>();
        for (StudyGoal g : goals) {
            StudyGoal fresh = goalMapper.selectById(g.getId());
            vos.add(toVO(fresh));
        }
        return vos;
    }

    @Override
    public GoalVO getById(Long id) {
        StudyGoal g = goalMapper.selectById(id);
        if (g == null) {
            throw new BusinessException("学习目标不存在");
        }
        return toVO(g);
    }

    @Override
    public void refreshGoalProgress(Long userId) {
        List<StudyGoal> goals = goalMapper.selectList(new LambdaQueryWrapper<StudyGoal>()
                .eq(StudyGoal::getUserId, userId));
        for (StudyGoal g : goals) {
            Integer completed = checkinMapper.sumRangeMinutes(userId, g.getStartDate(), LocalDate.now());
            g.setCompletedMinutes(completed != null ? completed : 0);
            if (g.getCompletedMinutes() >= g.getTargetMinutes() && g.getStatus() == 0) {
                g.setStatus(1);
                g.setCompleteTime(LocalDateTime.now());
            }
            g.setUpdateTime(LocalDateTime.now());
            goalMapper.updateById(g);
        }
    }

    private GoalVO toVO(StudyGoal g) {
        GoalVO vo = new GoalVO();
        vo.setId(g.getId());
        vo.setTitle(g.getTitle());
        vo.setTargetMinutes(g.getTargetMinutes());
        vo.setStartDate(g.getStartDate());
        vo.setEndDate(g.getEndDate());
        vo.setCompletedMinutes(g.getCompletedMinutes());
        vo.setStatus(g.getStatus());
        vo.setProgressPercent(g.getTargetMinutes() > 0
                ? Math.min(100, g.getCompletedMinutes() * 100 / g.getTargetMinutes())
                : 0);
        return vo;
    }
}