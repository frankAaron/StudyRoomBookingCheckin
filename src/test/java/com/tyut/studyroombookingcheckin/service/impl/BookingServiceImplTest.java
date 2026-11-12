package com.tyut.studyroombookingcheckin.service.impl;

import com.tyut.studyroombookingcheckin.cache.RankingCacheService;
import com.tyut.studyroombookingcheckin.cache.RedisLock;
import com.tyut.studyroombookingcheckin.cache.SeatCacheService;
import com.tyut.studyroombookingcheckin.common.BusinessException;
import com.tyut.studyroombookingcheckin.common.Constants;
import com.tyut.studyroombookingcheckin.common.ErrorCode;
import com.tyut.studyroombookingcheckin.dto.BookingCreateDTO;
import com.tyut.studyroombookingcheckin.entity.Booking;
import com.tyut.studyroombookingcheckin.entity.Room;
import com.tyut.studyroombookingcheckin.entity.Seat;
import com.tyut.studyroombookingcheckin.mapper.BookingMapper;
import com.tyut.studyroombookingcheckin.mapper.BuildingMapper;
import com.tyut.studyroombookingcheckin.mapper.RoomMapper;
import com.tyut.studyroombookingcheckin.mapper.SeatMapper;
import com.tyut.studyroombookingcheckin.mapper.UserMapper;
import com.tyut.studyroombookingcheckin.service.StudyCheckinService;
import com.tyut.studyroombookingcheckin.service.ViolationService;
import com.tyut.studyroombookingcheckin.vo.BookingVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 预约核心流程单元测试：冲突检测、超时规则、签到/离座、定时释放、并发安全。
 */
class BookingServiceImplTest {

    private BookingMapper bookingMapper;
    private SeatMapper seatMapper;
    private RoomMapper roomMapper;
    private BuildingMapper buildingMapper;
    private UserMapper userMapper;
    private StudyCheckinService studyCheckinService;
    private ViolationService violationService;
    private SeatCacheService seatCacheService;
    private RankingCacheService rankingCacheService;
    private RedisLock redisLock;
    private BookingServiceImpl service;

    @BeforeEach
    void setUp() {
        bookingMapper = mock(BookingMapper.class);
        seatMapper = mock(SeatMapper.class);
        roomMapper = mock(RoomMapper.class);
        buildingMapper = mock(BuildingMapper.class);
        userMapper = mock(UserMapper.class);
        studyCheckinService = mock(StudyCheckinService.class);
        violationService = mock(ViolationService.class);
        seatCacheService = mock(SeatCacheService.class);
        rankingCacheService = mock(RankingCacheService.class);
        redisLock = mock(RedisLock.class);
        service = new BookingServiceImpl(bookingMapper, seatMapper, roomMapper, buildingMapper, userMapper,
                studyCheckinService, violationService, seatCacheService, rankingCacheService, redisLock);
    }

    private Seat seat(long id, long roomId, int status) {
        Seat seat = new Seat();
        seat.setId(id);
        seat.setRoomId(roomId);
        seat.setSeatNo("A" + id);
        seat.setStatus(status);
        return seat;
    }

    private Room room(long id, long buildingId) {
        Room room = new Room();
        room.setId(id);
        room.setBuildingId(buildingId);
        room.setName("自习室" + id);
        return room;
    }

    @Test
    @DisplayName("创建预约成功：状态为待签到、生成 8 位签到码、座位置为已预约并同步缓存")
    void createSuccess() {
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        Seat seat = seat(10L, 1L, Constants.SEAT_STATUS_FREE);
        when(seatMapper.selectByIdForUpdate(10L)).thenReturn(seat);
        when(roomMapper.selectById(1L)).thenReturn(room(1L, 2L));
        when(bookingMapper.countSeatConflictBookings(eq(10L), any(), any())).thenReturn(0);
        when(bookingMapper.insert(any(Booking.class))).thenReturn(1);

        BookingCreateDTO dto = new BookingCreateDTO();
        dto.setSeatId(10L);
        dto.setStartTime(start);
        dto.setExpectedMinutes(60);

        BookingVO vo = service.create(2L, dto);

        assertThat(vo.getStatus()).isEqualTo(Constants.BOOKING_STATUS_PENDING);
        assertThat(vo.getCheckinCode()).matches("\\d{8}");
        assertThat(vo.getEndTime()).isEqualTo(start.plusMinutes(60));
        verify(seatMapper).updateSeatState(eq(10L), eq(Constants.SEAT_STATUS_RESERVED), any(), any(), any());
        verify(seatCacheService).syncSeat(any(Seat.class));
    }

