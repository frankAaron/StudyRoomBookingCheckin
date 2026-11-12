package com.tyut.studyroombookingcheckin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tyut.studyroombookingcheckin.common.Constants;
import com.tyut.studyroombookingcheckin.entity.*;
import com.tyut.studyroombookingcheckin.mapper.*;
import com.tyut.studyroombookingcheckin.service.AdminService;
import com.tyut.studyroombookingcheckin.vo.BookingVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserMapper userMapper;
    private final BuildingMapper buildingMapper;
    private final RoomMapper roomMapper;
    private final SeatMapper seatMapper;
    private final BookingMapper bookingMapper;
    private final ViolationMapper violationMapper;
    private final StudyCheckinMapper checkinMapper;

    @Override
    public Map<String, Object> getDashboard() {
        Map<String, Object> result = new HashMap<>();
        result.put("userCount", userMapper.selectCount(null));
        result.put("buildingCount", buildingMapper.selectCount(null));
        result.put("roomCount", roomMapper.selectCount(null));
        result.put("seatCount", seatMapper.selectCount(null));

        LocalDate today = LocalDate.now();
        long todayBookings = bookingMapper.selectCount(new LambdaQueryWrapper<Booking>()
                .ge(Booking::getCreateTime, today.atStartOfDay()));
        result.put("todayBookings", todayBookings);

        // 全站今日学习时长：不按用户过滤，使用独立聚合 SQL（原 user_id = null 恒为 0）
        Integer todayMinutes = checkinMapper.sumAllDailyMinutes(today, today);
        result.put("todayTotalMinutes", todayMinutes != null ? todayMinutes : 0);

        Integer active = checkinMapper.countActiveUsersSince(today.atStartOfDay());
        result.put("activeUsers", active != null ? active : 0);

        Long pendingViolations = violationMapper.selectCount(new LambdaQueryWrapper<Violation>()
                .eq(Violation::getStatus, 0));
        result.put("pendingViolations", pendingViolations);

        return result;
    }

    @Override
    public Page<BookingVO> pageViolations(long current, long size, Integer type) {
        LambdaQueryWrapper<Booking> wrapper = new LambdaQueryWrapper<>();
        if (type != null) {
            wrapper.eq(Booking::getStatus, type);
        } else {
            wrapper.in(Booking::getStatus, Arrays.asList(
                    Constants.BOOKING_STATUS_EXPIRED,
                    Constants.BOOKING_STATUS_CANCELLED));
        }
        wrapper.orderByDesc(Booking::getCreateTime);
        Page<Booking> page = bookingMapper.selectPage(new Page<>(current, size), wrapper);
        Page<BookingVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(toBookingVOList(page.getRecords()));
        return voPage;
    }

    @Override
    public Map<String, Object> getRoomUtilization() {
        Map<String, Object> result = new HashMap<>();
        List<Room> rooms = roomMapper.selectList(null);
        // 一次分组统计所有房间的座位数/空闲数，避免逐房间查询
        Map<Long, Map<String, Object>> usageMap = new HashMap<>();
        for (Map<String, Object> row : roomMapper.selectSeatUsageGrouped()) {
            Object roomId = row.get("roomId");
            if (roomId != null) {
                usageMap.put(((Number) roomId).longValue(), row);
            }
        }

        List<Map<String, Object>> data = new ArrayList<>();
        for (Room r : rooms) {
            Map<String, Object> item = new HashMap<>();
            item.put("roomId", r.getId());
            item.put("roomName", r.getName());
            Map<String, Object> usage = usageMap.get(r.getId());
            int total = 0;
            int available = 0;
            if (usage != null) {
                total = usage.get("total") == null ? 0 : ((Number) usage.get("total")).intValue();
                available = usage.get("available") == null ? 0 : ((Number) usage.get("available")).intValue();
            }
            item.put("availableSeats", available);
            item.put("totalSeats", total);
            double rate = total > 0 ? Math.round((total - available) * 100.0 / total) / 100.0 : 0;
            item.put("utilizationRate", rate);
            data.add(item);
        }
        result.put("rooms", data);
        return result;
    }

    @Override
    public Map<String, Object> getUserStats() {
        Map<String, Object> result = new HashMap<>();
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        long totalUsers = userMapper.selectCount(null);
        result.put("totalUsers", totalUsers);

        Long weekActive = checkinMapper.selectList(new LambdaQueryWrapper<StudyCheckin>()
                .ge(StudyCheckin::getStartTime, weekStart.atStartOfDay())
                .select(StudyCheckin::getUserId)
                .groupBy(StudyCheckin::getUserId)).stream().count();
        result.put("weekActiveUsers", weekActive);

        Integer totalAll = checkinMapper.sumAllDailyMinutes(LocalDate.of(2020, 1, 1), today);
        result.put("totalStudyMinutes", totalAll != null ? totalAll : 0);

        return result;
    }

    private List<BookingVO> toBookingVOList(List<Booking> bookings) {
        List<BookingVO> result = new ArrayList<>();
        if (bookings == null || bookings.isEmpty()) {
            return result;
        }
        Set<Long> userIds = new HashSet<>();
        Set<Long> seatIds = new HashSet<>();
        Set<Long> roomIds = new HashSet<>();
        Set<Long> buildingIds = new HashSet<>();
        for (Booking b : bookings) {
            if (b.getUserId() != null) userIds.add(b.getUserId());
            if (b.getSeatId() != null) seatIds.add(b.getSeatId());
            if (b.getRoomId() != null) roomIds.add(b.getRoomId());
            if (b.getBuildingId() != null) buildingIds.add(b.getBuildingId());
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
        Map<Long, Room> roomMap = new HashMap<>();
        if (!roomIds.isEmpty()) {
            for (Room r : roomMapper.selectBatchIds(roomIds)) {
                roomMap.put(r.getId(), r);
            }
        }
        Map<Long, Building> buildingMap = new HashMap<>();
        if (!buildingIds.isEmpty()) {
            for (Building b : buildingMapper.selectBatchIds(buildingIds)) {
                buildingMap.put(b.getId(), b);
            }
        }

        for (Booking b : bookings) {
            BookingVO vo = new BookingVO();
            vo.setId(b.getId());
            vo.setUserId(b.getUserId());
            User u = userMap.get(b.getUserId());
            if (u != null) vo.setUserNickname(u.getNickname());
            vo.setSeatId(b.getSeatId());
            Seat seat = seatMap.get(b.getSeatId());
            if (seat != null) vo.setSeatNo(seat.getSeatNo());
            vo.setRoomId(b.getRoomId());
            Room room = roomMap.get(b.getRoomId());
            if (room != null) vo.setRoomName(room.getName());
            vo.setBuildingId(b.getBuildingId());
            Building building = buildingMap.get(b.getBuildingId());
            if (building != null) vo.setBuildingName(building.getName());
            vo.setBookingDate(b.getBookingDate());
            vo.setStartTime(b.getStartTime());
            vo.setEndTime(b.getEndTime());
            vo.setExpectedMinutes(b.getExpectedMinutes());
            vo.setActualMinutes(b.getActualMinutes());
            vo.setStatus(b.getStatus());
            vo.setCheckinCode(b.getCheckinCode());
            vo.setCheckinTime(b.getCheckinTime());
            vo.setCheckoutTime(b.getCheckoutTime());
            result.add(vo);
        }
        return result;
    }
}
