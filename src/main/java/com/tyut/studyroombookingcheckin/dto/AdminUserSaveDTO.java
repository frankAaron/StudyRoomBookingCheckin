package com.tyut.studyroombookingcheckin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理端新增 / 编辑用户入参
 */
@Data
public class AdminUserSaveDTO {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 32, message = "用户名长度需为 2-32 位")
    @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "用户名仅支持字母、数字和下划线")
    private String username;

    /** 新增时必填；编辑时留空表示不修改密码 */
    private String password;

    @NotNull(message = "请选择所属角色")
    @Min(value = 0, message = "角色参数不合法")
    @Max(value = 1, message = "角色参数不合法")
    private Integer role;

    @NotBlank(message = "真实姓名不能为空")
    @Size(max = 20, message = "真实姓名不能超过 20 个字符")
    private String nickname;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "联系电话格式不正确")
    private String phone;

    @Min(value = 0, message = "账号状态参数不合法")
    @Max(value = 1, message = "账号状态参数不合法")
    private Integer status;
}
