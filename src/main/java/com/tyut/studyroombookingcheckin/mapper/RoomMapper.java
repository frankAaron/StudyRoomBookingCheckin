package com.tyut.studyroombookingcheckin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tyut.studyroombookingcheckin.entity.Room;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface RoomMapper extends BaseMapper<Room> {

    @Select("SELECT COALESCE(SUM(status = 0), 0) FROM t_seat WHERE room_id = #{roomId} AND deleted = 0")
    Integer countAvailableSeats(@Param("roomId") Long roomId);

    @Select("SELECT COALESCE(SUM(status != 3), 0) FROM t_seat WHERE room_id = #{roomId} AND deleted = 0")
    Integer countTotalActiveSeats(@Param("roomId") Long roomId);

    /**
     * 一次分组统计所有房间的座位总数与空闲数，用于使用率报表，避免逐房间查询。
     */
    @Select("SELECT room_id AS roomId, COUNT(*) AS total, COALESCE(SUM(status = 0), 0) AS available " +
            "FROM t_seat WHERE deleted = 0 AND status != 3 GROUP BY room_id")
    List<Map<String, Object>> selectSeatUsageGrouped();
}
