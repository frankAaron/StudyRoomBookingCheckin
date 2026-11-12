package com.tyut.studyroombookingcheckin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tyut.studyroombookingcheckin.entity.StudyCheckin;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface StudyCheckinMapper extends BaseMapper<StudyCheckin> {

    @Select("SELECT COALESCE(SUM(duration_minutes), 0) FROM t_study_checkin WHERE user_id = #{userId} AND study_date = #{date} AND deleted = 0")
    Integer sumDailyMinutes(Long userId, LocalDate date);

    @Select("SELECT COALESCE(SUM(duration_minutes), 0) FROM t_study_checkin WHERE user_id = #{userId} AND study_date BETWEEN #{startDate} AND #{endDate} AND deleted = 0")
    Integer sumRangeMinutes(Long userId, LocalDate startDate, LocalDate endDate);

    @Select("SELECT DISTINCT study_date FROM t_study_checkin WHERE user_id = #{userId} AND study_date BETWEEN #{startDate} AND #{endDate} AND deleted = 0 ORDER BY study_date")
    List<LocalDate> selectStudyDates(Long userId, LocalDate startDate, LocalDate endDate);

    @Select("SELECT COUNT(DISTINCT study_date) FROM t_study_checkin WHERE user_id = #{userId} AND deleted = 0")
    Integer countStudyDays(Long userId);

    @Select("SELECT COALESCE(SUM(duration_minutes), 0) FROM t_study_checkin WHERE deleted = 0 AND start_time >= #{since}")
    Integer sumAllMinutesSince(LocalDateTime since);
}