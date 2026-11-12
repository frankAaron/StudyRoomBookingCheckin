package com.tyut.studyroombookingcheckin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.tyut.studyroombookingcheckin.mapper")
public class StudyRoomBookingCheckinApplication {

    public static void main(String[] args) {
        SpringApplication.run(StudyRoomBookingCheckinApplication.class, args);
    }
}