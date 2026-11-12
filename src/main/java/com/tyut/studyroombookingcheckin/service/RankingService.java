package com.tyut.studyroombookingcheckin.service;

import com.tyut.studyroombookingcheckin.vo.RankingVO;

import java.util.List;

public interface RankingService {

    List<RankingVO> getDailyRanking(int topN);

    List<RankingVO> getWeeklyRanking(int topN);

    List<RankingVO> getMonthlyRanking(int topN);

    List<RankingVO> getAllTimeRanking(int topN);

    RankingVO getMyDailyRank(Long userId);

    RankingVO getMyWeeklyRank(Long userId);

    RankingVO getMyMonthlyRank(Long userId);
}