    @Test
    @DisplayName("座位存在时段冲突时拒绝创建预约，不写入数据库")
    void createConflictRejected() {
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        when(seatMapper.selectByIdForUpdate(10L)).thenReturn(seat(10L, 1L, Constants.SEAT_STATUS_FREE));
        when(bookingMapper.countSeatConflictBookings(eq(10L), any(), any())).thenReturn(1);

        BookingCreateDTO dto = new BookingCreateDTO();
        dto.setSeatId(10L);
        dto.setStartTime(start);
        dto.setExpectedMinutes(60);

        assertThatThrownBy(() -> service.create(2L, dto))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ErrorCode.SEAT_OCCUPIED.getCode());
        verify(bookingMapper, never()).insert(any(Booking.class));
    }

    @Test
    @DisplayName("结束时间不晚于开始时间时抛出无效时间范围")
    void createInvalidTimeRange() {
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        when(seatMapper.selectByIdForUpdate(10L)).thenReturn(seat(10L, 1L, Constants.SEAT_STATUS_FREE));

        BookingCreateDTO dto = new BookingCreateDTO();
        dto.setSeatId(10L);
        dto.setStartTime(start);
        dto.setEndTime(start);

        assertThatThrownBy(() -> service.create(2L, dto))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ErrorCode.INVALID_TIME_RANGE.getCode());
    }

    @Test
    @DisplayName("超过 开始时间 + DEFAULT_TIMEOUT_MINUTES 后签到失败")
    void checkinExpiredByUnifiedTimeoutRule() {
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setUserId(2L);
        booking.setSeatId(10L);
        booking.setStatus(Constants.BOOKING_STATUS_PENDING);
        booking.setStartTime(LocalDateTime.now().minusMinutes(Constants.DEFAULT_TIMEOUT_MINUTES + 5));
        when(bookingMapper.selectById(1L)).thenReturn(booking);

        assertThatThrownBy(() -> service.checkin(1L, 2L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ErrorCode.CHECKIN_EXPIRED.getCode());
        verify(seatMapper, never()).updateSeatState(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("超时时长内签到成功，座位置为占用并开始学习打卡")
    void checkinWithinTimeout() {
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setUserId(2L);
        booking.setSeatId(10L);
        booking.setStatus(Constants.BOOKING_STATUS_PENDING);
        booking.setStartTime(LocalDateTime.now().minusMinutes(5));
        when(bookingMapper.selectById(1L)).thenReturn(booking);
        when(seatMapper.selectById(10L)).thenReturn(seat(10L, 1L, Constants.SEAT_STATUS_OCCUPIED));

        BookingVO vo = service.checkin(1L, 2L);

        assertThat(vo.getStatus()).isEqualTo(Constants.BOOKING_STATUS_CONFIRMED);
        verify(seatMapper).updateSeatState(eq(10L), eq(Constants.SEAT_STATUS_OCCUPIED), eq(2L), any(), any());
        verify(studyCheckinService).startCheckin(2L, 10L, 1L);
    }

    @Test
    @DisplayName("非本人预约不能签到")
    void checkinRejectsOtherUser() {
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setUserId(9L);
        booking.setStatus(Constants.BOOKING_STATUS_PENDING);
        when(bookingMapper.selectById(1L)).thenReturn(booking);

        assertThatThrownBy(() -> service.checkin(1L, 2L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ErrorCode.BOOKING_NOT_EXIST.getCode());
    }

    @Test
    @DisplayName("签离：计算实际学习时长、释放座位并清理排行榜缓存")
    void checkoutReleasesSeat() {
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setUserId(2L);
        booking.setSeatId(10L);
        booking.setStatus(Constants.BOOKING_STATUS_CONFIRMED);
        booking.setCheckinTime(LocalDateTime.now().minusMinutes(45));
        when(bookingMapper.selectById(1L)).thenReturn(booking);
        when(seatMapper.selectById(10L)).thenReturn(seat(10L, 1L, Constants.SEAT_STATUS_OCCUPIED));

        BookingVO vo = service.checkout(1L, 2L);

        assertThat(vo.getStatus()).isEqualTo(Constants.BOOKING_STATUS_COMPLETED);
        assertThat(vo.getActualMinutes()).isBetween(44, 46);
        verify(seatMapper).updateSeatState(eq(10L), eq(Constants.SEAT_STATUS_FREE), any(), any(), any());
        verify(studyCheckinService).endByBooking(1L);
        verify(rankingCacheService).evictAll();
    }

    @Test
    @DisplayName("已签到状态取消预约时同步结束学习打卡")
    void cancelAfterCheckinEndsStudySession() {
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setUserId(2L);
        booking.setSeatId(10L);
        booking.setStatus(Constants.BOOKING_STATUS_CONFIRMED);
        booking.setCheckinTime(LocalDateTime.now().minusMinutes(20));
        when(bookingMapper.selectById(1L)).thenReturn(booking);
        when(seatMapper.selectById(10L)).thenReturn(seat(10L, 1L, Constants.SEAT_STATUS_OCCUPIED));

        BookingVO vo = service.cancel(1L, 2L);

        assertThat(vo.getStatus()).isEqualTo(Constants.BOOKING_STATUS_CANCELLED);
        verify(studyCheckinService, times(1)).endByBooking(1L);
    }

    @Test
    @DisplayName("签到码格式非法时提示 8 位数字签到码")
    void checkinByCodeRejectsInvalidFormat() {
        assertThatThrownBy(() -> service.checkinByCode("123", 2L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ErrorCode.BAD_REQUEST.getCode());
    }

    @Test
    @DisplayName("未签到预约按 开始时间+超时时长 自动过期并写入爽约违规记录")
    void autoReleaseExpiredWritesViolation() {
        Booking booking = new Booking();
        booking.setId(5L);
        booking.setUserId(2L);
        booking.setSeatId(10L);
        booking.setStatus(Constants.BOOKING_STATUS_PENDING);
        booking.setStartTime(LocalDateTime.now().minusMinutes(Constants.DEFAULT_TIMEOUT_MINUTES + 10));
        when(redisLock.tryLock(any(), any(Duration.class))).thenReturn(true);
        when(bookingMapper.selectList(any())).thenReturn(new ArrayList<>(List.of(booking)));
        when(seatMapper.selectById(10L)).thenReturn(seat(10L, 1L, Constants.SEAT_STATUS_RESERVED));

        service.autoReleaseExpired();

        assertThat(booking.getStatus()).isEqualTo(Constants.BOOKING_STATUS_EXPIRED);
        verify(violationService).record(eq(2L), eq(5L), eq(10L),
                eq(ViolationServiceImpl.TYPE_NO_SHOW), any(), eq(5));
        verify(redisLock).unlock(any());
    }

    @Test
    @DisplayName("超时占座自动结束并写入超时违规记录")
    void autoReleaseTimeoutWritesViolation() {
        Booking booking = new Booking();
        booking.setId(6L);
        booking.setUserId(2L);
        booking.setSeatId(10L);
        booking.setStatus(Constants.BOOKING_STATUS_CONFIRMED);
        booking.setCheckinTime(LocalDateTime.now().minusHours(3));
        booking.setEndTime(LocalDateTime.now().minusMinutes(10));
        when(redisLock.tryLock(any(), any(Duration.class))).thenReturn(true);
        when(bookingMapper.selectList(any())).thenReturn(new ArrayList<>(List.of(booking)));
        when(seatMapper.selectById(10L)).thenReturn(seat(10L, 1L, Constants.SEAT_STATUS_OCCUPIED));

        service.autoReleaseTimeout();

        assertThat(booking.getStatus()).isEqualTo(Constants.BOOKING_STATUS_COMPLETED);
        verify(violationService).record(eq(2L), eq(6L), eq(10L),
                eq(ViolationServiceImpl.TYPE_OVERTIME), any(), eq(3));
        verify(rankingCacheService).evictAll();
    }

    @Test
    @DisplayName("并发场景：20 个线程抢同一座位同一时段，仅 1 条预约成功")
    void concurrentBookingOnlyOneSucceeds() throws InterruptedException {
        int threads = 20;
        Seat seat = seat(10L, 1L, Constants.SEAT_STATUS_FREE);

        ReentrantLock rowLock = new ReentrantLock(true);
        Set<String> activeSlots = ConcurrentHashMap.newKeySet();
        AtomicLong idGenerator = new AtomicLong();

        // 模拟数据库行锁：select ... for update 加锁，事务结束（更新/失败）时释放
        when(seatMapper.selectByIdForUpdate(10L)).thenAnswer(inv -> {
            rowLock.lock();
            return seat;
        });
        when(bookingMapper.countSeatConflictBookings(eq(10L), any(), any())).thenAnswer(inv -> {
            boolean conflict = !activeSlots.add("slot");
            if (conflict) {
                rowLock.unlock();
                return 1;
            }
            return 0;
        });
        when(bookingMapper.insert(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId(idGenerator.incrementAndGet());
            return 1;
        });
        when(seatMapper.updateSeatState(any(), any(), any(), any(), any())).thenAnswer(inv -> {
            rowLock.unlock();
            return 1;
        });

        BookingCreateDTO dto = new BookingCreateDTO();
        dto.setSeatId(10L);
        dto.setStartTime(LocalDateTime.now().plusHours(2));
        dto.setExpectedMinutes(60);

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger conflict = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    service.create(2L, dto);
                    success.incrementAndGet();
                } catch (BusinessException e) {
                    if (ErrorCode.SEAT_OCCUPIED.getCode().equals(e.getCode())) {
                        conflict.incrementAndGet();
                    }
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        ready.await(5, TimeUnit.SECONDS);
        start.countDown();
        assertThat(done.await(15, TimeUnit.SECONDS)).isTrue();
        pool.shutdownNow();

        assertThat(success.get()).isEqualTo(1);
        assertThat(conflict.get()).isEqualTo(threads - 1);
        verify(bookingMapper, times(1)).insert(any(Booking.class));
    }
}
