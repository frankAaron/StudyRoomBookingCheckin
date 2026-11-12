package com.tyut.studyroombookingcheckin.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.tyut.studyroombookingcheckin.cache.RankingCacheService;
import com.tyut.studyroombookingcheckin.entity.StudyCheckin;
import com.tyut.studyroombookingcheckin.entity.User;
import com.tyut.studyroombookingcheckin.mapper.StudyCheckinMapper;
import com.tyut.studyroombookingcheckin.mapper.UserMapper;
import com.tyut.studyroombookingcheckin.vo.RankingVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 排行榜单元测试：排序、截断、我的排名、批量加载用户（无 N+1）。
 */
class RankingServiceImplTest {

    private StudyCheckinMapper checkinMapper;
    private UserMapper userMapper;
    private RankingServiceImpl service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        // 单测无 Spring 上下文，先初始化 MyBatis-Plus 的 Lambda 列缓存，供 select(...) 解析列名
        TableInfoHelper.initTableInfo(
                new org.apache.ibatis.builder.MapperBuilderAssistant(new MybatisConfiguration(), ""),
                StudyCheckin.class);
        checkinMapper = mock(StudyCheckinMapper.class);
        userMapper = mock(UserMapper.class);
        RankingCacheService cache = mock(RankingCacheService.class);
        when(cache.getDaily(anyString(), any())).thenAnswer(inv ->
                ((Supplier<List<RankingVO>>) inv.getArgument(1)).get());
        service = new RankingServiceImpl(checkinMapper, userMapper, cache);
    }

    private StudyCheckin record(long userId, int minutes) {
        StudyCheckin c = new StudyCheckin();
        c.setUserId(userId);
        c.setDurationMinutes(minutes);
        return c;
    }

    private User user(long id, String nickname) {
        User u = new User();
        u.setId(id);
        u.setNickname(nickname);
        return u;
    }

    private void stubData() {
        when(checkinMapper.selectList(any())).thenReturn(new ArrayList<>(List.of(
                record(1L, 60), record(1L, 40), record(2L, 30), record(3L, 90))));
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(
                user(1L, "张三"), user(2L, "李四"), user(3L, "王五")));
    }

    @Test
    @DisplayName("日榜按累计时长倒序排名，用户信息一次批量加载")
    void dailyRankingSortedAndBatchLoaded() {
        stubData();

        List<RankingVO> ranking = service.getDailyRanking(10);

        assertThat(ranking).hasSize(3);
        // 同一用户多条打卡累计后排序：1 号 100 分钟 > 3 号 90 分钟 > 2 号 30 分钟
        assertThat(ranking.get(0).getUserId()).isEqualTo(1L);
        assertThat(ranking.get(0).getTotalMinutes()).isEqualTo(100);
        assertThat(ranking.get(0).getRank()).isEqualTo(1);
        assertThat(ranking.get(1).getUserId()).isEqualTo(3L);
        assertThat(ranking.get(1).getTotalMinutes()).isEqualTo(90);
        assertThat(ranking.get(2).getRank()).isEqualTo(3);
        verify(userMapper, times(1)).selectBatchIds(any());
    }

    @Test
    @DisplayName("榜单按 topN 截断，但缓存内保留完整榜单以复用")
    void rankingTruncatedByTopN() {
        stubData();

        assertThat(service.getDailyRanking(2)).hasSize(2);
    }

    @Test
    @DisplayName("未上榜用户返回第 list.size()+1 名且时长为 0")
    void myRankWhenNotOnBoard() {
        stubData();

        RankingVO mine = service.getMyDailyRank(99L);

        assertThat(mine.getUserId()).isEqualTo(99L);
        assertThat(mine.getTotalMinutes()).isZero();
        assertThat(mine.getRank()).isEqualTo(4);
    }
}
