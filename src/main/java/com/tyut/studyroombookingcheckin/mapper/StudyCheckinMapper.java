package com.tyut.studyroombookingcheckin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tyut.studyroombookingcheckin.entity.StudyCheckin;
import com.tyut.studyroombookingcheckin.vo.StudyStatisticsVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface StudyCheckinMapper extends BaseMapper<StudyCheckin> {

    @Select("SELECT COALESCE(SUM(duration_minutes), 0) FROM t_study_checkin WHERE user_id = #{userId} AND study_date = #{date} AND deleted = 0")
    Integer sumDailyMinutes(@Param("userId") Long userId, @Param("date") LocalDate date);

    @Select("SELECT COALESCE(SUM(duration_minutes), 0) FROM t_study_checkin WHERE user_id = #{userId} AND study_date BETWEEN #{startDate} AND #{endDate} AND deleted = 0")
    Integer sumRangeMinutes(@Param("userId") Long userId,
                            @Param("startDate") LocalDate startDate,
                            @Param("endDate") LocalDate endDate);

    @Select("SELECT COALESCE(SUM(duration_minutes), 0) FROM t_study_checkin WHERE study_date BETWEEN #{startDate} AND #{endDate} AND deleted = 0")
    Integer sumAllDailyMinutes(@Param("startDate") LocalDate startDate,
                               @Param("endDate") LocalDate endDate);

    @Select("SELECT DISTINCT study_date FROM t_study_checkin WHERE user_id = #{userId} AND study_date BETWEEN #{startDate} AND #{endDate} AND deleted = 0 ORDER BY study_date")
    List<LocalDate> selectStudyDates(@Param("userId") Long userId,
                                     @Param("startDate") LocalDate startDate,
                                     @Param("endDate") LocalDate endDate);

    /**
     * 按天聚合学习时长，一次查询替代逐天查询（热力图 / 趋势图 / 连续打卡）。
     */
    @Select("SELECT study_date AS date, COALESCE(SUM(duration_minutes), 0) AS minutes " +
            "FROM t_study_checkin WHERE user_id = #{userId} AND study_date BETWEEN #{startDate} AND #{endDate} " +
            "AND deleted = 0 GROUP BY study_date ORDER BY study_date")
    List<StudyStatisticsVO.DailyMinutesVO> selectDailyMinutes(@Param("userId") Long userId,
                                                              @Param("startDate") LocalDate startDate,
                                                              @Param("endDate") LocalDate endDate);

    @Select("SELECT COUNT(DISTINCT study_date) FROM t_study_checkin WHERE user_id = #{userId} AND deleted = 0")
    Integer countStudyDays(@Param("userId") Long userId);

    @Select("SELECT COALESCE(SUM(duration_minutes), 0) FROM t_study_checkin WHERE deleted = 0 AND start_time >= #{since}")
    Integer sumAllMinutesSince(@Param("since") LocalDateTime since);

    @Select("SELECT COUNT(DISTINCT user_id) FROM t_study_checkin WHERE deleted = 0 AND start_time >= #{since}")
    Integer countActiveUsersSince(@Param("since") LocalDateTime since);
}
