package com.tyut.studyroombookingcheckin.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端用户列表项（不含密码）
 */
@Data
public class AdminUserVO {

    private Long id;

    private String studentNo;

    private String username;

    private String nickname;

    private Integer role;

    private String phone;

    private Integer status;

    private LocalDateTime createTime;
}
