package com.tyut.studyroombookingcheckin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tyut.studyroombookingcheckin.cache.RankingCacheService;
import com.tyut.studyroombookingcheckin.entity.StudyCheckin;
import com.tyut.studyroombookingcheckin.entity.User;
import com.tyut.studyroombookingcheckin.mapper.StudyCheckinMapper;
import com.tyut.studyroombookingcheckin.mapper.UserMapper;
import com.tyut.studyroombookingcheckin.service.RankingService;
import com.tyut.studyroombookingcheckin.vo.RankingVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RankingServiceImpl implements RankingService {

    private final StudyCheckinMapper checkinMapper;
    private final UserMapper userMapper;
    private final RankingCacheService rankingCacheService;

    @Override
    public List<RankingVO> getDailyRanking(int topN) {
        LocalDate today = LocalDate.now();
        return limit(rankingCacheService.getDaily(today.toString(),
                () -> computeRanking(today, today)), topN);
    }

    @Override
    public List<RankingVO> getWeeklyRanking(int topN) {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return limit(rankingCacheService.getWeekly(weekStart.toString(),
                () -> computeRanking(weekStart, today)), topN);
    }

    @Override
    public List<RankingVO> getMonthlyRanking(int topN) {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.with(TemporalAdjusters.firstDayOfMonth());
        return limit(rankingCacheService.getMonthly(YearMonth.from(today).toString(),
                () -> computeRanking(monthStart, today)), topN);
    }

    @Override
    public List<RankingVO> getAllTimeRanking(int topN) {
        return limit(rankingCacheService.getAllTime(
                () -> computeRanking(LocalDate.of(2020, 1, 1), LocalDate.now())), topN);
    }

    @Override
    public RankingVO getMyDailyRank(Long userId) {
        LocalDate today = LocalDate.now();
        return findMyRank(rankingCacheService.getDaily(today.toString(),
                () -> computeRanking(today, today)), userId);
    }

    @Override
    public RankingVO getMyWeeklyRank(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return findMyRank(rankingCacheService.getWeekly(weekStart.toString(),
                () -> computeRanking(weekStart, today)), userId);
    }

    @Override
    public RankingVO getMyMonthlyRank(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.with(TemporalAdjusters.firstDayOfMonth());
        return findMyRank(rankingCacheService.getMonthly(YearMonth.from(today).toString(),
                () -> computeRanking(monthStart, today)), userId);
    }

    private List<RankingVO> limit(List<RankingVO> all, int topN) {
        if (all == null || all.isEmpty() || topN <= 0) {
            return new ArrayList<>();
        }
        return new ArrayList<>(all.subList(0, Math.min(topN, all.size())));
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

    /**
     * 计算周期内完整排行榜（不做截断，便于缓存复用与"我的排名"查询）。
     * 用户信息通过 selectBatchIds 批量加载，避免 N+1。
     */
    private List<RankingVO> computeRanking(LocalDate start, LocalDate end) {
        LambdaQueryWrapper<StudyCheckin> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(StudyCheckin::getUserId, StudyCheckin::getDurationMinutes);
        wrapper.ge(StudyCheckin::getStudyDate, start);
        wrapper.le(StudyCheckin::getStudyDate, end);
        wrapper.isNotNull(StudyCheckin::getUserId);
        List<StudyCheckin> records = checkinMapper.selectList(wrapper);

        Map<Long, Integer> userMinutes = new HashMap<>();
        for (StudyCheckin r : records) {
            userMinutes.merge(r.getUserId(),
                    r.getDurationMinutes() != null ? r.getDurationMinutes() : 0,
                    Integer::sum);
        }

        List<Map.Entry<Long, Integer>> entries = new ArrayList<>(userMinutes.entrySet());
        entries.sort(Map.Entry.<Long, Integer>comparingByValue().reversed());

        List<Long> userIds = new ArrayList<>(entries.size());
        for (Map.Entry<Long, Integer> e : entries) {
            userIds.add(e.getKey());
        }
        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            for (User u : userMapper.selectBatchIds(userIds)) {
                userMap.put(u.getId(), u);
            }
        }

        List<RankingVO> result = new ArrayList<>();
        int rank = 0;
        for (Map.Entry<Long, Integer> e : entries) {
            User u = userMap.get(e.getKey());
            if (u == null) {
                continue;
            }
            rank++;
            RankingVO vo = new RankingVO();
            vo.setUserId(u.getId());
            vo.setNickname(u.getNickname());
            vo.setAvatar(u.getAvatar());
            vo.setTotalMinutes(e.getValue());
            vo.setRank(rank);
            result.add(vo);
        }
        return result;
    }
}
