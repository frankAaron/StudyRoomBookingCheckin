package com.tyut.studyroombookingcheckin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tyut.studyroombookingcheckin.common.BusinessException;
import com.tyut.studyroombookingcheckin.entity.Room;
import com.tyut.studyroombookingcheckin.entity.Seat;
import com.tyut.studyroombookingcheckin.entity.User;
import com.tyut.studyroombookingcheckin.entity.Violation;
import com.tyut.studyroombookingcheckin.mapper.RoomMapper;
import com.tyut.studyroombookingcheckin.mapper.SeatMapper;
import com.tyut.studyroombookingcheckin.mapper.UserMapper;
import com.tyut.studyroombookingcheckin.mapper.ViolationMapper;
import com.tyut.studyroombookingcheckin.service.ViolationService;
import com.tyut.studyroombookingcheckin.vo.ViolationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ViolationServiceImpl implements ViolationService {

    public static final int TYPE_NO_SHOW = 1;
    public static final int TYPE_OVERTIME = 2;
    public static final int TYPE_MANUAL = 3;

    private final ViolationMapper violationMapper;
    private final UserMapper userMapper;
    private final SeatMapper seatMapper;
    private final RoomMapper roomMapper;

    @Override
    public void record(Long userId, Long bookingId, Long seatId, Integer type, String description, Integer penalty) {
        if (userId == null || bookingId == null || type == null) {
            return;
        }
        Long exists = violationMapper.selectCount(new LambdaQueryWrapper<Violation>()
                .eq(Violation::getBookingId, bookingId)
                .eq(Violation::getType, type));
        if (exists != null && exists > 0) {
            return;
        }
        Violation v = new Violation();
        v.setUserId(userId);
        v.setBookingId(bookingId);
        v.setSeatId(seatId);
        v.setType(type);
        v.setDescription(description);
        v.setPenalty(penalty == null ? 0 : penalty);
        v.setStatus(0);
        v.setCreateTime(LocalDateTime.now());
        v.setUpdateTime(LocalDateTime.now());
        violationMapper.insert(v);
    }

    @Override
    public Page<ViolationVO> page(long current, long size, Integer type, Integer status) {
        LambdaQueryWrapper<Violation> wrapper = new LambdaQueryWrapper<>();
        if (type != null) {
            wrapper.eq(Violation::getType, type);
        }
        if (status != null) {
            wrapper.eq(Violation::getStatus, status);
        }
        wrapper.orderByDesc(Violation::getCreateTime);
        Page<Violation> page = violationMapper.selectPage(new Page<>(current, size), wrapper);

        List<ViolationVO> records = toVOList(page.getRecords());
        Page<ViolationVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(records);
        return voPage;
    }

    @Override
    public void handle(Long id, Integer status) {
        Violation v = violationMapper.selectById(id);
        if (v == null) {
            throw new BusinessException(404, "违规记录不存在");
        }
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(400, "处理状态不合法");
        }
        v.setStatus(status);
        v.setUpdateTime(LocalDateTime.now());
        violationMapper.updateById(v);
    }

    private List<ViolationVO> toVOList(List<Violation> list) {
        List<ViolationVO> result = new ArrayList<>();
        if (list == null || list.isEmpty()) {
            return result;
        }
        List<Long> userIds = new ArrayList<>();
        List<Long> seatIds = new ArrayList<>();
        for (Violation v : list) {
            if (v.getUserId() != null) {
                userIds.add(v.getUserId());
            }
            if (v.getSeatId() != null) {
                seatIds.add(v.getSeatId());
            }
        }
        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            for (User u : userMapper.selectBatchIds(userIds)) {
                userMap.put(u.getId(), u);
            }
        }
        Map<Long, Seat> seatMap = new HashMap<>();
        if (!seatIds.isEmpty()) {
            for (Seat s : seatMapper.selectBatchIds(seatIds)) {
                seatMap.put(s.getId(), s);
            }
        }
        List<Long> roomIds = new ArrayList<>();
        for (Seat s : seatMap.values()) {
            if (s.getRoomId() != null) {
                roomIds.add(s.getRoomId());
            }
        }
        Map<Long, Room> roomMap = new HashMap<>();
        if (!roomIds.isEmpty()) {
            for (Room r : roomMapper.selectBatchIds(roomIds)) {
                roomMap.put(r.getId(), r);
            }
        }

        for (Violation v : list) {
            ViolationVO vo = new ViolationVO();
            vo.setId(v.getId());
            vo.setUserId(v.getUserId());
            User u = userMap.get(v.getUserId());
            if (u != null) {
                vo.setNickname(u.getNickname());
            }
            vo.setBookingId(v.getBookingId());
            vo.setSeatId(v.getSeatId());
            Seat seat = seatMap.get(v.getSeatId());
            if (seat != null) {
                vo.setSeatNo(seat.getSeatNo());
                Room room = roomMap.get(seat.getRoomId());
                if (room != null) {
                    vo.setRoomName(room.getName());
                }
            }
            vo.setType(v.getType());
            vo.setTypeName(typeName(v.getType()));
            vo.setDescription(v.getDescription());
            vo.setPenalty(v.getPenalty());
            vo.setStatus(v.getStatus());
            vo.setCreateTime(v.getCreateTime());
            result.add(vo);
        }
        return result;
    }

    private String typeName(Integer type) {
        if (type == null) {
            return "未知";
        }
        return switch (type) {
            case TYPE_NO_SHOW -> "爽约未签到";
            case TYPE_OVERTIME -> "超时占座";
            default -> "其他违规";
        };
    }
}
