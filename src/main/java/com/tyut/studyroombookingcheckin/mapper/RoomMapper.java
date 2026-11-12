package com.tyut.studyroombookingcheckin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tyut.studyroombookingcheckin.entity.Room;
import org.apache.ibatis.annotations.Select;

public interface RoomMapper extends BaseMapper<Room> {

    @Select("SELECT COALESCE(SUM(status = 0), 0) FROM t_seat WHERE room_id = #{roomId} AND deleted = 0")
    Integer countAvailableSeats(Long roomId);

    @Select("SELECT COALESCE(SUM(status != 3), 0) FROM t_seat WHERE room_id = #{roomId} AND deleted = 0")
    Integer countTotalActiveSeats(Long roomId);
}