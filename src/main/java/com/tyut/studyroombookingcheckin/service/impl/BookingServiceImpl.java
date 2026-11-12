package com.tyut.studyroombookingcheckin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tyut.studyroombookingcheckin.cache.RankingCacheService;
import com.tyut.studyroombookingcheckin.cache.RedisLock;
import com.tyut.studyroombookingcheckin.cache.SeatCacheService;
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
import com.tyut.studyroombookingcheckin.service.StudyCheckinService;
import com.tyut.studyroombookingcheckin.service.ViolationService;
import com.tyut.studyroombookingcheckin.vo.BookingVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private static final String LOCK_AUTO_RELEASE_EXPIRED = "lock:job:booking:expired";
    private static final String LOCK_AUTO_RELEASE_TIMEOUT = "lock:job:booking:timeout";

    private final BookingMapper bookingMapper;
    private final SeatMapper seatMapper;
    private final RoomMapper roomMapper;
    private final BuildingMapper buildingMapper;
    private final UserMapper userMapper;
    private final StudyCheckinService studyCheckinService;
    private final ViolationService violationService;
    private final SeatCacheService seatCacheService;
    private final RankingCacheService rankingCacheService;
    private final RedisLock redisLock;

    @Override
    @Transactional
    public BookingVO create(Long userId, BookingCreateDTO dto) {
        if (dto.getSeatId() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "座位ID不能为空");
        }

        // 悲观锁：同一座位的并发预约串行执行，从根源上杜绝重复预订
        Seat seat = seatMapper.selectByIdForUpdate(dto.getSeatId());
        if (seat == null) {
            throw new BusinessException(ErrorCode.SEAT_NOT_EXIST);
        }
        if (Constants.SEAT_STATUS_MAINTENANCE.equals(seat.getStatus())) {
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
        if (!endTime.isAfter(startTime)) {
            throw new BusinessException(ErrorCode.INVALID_TIME_RANGE);
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
        booking.setCheckinCode(generateCheckinCode());
        booking.setRemark(dto.getRemark());
        booking.setCreateTime(LocalDateTime.now());
        booking.setUpdateTime(LocalDateTime.now());
        bookingMapper.insert(booking);

        seatMapper.updateSeatState(seat.getId(), Constants.SEAT_STATUS_RESERVED, null, null, LocalDateTime.now());
        seat.setStatus(Constants.SEAT_STATUS_RESERVED);
        // 与数据库保持一致，避免把旧的占用人信息同步进缓存
        seat.setOccupiedUserId(null);
        seat.setOccupiedTime(null);
        seatCacheService.syncSeat(seat);

        return toVO(booking);
    }

    @Override
    @Transactional
    public BookingVO checkin(Long bookingId, Long userId) {
        Booking booking = bookingMapper.selectById(bookingId);
        if (booking == null || booking.getUserId() == null || !booking.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.BOOKING_NOT_EXIST);
        }
        if (!Constants.BOOKING_STATUS_PENDING.equals(booking.getStatus())) {
            throw new BusinessException(ErrorCode.ALREADY_CHECKED_IN);
        }
        LocalDateTime now = LocalDateTime.now();
        // 统一规则：开始时间 + 超时时长后不可签到
        LocalDateTime deadline = booking.getStartTime().plusMinutes(Constants.DEFAULT_TIMEOUT_MINUTES);
        if (now.isAfter(deadline)) {
            throw new BusinessException(ErrorCode.CHECKIN_EXPIRED);
        }

        booking.setStatus(Constants.BOOKING_STATUS_CONFIRMED);
        booking.setCheckinTime(now);
        booking.setUpdateTime(now);
        bookingMapper.updateById(booking);

        seatMapper.updateSeatState(booking.getSeatId(), Constants.SEAT_STATUS_OCCUPIED,
                userId, now, now);
        Seat seat = seatMapper.selectById(booking.getSeatId());
        seatCacheService.syncSeat(seat);

        studyCheckinService.startCheckin(userId, booking.getSeatId(), bookingId);
        return toVO(booking);
    }

    @Override
    @Transactional
    public BookingVO checkinByCode(String checkinCode, Long userId) {
        if (checkinCode == null || !checkinCode.matches("\\d{8}")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "请输入 8 位数字签到码");
        }
        Booking booking = bookingMapper.selectOne(new LambdaQueryWrapper<Booking>()
                .eq(Booking::getCheckinCode, checkinCode)
                .eq(Booking::getUserId, userId)
                .last("LIMIT 1"));
        if (booking == null) {
            throw new BusinessException("签到码无效");
        }
        return checkin(booking.getId(), userId);
    }

    @Override
    @Transactional
    public BookingVO checkout(Long bookingId, Long userId) {
        Booking booking = bookingMapper.selectById(bookingId);
        if (booking == null || booking.getUserId() == null || !booking.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.BOOKING_NOT_EXIST);
        }
        if (!Constants.BOOKING_STATUS_CONFIRMED.equals(booking.getStatus())) {
            throw new BusinessException(ErrorCode.NOT_CHECKED_IN);
        }

        LocalDateTime now = LocalDateTime.now();
        booking.setStatus(Constants.BOOKING_STATUS_COMPLETED);
        booking.setCheckoutTime(now);
        booking.setActualMinutes(booking.getCheckinTime() == null ? 0
                : (int) ChronoUnit.MINUTES.between(booking.getCheckinTime(), now));
        booking.setUpdateTime(now);
        bookingMapper.updateById(booking);

        releaseSeat(booking.getSeatId(), now);
        studyCheckinService.endByBooking(bookingId);
        rankingCacheService.evictAll();
        return toVO(booking);
    }

    @Override
    @Transactional
    public BookingVO cancel(Long bookingId, Long userId) {
        Booking booking = bookingMapper.selectById(bookingId);
        if (booking == null || booking.getUserId() == null || !booking.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.BOOKING_NOT_EXIST);
        }
        if (Constants.BOOKING_STATUS_COMPLETED.equals(booking.getStatus())) {
            throw new BusinessException("预约已完成，无法取消");
        }
        if (Constants.BOOKING_STATUS_CANCELLED.equals(booking.getStatus())) {
            return toVO(booking);
        }

        LocalDateTime now = LocalDateTime.now();
        boolean wasCheckedIn = Constants.BOOKING_STATUS_CONFIRMED.equals(booking.getStatus());
        booking.setStatus(Constants.BOOKING_STATUS_CANCELLED);
        booking.setUpdateTime(now);
        if (wasCheckedIn) {
            booking.setActualMinutes(booking.getCheckinTime() == null ? 0
                    : (int) ChronoUnit.MINUTES.between(booking.getCheckinTime(), now));
        }
        bookingMapper.updateById(booking);

        if (wasCheckedIn) {
            studyCheckinService.endByBooking(bookingId);
            rankingCacheService.evictAll();
        }
        releaseSeat(booking.getSeatId(), now);
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
    @Scheduled(cron = "0 */5 * * * ?")
    @Transactional
    public void autoReleaseExpired() {
        String lockKey = LOCK_AUTO_RELEASE_EXPIRED;
        if (!redisLock.tryLock(lockKey, Duration.ofMinutes(4))) {
            return;
        }
        try {
            LocalDateTime now = LocalDateTime.now();
            // 规则：开始时间 + 30 分钟仍未签到 → 过期释放
            LocalDateTime deadline = now.minusMinutes(Constants.DEFAULT_TIMEOUT_MINUTES);
            List<Booking> expired = bookingMapper.selectList(new LambdaQueryWrapper<Booking>()
                    .eq(Booking::getStatus, Constants.BOOKING_STATUS_PENDING)
                    .lt(Booking::getStartTime, deadline));
            for (Booking b : expired) {
                b.setStatus(Constants.BOOKING_STATUS_EXPIRED);
                b.setUpdateTime(now);
                bookingMapper.updateById(b);
                releaseSeatIfReserved(b.getSeatId(), now);
                violationService.record(b.getUserId(), b.getId(), b.getSeatId(),
                        ViolationServiceImpl.TYPE_NO_SHOW,
                        "预约开始后 " + Constants.DEFAULT_TIMEOUT_MINUTES + " 分钟内未签到，系统自动释放座位", 5);
            }
            if (!expired.isEmpty()) {
                log.info("自动释放超时未签到预约 {} 条", expired.size());
            }
        } finally {
            redisLock.unlock(lockKey);
        }
    }

    @Override
    @Scheduled(cron = "0 */10 * * * ?")
    @Transactional
    public void autoReleaseTimeout() {
        String lockKey = LOCK_AUTO_RELEASE_TIMEOUT;
        if (!redisLock.tryLock(lockKey, Duration.ofMinutes(9))) {
            return;
        }
        try {
            LocalDateTime now = LocalDateTime.now();
            List<Booking> overtime = bookingMapper.selectList(new LambdaQueryWrapper<Booking>()
                    .eq(Booking::getStatus, Constants.BOOKING_STATUS_CONFIRMED)
                    .lt(Booking::getEndTime, now));
            for (Booking b : overtime) {
                b.setStatus(Constants.BOOKING_STATUS_COMPLETED);
                b.setCheckoutTime(now);
                if (b.getCheckinTime() != null) {
                    b.setActualMinutes((int) ChronoUnit.MINUTES.between(b.getCheckinTime(), now));
                }
                b.setUpdateTime(now);
                bookingMapper.updateById(b);

                releaseSeat(b.getSeatId(), now);
                studyCheckinService.endByBooking(b.getId());
                violationService.record(b.getUserId(), b.getId(), b.getSeatId(),
                        ViolationServiceImpl.TYPE_OVERTIME,
                        "超过预约结束时间仍未离座，系统自动结束并释放座位", 3);
            }
            if (!overtime.isEmpty()) {
                rankingCacheService.evictAll();
                log.info("自动结束超时占座预约 {} 条", overtime.size());
            }
        } finally {
            redisLock.unlock(lockKey);
        }
    }

    private void releaseSeat(Long seatId, LocalDateTime now) {
        Seat seat = seatMapper.selectById(seatId);
        if (seat == null) {
            return;
        }
        seatMapper.updateSeatState(seatId, Constants.SEAT_STATUS_FREE, null, null, now);
        seat.setStatus(Constants.SEAT_STATUS_FREE);
        seat.setOccupiedUserId(null);
        seat.setOccupiedTime(null);
        seatCacheService.syncSeat(seat);
    }

    private void releaseSeatIfReserved(Long seatId, LocalDateTime now) {
        Seat seat = seatMapper.selectById(seatId);
        if (seat == null || !Constants.SEAT_STATUS_RESERVED.equals(seat.getStatus())) {
            return;
        }
        seatMapper.updateSeatState(seatId, Constants.SEAT_STATUS_FREE, null, null, now);
        seat.setStatus(Constants.SEAT_STATUS_FREE);
        seatCacheService.syncSeat(seat);
    }

    private String generateCheckinCode() {
        return String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
    }

    private Page<BookingVO> convertPage(Page<Booking> page) {
        Page<BookingVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(toVOList(page.getRecords()));
        return voPage;
    }

    /**
     * 批量转换，一次性加载用户/座位/房间/楼栋，消除 N+1 查询。
     */
    private List<BookingVO> toVOList(List<Booking> bookings) {
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
        Map<Long, User> userMap = batchUsers(userIds);
        Map<Long, Seat> seatMap = batchSeats(seatIds);
        Map<Long, Room> roomMap = batchRooms(roomIds);
        Map<Long, Building> buildingMap = batchBuildings(buildingIds);

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
            vo.setRemark(b.getRemark());
            result.add(vo);
        }
        return result;
    }

    private BookingVO toVO(Booking b) {
        return toVOList(List.of(b)).get(0);
    }

    private Map<Long, User> batchUsers(Set<Long> ids) {
        Map<Long, User> map = new HashMap<>();
        if (!ids.isEmpty()) {
            for (User u : userMapper.selectBatchIds(ids)) {
                map.put(u.getId(), u);
            }
        }
        return map;
    }

    private Map<Long, Seat> batchSeats(Set<Long> ids) {
        Map<Long, Seat> map = new HashMap<>();
        if (!ids.isEmpty()) {
            for (Seat s : seatMapper.selectBatchIds(ids)) {
                map.put(s.getId(), s);
            }
        }
        return map;
    }

    private Map<Long, Room> batchRooms(Set<Long> ids) {
        Map<Long, Room> map = new HashMap<>();
        if (!ids.isEmpty()) {
            for (Room r : roomMapper.selectBatchIds(ids)) {
                map.put(r.getId(), r);
            }
        }
        return map;
    }

    private Map<Long, Building> batchBuildings(Set<Long> ids) {
        Map<Long, Building> map = new HashMap<>();
        if (!ids.isEmpty()) {
            for (Building b : buildingMapper.selectBatchIds(ids)) {
                map.put(b.getId(), b);
            }
        }
        return map;
    }
}
