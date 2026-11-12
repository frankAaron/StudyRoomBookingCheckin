package com.tyut.studyroombookingcheckin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tyut.studyroombookingcheckin.entity.Seat;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

public interface SeatMapper extends BaseMapper<Seat> {

    /**
     * 悲观锁读取座位，用于预约创建时串行化同一座位的并发请求。
     * 必须在事务中调用。
     * <p>
     * 显式列出列名并为 seat_col 起别名：手写 SQL 不会应用实体的 @TableField 映射，
     * 否则 col 字段会因列名 seat_col 无法自动映射而丢失。
     */
    @Select("SELECT id, room_id, seat_no, seat_row, seat_col AS col, qrcode, status, " +
            "occupied_user_id, occupied_time, create_time, update_time, deleted " +
            "FROM t_seat WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    Seat selectByIdForUpdate(@Param("id") Long id);

    /**
     * 原子更新座位状态（用于预约/签到/离座），避免读改写丢更新。
     */
    @Update("UPDATE t_seat SET status = #{status}, occupied_user_id = #{occupiedUserId}, " +
            "occupied_time = #{occupiedTime}, update_time = #{now} " +
            "WHERE id = #{seatId} AND deleted = 0")
    int updateSeatState(@Param("seatId") Long seatId,
                        @Param("status") Integer status,
                        @Param("occupiedUserId") Long occupiedUserId,
                        @Param("occupiedTime") LocalDateTime occupiedTime,
                        @Param("now") LocalDateTime now);
}
