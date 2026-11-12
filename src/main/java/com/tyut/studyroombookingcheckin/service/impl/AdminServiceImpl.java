package com.tyut.studyroombookingcheckin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tyut.studyroombookingcheckin.common.Constants;
import com.tyut.studyroombookingcheckin.entity.*;
import com.tyut.studyroombookingcheckin.mapper.*;
import com.tyut.studyroombookingcheckin.service.AdminService;
import com.tyut.studyroombookingcheckin.vo.BookingVO;
import com.tyut.studyroombookingcheckin.vo.RoomVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

        Integer todayMinutes = checkinMapper.sumRangeMinutes(null, today, today);
        result.put("todayTotalMinutes", todayMinutes != null ? todayMinutes : 0);

        long activeUsers = checkinMapper.selectCount(new LambdaQueryWrapper<StudyCheckin>()
                .ge(StudyCheckin::getStartTime, today.atStartOfDay())
                .select(StudyCheckin::getUserId)
                .groupBy(StudyCheckin::getUserId));
        result.put("activeUsers", activeUsers);

        return result;
    }

    @Override
    public Page<BookingVO> pageViolations(long current, long size, Integer type) {
        LambdaQueryWrapper<Booking> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Booking::getStatus, Arrays.asList(
                Constants.BOOKING_STATUS_EXPIRED,
                Constants.BOOKING_STATUS_CANCELLED));
        wrapper.orderByDesc(Booking::getCreateTime);
        Page<Booking> page = bookingMapper.selectPage(new Page<>(current, size), wrapper);
        Page<BookingVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<BookingVO> records = new ArrayList<>();
        for (Booking b : page.getRecords()) {
            records.add(toBookingVO(b));
        }
        voPage.setRecords(records);
        return voPage;
    }

    @Override
    public Map<String, Object> getRoomUtilization() {
        Map<String, Object> result = new HashMap<>();
        List<Room> rooms = roomMapper.selectList(null);
        List<Map<String, Object>> data = new ArrayList<>();
        for (Room r : rooms) {
            Map<String, Object> item = new HashMap<>();
            item.put("roomId", r.getId());
            item.put("roomName", r.getName());
            Integer available = roomMapper.countAvailableSeats(r.getId());
            item.put("availableSeats", available);
            Integer active = roomMapper.countTotalActiveSeats(r.getId());
            item.put("totalSeats", active);
            double rate = active > 0 ? Math.round((active - available) * 100.0 / active) / 100.0 : 0;
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
        LocalDate weekStart = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));

        long totalUsers = userMapper.selectCount(null);
        result.put("totalUsers", totalUsers);

        Long weekActive = checkinMapper.selectList(new LambdaQueryWrapper<StudyCheckin>()
                .ge(StudyCheckin::getStartTime, weekStart.atStartOfDay())
                .select(StudyCheckin::getUserId)
                .groupBy(StudyCheckin::getUserId)).stream().count();
        result.put("weekActiveUsers", weekActive);

        Integer totalAll = checkinMapper.sumRangeMinutes(null, LocalDate.of(2020, 1, 1), today);
        result.put("totalStudyMinutes", totalAll != null ? totalAll : 0);

        return result;
    }

    private BookingVO toBookingVO(Booking b) {
        BookingVO vo = new BookingVO();
        vo.setId(b.getId());
        vo.setUserId(b.getUserId());
        User u = userMapper.selectById(b.getUserId());
        if (u != null) vo.setUserNickname(u.getNickname());
        vo.setSeatId(b.getSeatId());
        Seat seat = seatMapper.selectById(b.getSeatId());
        if (seat != null) vo.setSeatNo(seat.getSeatNo());
        vo.setRoomId(b.getRoomId());
        Room room = roomMapper.selectById(b.getRoomId());
        if (room != null) vo.setRoomName(room.getName());
        vo.setBuildingId(b.getBuildingId());
        Building building = buildingMapper.selectById(b.getBuildingId());
        if (building != null) vo.setBuildingName(building.getName());
        vo.setBookingDate(b.getBookingDate());
        vo.setStartTime(b.getStartTime());
        vo.setEndTime(b.getEndTime());
        vo.setExpectedMinutes(b.getExpectedMinutes());
        vo.setActualMinutes(b.getActualMinutes());
        vo.setStatus(b.getStatus());
        vo.setCheckinTime(b.getCheckinTime());
        vo.setCheckoutTime(b.getCheckoutTime());
        return vo;
    }
}