package com.tyut.studyroombookingcheckin.dto;

import lombok.Data;

/**
 * 个人信息修改入参
 */
@Data
public class ProfileUpdateDTO {

    private String nickname;

    private String email;

    private String phone;
}
