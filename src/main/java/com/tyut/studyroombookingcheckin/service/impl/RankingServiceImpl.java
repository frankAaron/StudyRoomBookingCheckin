package com.tyut.studyroombookingcheckin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tyut.studyroombookingcheckin.entity.StudyCheckin;
import com.tyut.studyroombookingcheckin.entity.User;
import com.tyut.studyroombookingcheckin.mapper.StudyCheckinMapper;
import com.tyut.studyroombookingcheckin.mapper.UserMapper;
import com.tyut.studyroombookingcheckin.service.RankingService;
import com.tyut.studyroombookingcheckin.vo.RankingVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RankingServiceImpl implements RankingService {

    private final StudyCheckinMapper checkinMapper;
    private final UserMapper userMapper;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public List<RankingVO> getDailyRanking(int topN) {
        LocalDate today = LocalDate.now();
        return computeRanking(today, today, topN);
    }

    @Override
    public List<RankingVO> getWeeklyRanking(int topN) {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        return computeRanking(weekStart, today, topN);
    }

    @Override
    public List<RankingVO> getMonthlyRanking(int topN) {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.with(TemporalAdjusters.firstDayOfMonth());
        return computeRanking(monthStart, today, topN);
    }

    @Override
    public List<RankingVO> getAllTimeRanking(int topN) {
        return computeRanking(LocalDate.of(2020, 1, 1), LocalDate.now(), topN);
    }

    @Override
    public RankingVO getMyDailyRank(Long userId) {
        LocalDate today = LocalDate.now();
        List<RankingVO> list = computeRanking(today, today, Integer.MAX_VALUE);
        return findMyRank(list, userId);
    }

    @Override
    public RankingVO getMyWeeklyRank(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        List<RankingVO> list = computeRanking(weekStart, today, Integer.MAX_VALUE);
        return findMyRank(list, userId);
    }

    @Override
    public RankingVO getMyMonthlyRank(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.with(TemporalAdjusters.firstDayOfMonth());
        List<RankingVO> list = computeRanking(monthStart, today, Integer.MAX_VALUE);
        return findMyRank(list, userId);
    }

    private RankingVO findMyRank(List<RankingVO> list, Long userId) {
        for (RankingVO r : list) {
            if (r.getUserId().equals(userId)) {
                return r;
            }
        }
        RankingVO empty = new RankingVO();
        empty.setUserId(userId);
        empty.setTotalMinutes(0);
        empty.setRank(list.size() + 1);
        return empty;
    }

    private List<RankingVO> computeRanking(LocalDate start, LocalDate end, int topN) {
        LambdaQueryWrapper<StudyCheckin> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(StudyCheckin::getUserId, StudyCheckin::getDurationMinutes);
        wrapper.ge(StudyCheckin::getStudyDate, start);
        wrapper.le(StudyCheckin::getStudyDate, end);
        wrapper.isNotNull(StudyCheckin::getUserId);
        List<StudyCheckin> records = checkinMapper.selectList(wrapper);

        java.util.Map<Long, Integer> userMinutes = new java.util.HashMap<>();
        for (StudyCheckin r : records) {
            userMinutes.merge(r.getUserId(),
                    r.getDurationMinutes() != null ? r.getDurationMinutes() : 0,
                    Integer::sum);
        }

        List<java.util.Map.Entry<Long, Integer>> entries = new ArrayList<>(userMinutes.entrySet());
        entries.sort(java.util.Map.Entry.<Long, Integer>comparingByValue().reversed());

        List<RankingVO> result = new ArrayList<>();
        int limit = Math.min(topN, entries.size());
        for (int i = 0; i < limit; i++) {
            java.util.Map.Entry<Long, Integer> e = entries.get(i);
            User u = userMapper.selectById(e.getKey());
            if (u == null) continue;
            RankingVO vo = new RankingVO();
            vo.setUserId(u.getId());
            vo.setNickname(u.getNickname());
            vo.setAvatar(u.getAvatar());
            vo.setTotalMinutes(e.getValue());
            vo.setRank(i + 1);
            result.add(vo);
        }
        return result;
    }
}