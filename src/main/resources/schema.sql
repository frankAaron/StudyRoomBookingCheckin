-- ============================================================
-- Study Room Booking & Checkin System Database Schema
-- MySQL 8.0+ / utf8mb4 / utf8mb4_0900_ai_ci
-- 执行顺序：先建库，再建表（按依赖顺序），最后初始化数据
-- ============================================================

CREATE DATABASE IF NOT EXISTS study_room
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE study_room;
SET NAMES utf8mb4;

DROP TABLE IF EXISTS t_admin_operation_log;
DROP TABLE IF EXISTS t_violation;
DROP TABLE IF EXISTS t_friend;
DROP TABLE IF EXISTS t_study_goal;
DROP TABLE IF EXISTS t_study_checkin;
DROP TABLE IF EXISTS t_booking;
DROP TABLE IF EXISTS t_seat;
DROP TABLE IF EXISTS t_room;
DROP TABLE IF EXISTS t_building;
DROP TABLE IF EXISTS t_user;

-- 用户表
CREATE TABLE t_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_no VARCHAR(32) NOT NULL,
    username VARCHAR(64) NOT NULL,
    password VARCHAR(128) NOT NULL,
    nickname VARCHAR(64),
    avatar VARCHAR(256),
    email VARCHAR(128),
    phone VARCHAR(32),
    faculty INT COMMENT '院系',
    role INT DEFAULT 0 COMMENT '0-学生 1-管理员',
    status INT DEFAULT 1 COMMENT '0-禁用 1-正常',
    total_study_minutes INT DEFAULT 0,
    max_streak_days INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    UNIQUE KEY uk_student_no (student_no),
    UNIQUE KEY uk_username (username),
    KEY idx_role_status (role, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';

-- 楼栋表
CREATE TABLE t_building (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(64) NOT NULL,
    code VARCHAR(32),
    location VARCHAR(128),
    description VARCHAR(512),
    status INT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='楼栋表';

-- 自习室表
CREATE TABLE t_room (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    building_id BIGINT NOT NULL,
    name VARCHAR(64) NOT NULL,
    code VARCHAR(32),
    floor INT,
    total_seats INT DEFAULT 0,
    available_seats INT DEFAULT 0,
    equipment VARCHAR(256),
    open_time TIME DEFAULT '08:00:00',
    close_time TIME DEFAULT '22:00:00',
    status INT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    KEY idx_building_floor (building_id, floor),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='自习室表';

-- 座位表
CREATE TABLE t_seat (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    room_id BIGINT NOT NULL,
    seat_no VARCHAR(32) NOT NULL,
    seat_row INT,
    seat_col INT,
    qrcode VARCHAR(128),
    status INT DEFAULT 0 COMMENT '0-空闲 1-占用 2-预留 3-维护',
    occupied_user_id BIGINT,
    occupied_time DATETIME,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    UNIQUE KEY uk_room_seat_no (room_id, seat_no),
    UNIQUE KEY uk_qrcode (qrcode),
    KEY idx_room_status (room_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='座位表';

-- 预约表
CREATE TABLE t_booking (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    seat_id BIGINT NOT NULL,
    room_id BIGINT,
    building_id BIGINT,
    booking_date DATE,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    expected_minutes INT,
    actual_minutes INT,
    status INT DEFAULT 0 COMMENT '0-待签到 1-学习中 2-已完成 3-已取消 4-已过期',
    checkin_code VARCHAR(32),
    checkin_time DATETIME,
    checkout_time DATETIME,
    remark VARCHAR(256),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    -- 仅对"有效预约(status 0/1 且未删除)"生成标记值，历史取消/完成记录为 NULL 不参与唯一约束
    active_slot TINYINT GENERATED ALWAYS AS (
        CASE WHEN deleted = 0 AND status IN (0, 1) THEN 1 ELSE NULL END
    ) VIRTUAL,
    -- 同一座位同一开始时间仅允许一条有效预约，与 SELECT ... FOR UPDATE 共同防并发重复预订
    UNIQUE KEY uk_seat_active_start (seat_id, start_time, active_slot),
    UNIQUE KEY uk_checkin_code (checkin_code),
    KEY idx_user_status (user_id, status),
    KEY idx_seat_status_time (seat_id, status, start_time, end_time),
    KEY idx_booking_date (booking_date),
    KEY idx_status (status),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='预约表';

-- 学习打卡表
CREATE TABLE t_study_checkin (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    booking_id BIGINT,
    seat_id BIGINT,
    study_date DATE NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME,
    duration_minutes INT,
    status INT DEFAULT 0 COMMENT '0-进行中 1-已结束 2-暂离',
    remark VARCHAR(256),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    KEY idx_user_date (user_id, study_date),
    KEY idx_booking (booking_id),
    KEY idx_study_date (study_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学习打卡表';

-- 学习目标表
CREATE TABLE t_study_goal (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(128) NOT NULL,
    target_minutes INT NOT NULL,
    start_date DATE,
    end_date DATE,
    completed_minutes INT DEFAULT 0,
    status INT DEFAULT 0 COMMENT '0-进行中 1-已完成 2-已放弃',
    complete_time DATETIME,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    KEY idx_user_status (user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学习目标表';

-- 好友关系表
CREATE TABLE t_friend (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    friend_id BIGINT NOT NULL,
    status INT DEFAULT 0 COMMENT '0-待确认 1-已确认',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    UNIQUE KEY uk_pair (user_id, friend_id),
    KEY idx_friend_status (friend_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='好友关系表';

-- 违规记录表
CREATE TABLE t_violation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    booking_id BIGINT,
    seat_id BIGINT,
    type INT COMMENT '1-爽约未签到 2-超时占座 3-其他',
    description VARCHAR(512),
    penalty INT DEFAULT 0 COMMENT '处罚等级',
    status INT DEFAULT 0 COMMENT '0-未处理 1-已处理',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    UNIQUE KEY uk_booking_type (booking_id, type),
    KEY idx_user_status (user_id, status),
    KEY idx_type_status (type, status),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='违规记录表';

-- 管理端操作日志表
CREATE TABLE t_admin_operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    operator_id BIGINT NOT NULL COMMENT '操作人 ID',
    operator_name VARCHAR(64) COMMENT '操作人用户名',
    action VARCHAR(32) NOT NULL COMMENT '操作类型',
    target_id BIGINT COMMENT '被操作用户 ID',
    target_name VARCHAR(64) COMMENT '被操作用户名',
    detail VARCHAR(256) COMMENT '操作详情',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_operator (operator_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='管理端操作日志表';

-- ============================================================
-- 初始化数据
-- ============================================================

-- 管理员账号：admin / admin123（BCrypt 加密）
INSERT INTO t_user (student_no, username, password, nickname, role, status) VALUES
('admin', 'admin', '$2a$10$65I8MIACFZnNnoZ8zc3Ap.rEtop5jN5d12RekRXTebaFvLuYvUBfm', '系统管理员', 1, 1);

-- 楼栋示例
INSERT INTO t_building (name, code, location, description) VALUES
('图书馆', 'LIB', '校园中心', '主图书馆，四层楼自习区'),
('教学楼A', 'TEACH_A', '东区', '公共自习教室'),
('教学楼B', 'TEACH_B', '西区', '专业自习室');

-- 自习室示例
INSERT INTO t_room (building_id, name, code, floor, total_seats, open_time, close_time) VALUES
(1, '一楼自习区', 'LIB-1F', 1, 60, '08:00:00', '22:00:00'),
(1, '二楼自习区', 'LIB-2F', 2, 80, '08:00:00', '22:00:00'),
(2, 'A101', 'A101', 1, 50, '07:30:00', '21:30:00'),
(2, 'A201', 'A201', 2, 50, '07:30:00', '21:30:00'),
(3, 'B301-考研自习室', 'B301', 3, 40, '06:30:00', '23:30:00');
