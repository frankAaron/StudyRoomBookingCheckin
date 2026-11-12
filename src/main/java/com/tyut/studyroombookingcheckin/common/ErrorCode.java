package com.tyut.studyroombookingcheckin.common;

import lombok.Getter;

@Getter
public enum ErrorCode {

    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权，请先登录"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    SERVER_ERROR(500, "服务器内部错误"),

    USER_NOT_EXIST(1001, "用户不存在"),
    USER_ALREADY_EXIST(1002, "用户已存在"),
    PASSWORD_ERROR(1003, "密码错误"),

    BUILDING_NOT_EXIST(2001, "楼栋不存在"),
    ROOM_NOT_EXIST(2002, "自习室不存在"),
    SEAT_NOT_EXIST(2003, "座位不存在"),
    SEAT_OCCUPIED(2004, "座位已被预约/占用"),
    SEAT_ALREADY_RELEASED(2005, "座位已释放"),

    BOOKING_NOT_EXIST(3001, "预约记录不存在"),
    BOOKING_CONFLICT(3002, "预约时段冲突"),
    BOOKING_EXPIRED(3003, "预约已过期"),
    BOOKING_CANCELLED(3004, "预约已取消"),
    CHECKIN_EXPIRED(3005, "签到时间已过"),
    ALREADY_CHECKED_IN(3006, "已签到，请勿重复签到"),

    CHECKIN_NOT_EXIST(4001, "打卡记录不存在"),
    NOT_CHECKED_IN(4002, "尚未打卡签到"),

    INVALID_TIME_RANGE(5001, "无效的时间范围"),
    FRIEND_REQUEST_EXIST(6001, "好友请求已存在"),
    ALREADY_FRIEND(6002, "已经是好友");

    private final Integer code;
    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}