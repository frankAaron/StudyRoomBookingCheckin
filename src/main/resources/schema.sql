-- Study Room Booking & Checkin System Database Schema
-- MySQL 8.0+, utf8mb4
DROP TABLE IF EXISTS t_seat;
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
    INDEX idx_room_id (room_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='座位表';

CREATE DATABASE IF NOT EXISTS study_room DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE study_room;

-- 用户表
CREATE TABLE IF NOT EXISTS t_user (
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
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 楼栋表
CREATE TABLE IF NOT EXISTS t_building (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(64) NOT NULL,
    code VARCHAR(32),
    location VARCHAR(128),
    description VARCHAR(512),
    status INT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='楼栋表';

-- 自习室表
CREATE TABLE IF NOT EXISTS t_room (
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
    INDEX idx_building_id (building_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自习室表';

-- 座位表
CREATE TABLE IF NOT EXISTS t_seat (
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
    INDEX idx_room_id (room_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='座位表';

-- 预约表
CREATE TABLE IF NOT EXISTS t_booking (
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
    status INT DEFAULT 0 COMMENT '0-待签到 1-已签到 2-已完成 3-已取消 4-已过期',
    checkin_code VARCHAR(32),
    checkin_time DATETIME,
    checkout_time DATETIME,
    remark VARCHAR(256),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_user_id (user_id),
    INDEX idx_seat_id (seat_id),
    INDEX idx_booking_date (booking_date),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预约表';

-- 学习打卡表
CREATE TABLE IF NOT EXISTS t_study_checkin (
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
    INDEX idx_user_id (user_id),
    INDEX idx_study_date (study_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习打卡表';

-- 学习目标表
CREATE TABLE IF NOT EXISTS t_study_goal (
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
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习目标表';

-- 好友关系表
CREATE TABLE IF NOT EXISTS t_friend (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    friend_id BIGINT NOT NULL,
    status INT DEFAULT 0 COMMENT '0-待确认 1-已确认',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    UNIQUE KEY uk_pair (user_id, friend_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='好友关系表';

-- 违规记录表
CREATE TABLE IF NOT EXISTS t_violation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    booking_id BIGINT,
    seat_id BIGINT,
    type INT COMMENT '违规类型',
    description VARCHAR(512),
    penalty INT DEFAULT 0 COMMENT '处罚等级',
    status INT DEFAULT 0 COMMENT '0-未处理 1-已处理',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='违规记录表';

-- 初始化数据：管理员账号（密码 admin123，BCrypt加密）
INSERT INTO t_user (student_no, username, password, nickname, role, status) VALUES
('admin', 'admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '系统管理员', 1, 1);

-- 初始化楼栋示例
INSERT INTO t_building (name, code, location, description) VALUES
('图书馆', 'LIB', '校园中心', '主图书馆，四层楼自习区'),
('教学楼A', 'TEACH_A', '东区', '公共自习教室'),
('教学楼B', 'TEACH_B', '西区', '专业自习室');

-- 初始化自习室示例
INSERT INTO t_room (building_id, name, code, floor, total_seats, open_time, close_time) VALUES
(1, '一楼自习区', 'LIB-1F', 1, 60, '08:00:00', '22:00:00'),
(1, '二楼自习区', 'LIB-2F', 2, 80, '08:00:00', '22:00:00'),
(2, 'A101', 'A101', 1, 50, '07:30:00', '21:30:00'),
(2, 'A201', 'A201', 2, 50, '07:30:00', '21:30:00'),
(3, 'B301-考研自习室', 'B301', 3, 40, '06:30:00', '23:30:00');