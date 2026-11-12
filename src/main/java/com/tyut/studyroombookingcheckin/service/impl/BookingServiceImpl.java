package com.tyut.studyroombookingcheckin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tyut.studyroombookingcheckin.common.BusinessException;
import com.tyut.studyroombookingcheckin.common.Constants;
import com.tyut.studyroombookingcheckin.common.ErrorCode;
import com.tyut.studyroombookingcheckin.dto.BookingCreateDTO;
import com.tyut.studyroombookingcheckin.entity.Booking;
import com.tyut.studyroombookingcheckin.entity.Building;
import com.tyut.studyroombookingcheckin.entity.Room;
import com.tyut.studyroombookingcheckin.entity.Seat;
import com.tyut.studyroombookingcheckin.entity.User;
import com.tyut.studyroombookingcheckin.mapper.BookingMapper;
import com.tyut.studyroombookingcheckin.mapper.BuildingMapper;
import com.tyut.studyroombookingcheckin.mapper.RoomMapper;
import com.tyut.studyroombookingcheckin.mapper.SeatMapper;
import com.tyut.studyroombookingcheckin.mapper.UserMapper;
import com.tyut.studyroombookingcheckin.service.BookingService;
import com.tyut.studyroombookingcheckin.service.SeatService;
import com.tyut.studyroombookingcheckin.service.StudyCheckinService;
import com.tyut.studyroombookingcheckin.vo.BookingVO;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingMapper bookingMapper;
    private final SeatMapper seatMapper;
    private final RoomMapper roomMapper;
    private final BuildingMapper buildingMapper;
    private final UserMapper userMapper;
    private final SeatService seatService;
    private final StudyCheckinService studyCheckinService;

    @Override
    @Transactional
    public BookingVO create(Long userId, BookingCreateDTO dto) {
        Seat seat = seatMapper.selectById(dto.getSeatId());
        if (seat == null) {
            throw new BusinessException(ErrorCode.SEAT_NOT_EXIST);
        }
        if (seat.getStatus() == Constants.SEAT_STATUS_MAINTENANCE) {
            throw new BusinessException("座位正在维护中，无法预约");
        }

        LocalDateTime startTime = dto.getStartTime() != null ? dto.getStartTime() : LocalDateTime.now();
        LocalDateTime endTime;
        if (dto.getEndTime() != null) {
            endTime = dto.getEndTime();
        } else if (dto.getExpectedMinutes() != null) {
            endTime = startTime.plusMinutes(dto.getExpectedMinutes());
        } else {
            endTime = startTime.plusMinutes(120);
        }

        Integer conflict = bookingMapper.countSeatConflictBookings(seat.getId(), startTime, endTime);
        if (conflict != null && conflict > 0) {
            throw new BusinessException(ErrorCode.SEAT_OCCUPIED);
        }

        Booking booking = new Booking();
        booking.setUserId(userId);
        booking.setSeatId(seat.getId());
        booking.setRoomId(seat.getRoomId());
        Room room = roomMapper.selectById(seat.getRoomId());
        if (room != null) {
            booking.setBuildingId(room.getBuildingId());
        }
        booking.setBookingDate(startTime.toLocalDate());
        booking.setStartTime(startTime);
        booking.setEndTime(endTime);
        booking.setExpectedMinutes((int) ChronoUnit.MINUTES.between(startTime, endTime));
        booking.setStatus(Constants.BOOKING_STATUS_PENDING);
        booking.setCheckinCode(UUID.randomUUID().toString().substring(0, 8));
        booking.setRemark(dto.getRemark());
        booking.setCreateTime(LocalDateTime.now());
        booking.setUpdateTime(LocalDateTime.now());
        bookingMapper.insert(booking);

        seat.setStatus(Constants.SEAT_STATUS_RESERVED);
        seat.setUpdateTime(LocalDateTime.now());
        seatMapper.updateById(seat);

        return toVO(booking);
    }

    @Override
    @Transactional
    public BookingVO checkin(Long bookingId, Long userId) {
        Booking booking = bookingMapper.selectById(bookingId);
        if (booking == null || booking.getUserId() == null || !booking.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.BOOKING_NOT_EXIST);
        }
        if (booking.getStatus() != Constants.BOOKING_STATUS_PENDING) {
            throw new BusinessException(ErrorCode.ALREADY_CHECKED_IN);
        }
        if (booking.getStartTime().isBefore(LocalDateTime.now().minusMinutes(30))) {
            throw new BusinessException(ErrorCode.CHECKIN_EXPIRED);
        }

        booking.setStatus(Constants.BOOKING_STATUS_CONFIRMED);
        booking.setCheckinTime(LocalDateTime.now());
        booking.setUpdateTime(LocalDateTime.now());
        bookingMapper.updateById(booking);

        Seat seat = seatMapper.selectById(booking.getSeatId());
        if (seat != null) {
            seat.setStatus(Constants.SEAT_STATUS_OCCUPIED);
            seat.setOccupiedUserId(userId);
            seat.setOccupiedTime(LocalDateTime.now());
            seat.setUpdateTime(LocalDateTime.now());
            seatMapper.updateById(seat);
        }

        studyCheckinService.startCheckin(userId, booking.getSeatId(), bookingId);
        return toVO(booking);
    }

    @Override
    @Transactional
    public BookingVO checkout(Long bookingId, Long userId) {
        Booking booking = bookingMapper.selectById(bookingId);
        if (booking == null || booking.getUserId() == null || !booking.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.BOOKING_NOT_EXIST);
        }
        if (booking.getStatus() != Constants.BOOKING_STATUS_CONFIRMED) {
            throw new BusinessException(ErrorCode.NOT_CHECKED_IN);
        }

        booking.setStatus(Constants.BOOKING_STATUS_COMPLETED);
        booking.setCheckoutTime(LocalDateTime.now());
        booking.setActualMinutes((int) ChronoUnit.MINUTES.between(booking.getCheckinTime(), LocalDateTime.now()));
        booking.setUpdateTime(LocalDateTime.now());
        bookingMapper.updateById(booking);

        Seat seat = seatMapper.selectById(booking.getSeatId());
        if (seat != null) {
            seat.setStatus(Constants.SEAT_STATUS_FREE);
            seat.setOccupiedUserId(null);
            seat.setOccupiedTime(null);
            seat.setUpdateTime(LocalDateTime.now());
            seatMapper.updateById(seat);
        }
        studyCheckinService.endByBooking(bookingId);
        return toVO(booking);
    }

    @Override
    @Transactional
    public BookingVO cancel(Long bookingId, Long userId) {
        Booking booking = bookingMapper.selectById(bookingId);
        if (booking == null || booking.getUserId() == null || !booking.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.BOOKING_NOT_EXIST);
        }
        if (booking.getStatus() == Constants.BOOKING_STATUS_COMPLETED) {
            throw new BusinessException("预约已完成，无法取消");
        }
        booking.setStatus(Constants.BOOKING_STATUS_CANCELLED);
        booking.setUpdateTime(LocalDateTime.now());
        bookingMapper.updateById(booking);

        Seat seat = seatMapper.selectById(booking.getSeatId());
        if (seat != null && seat.getStatus() == Constants.SEAT_STATUS_RESERVED) {
            seat.setStatus(Constants.SEAT_STATUS_FREE);
            seat.setUpdateTime(LocalDateTime.now());
            seatMapper.updateById(seat);
        }
        return toVO(booking);
    }

    @Override
    public Page<BookingVO> pageByUser(Long userId, long current, long size, Integer status) {
        LambdaQueryWrapper<Booking> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Booking::getUserId, userId);
        if (status != null) {
            wrapper.eq(Booking::getStatus, status);
        }
        wrapper.orderByDesc(Booking::getCreateTime);
        Page<Booking> page = bookingMapper.selectPage(new Page<>(current, size), wrapper);
        return convertPage(page);
    }

    @Override
    public Page<BookingVO> pageAdmin(long current, long size, Integer status, Long roomId) {
        LambdaQueryWrapper<Booking> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Booking::getStatus, status);
        }
        if (roomId != null) {
            wrapper.eq(Booking::getRoomId, roomId);
        }
        wrapper.orderByDesc(Booking::getCreateTime);
        Page<Booking> page = bookingMapper.selectPage(new Page<>(current, size), wrapper);
        return convertPage(page);
    }

    @Override
    public BookingVO getById(Long id) {
        Booking b = bookingMapper.selectById(id);
        if (b == null) {
            throw new BusinessException(ErrorCode.BOOKING_NOT_EXIST);
        }
        return toVO(b);
    }

    @Override
    @Scheduled(cron = "0 */10 * * * ?")
    @Transactional
    public void autoReleaseExpired() {
        LocalDateTime now = LocalDateTime.now();
        List<Booking> expired = bookingMapper.selectList(new LambdaQueryWrapper<Booking>()
                .eq(Booking::getStatus, Constants.BOOKING_STATUS_PENDING)
                .lt(Booking::getEndTime, now));
        for (Booking b : expired) {
            b.setStatus(Constants.BOOKING_STATUS_EXPIRED);
            b.setUpdateTime(now);
            bookingMapper.updateById(b);
            Seat seat = seatMapper.selectById(b.getSeatId());
            if (seat != null && seat.getStatus() == Constants.SEAT_STATUS_RESERVED) {
                seat.setStatus(Constants.SEAT_STATUS_FREE);
                seat.setUpdateTime(now);
                seatMapper.updateById(seat);
            }
        }
    }

    @Override
    @Scheduled(cron = "0 */15 * * * ?")
    @Transactional
    public void autoReleaseTimeout() {
        LocalDateTime now = LocalDateTime.now();
        List<Booking> confirmed = bookingMapper.selectList(new LambdaQueryWrapper<Booking>()
                .eq(Booking::getStatus, Constants.BOOKING_STATUS_CONFIRMED)
                .lt(Booking::getEndTime, now));
        for (Booking b : confirmed) {
            b.setStatus(Constants.BOOKING_STATUS_COMPLETED);
            b.setCheckoutTime(now);
            if (b.getCheckinTime() != null) {
                b.setActualMinutes((int) ChronoUnit.MINUTES.between(b.getCheckinTime(), now));
            }
            b.setUpdateTime(now);
            bookingMapper.updateById(b);

            Seat seat = seatMapper.selectById(b.getSeatId());
            if (seat != null) {
                seat.setStatus(Constants.SEAT_STATUS_FREE);
                seat.setOccupiedUserId(null);
                seat.setOccupiedTime(null);
                seat.setUpdateTime(now);
                seatMapper.updateById(seat);
            }
        }
    }

    private Page<BookingVO> convertPage(Page<Booking> page) {
        Page<BookingVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<BookingVO> records = new ArrayList<>();
        for (Booking b : page.getRecords()) {
            records.add(toVO(b));
        }
        voPage.setRecords(records);
        return voPage;
    }

    private BookingVO toVO(Booking b) {
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
        vo.setCheckinCode(b.getCheckinCode());
        vo.setCheckinTime(b.getCheckinTime());
        vo.setCheckoutTime(b.getCheckoutTime());
        vo.setRemark(b.getRemark());
        return vo;
    }
}