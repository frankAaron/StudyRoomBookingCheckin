package com.tyut.studyroombookingcheckin.dto;

import lombok.Data;

/**
 * 修改密码入参
 */
@Data
public class PasswordUpdateDTO {

    private String oldPassword;

    private String newPassword;
}
