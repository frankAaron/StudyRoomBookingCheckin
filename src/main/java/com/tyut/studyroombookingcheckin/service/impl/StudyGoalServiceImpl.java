package com.tyut.studyroombookingcheckin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tyut.studyroombookingcheckin.common.BusinessException;
import com.tyut.studyroombookingcheckin.dto.GoalCreateDTO;
import com.tyut.studyroombookingcheckin.entity.StudyGoal;
import com.tyut.studyroombookingcheckin.mapper.StudyCheckinMapper;
import com.tyut.studyroombookingcheckin.mapper.StudyGoalMapper;
import com.tyut.studyroombookingcheckin.service.StudyGoalService;
import com.tyut.studyroombookingcheckin.vo.GoalVO;
import com.tyut.studyroombookingcheckin.vo.StudyStatisticsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        return toVO(goalMapper.selectById(g.getId()));
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
        return toVO(goalMapper.selectById(id));
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
        refreshGoalProgress(userId);
        List<StudyGoal> goals = goalMapper.selectList(new LambdaQueryWrapper<StudyGoal>()
                .eq(StudyGoal::getUserId, userId)
                .orderByDesc(StudyGoal::getCreateTime));
        List<GoalVO> vos = new ArrayList<>();
        for (StudyGoal g : goals) {
            vos.add(toVO(g));
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

    /**
     * 一次按天聚合查询覆盖所有目标区间，替代逐目标 SUM 查询与逐条回查。
     */
    @Override
    public void refreshGoalProgress(Long userId) {
        List<StudyGoal> goals = goalMapper.selectList(new LambdaQueryWrapper<StudyGoal>()
                .eq(StudyGoal::getUserId, userId));
        if (goals.isEmpty()) {
            return;
        }
        LocalDate today = LocalDate.now();
        LocalDate minStart = today;
        for (StudyGoal g : goals) {
            if (g.getStartDate() != null && g.getStartDate().isBefore(minStart)) {
                minStart = g.getStartDate();
            }
        }
        List<StudyStatisticsVO.DailyMinutesVO> rows =
                checkinMapper.selectDailyMinutes(userId, minStart, today);
        Map<LocalDate, Integer> minutesMap = new HashMap<>();
        for (StudyStatisticsVO.DailyMinutesVO row : rows) {
            minutesMap.put(row.getDate(), row.getMinutes() == null ? 0 : row.getMinutes());
        }

        for (StudyGoal g : goals) {
            LocalDate start = g.getStartDate() != null ? g.getStartDate() : today;
            int completed = 0;
            for (LocalDate d = start; !d.isAfter(today); d = d.plusDays(1)) {
                completed += minutesMap.getOrDefault(d, 0);
            }
            g.setCompletedMinutes(completed);
            if (completed >= g.getTargetMinutes() && g.getStatus() == 0) {
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
