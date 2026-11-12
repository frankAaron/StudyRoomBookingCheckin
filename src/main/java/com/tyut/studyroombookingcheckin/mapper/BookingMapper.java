package com.tyut.studyroombookingcheckin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tyut.studyroombookingcheckin.entity.Booking;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

public interface BookingMapper extends BaseMapper<Booking> {

    @Select("SELECT COUNT(*) FROM t_booking WHERE user_id = #{userId} AND seat_id = #{seatId} AND deleted = 0 " +
            "AND ((start_time <= #{endTime} AND end_time >= #{startTime})) AND status IN (0, 1)")
    Integer countConflictBookings(Long userId, Long seatId, LocalDateTime startTime, LocalDateTime endTime);

    @Select("SELECT COUNT(*) FROM t_booking WHERE seat_id = #{seatId} AND deleted = 0 " +
            "AND ((start_time <= #{endTime} AND end_time >= #{startTime})) AND status IN (0, 1)")
    Integer countSeatConflictBookings(Long seatId, LocalDateTime startTime, LocalDateTime endTime);
}