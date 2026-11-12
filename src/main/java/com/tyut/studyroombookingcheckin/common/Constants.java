package com.tyut.studyroombookingcheckin.common;

public class Constants {

    public static final Integer LOGIC_DELETED = 1;
    public static final Integer LOGIC_NOT_DELETED = 0;

    public static final Long DEFAULT_TIMEOUT_MINUTES = 30L;

    public static final Integer SEAT_STATUS_FREE = 0;
    public static final Integer SEAT_STATUS_OCCUPIED = 1;
    public static final Integer SEAT_STATUS_RESERVED = 2;
    public static final Integer SEAT_STATUS_MAINTENANCE = 3;

    public static final Integer BOOKING_STATUS_PENDING = 0;
    public static final Integer BOOKING_STATUS_CONFIRMED = 1;
    public static final Integer BOOKING_STATUS_COMPLETED = 2;
    public static final Integer BOOKING_STATUS_CANCELLED = 3;
    public static final Integer BOOKING_STATUS_EXPIRED = 4;

    public static final Integer CHECKIN_STATUS_STARTED = 0;
    public static final Integer CHECKIN_STATUS_ENDED = 1;
    public static final Integer CHECKIN_STATUS_TEMP_LEFT = 2;
}