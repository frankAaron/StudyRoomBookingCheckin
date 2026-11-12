package com.tyut.studyroombookingcheckin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 管理端操作日志
 */
@Data
@TableName("t_admin_operation_log")
public class AdminOperationLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作人 ID */
    private Long operatorId;

    /** 操作人用户名 */
    private String operatorName;

    /** 操作类型：新增用户 / 编辑用户 / 删除用户 / 启用账号 / 禁用账号 */
    private String action;

    /** 被操作用户 ID */
    private Long targetId;

    /** 被操作用户名 */
    private String targetName;

    /** 操作详情 */
    private String detail;

    private LocalDateTime createTime;
}
