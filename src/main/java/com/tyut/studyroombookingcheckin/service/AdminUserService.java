package com.tyut.studyroombookingcheckin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tyut.studyroombookingcheckin.dto.AdminUserSaveDTO;
import com.tyut.studyroombookingcheckin.entity.AdminOperationLog;
import com.tyut.studyroombookingcheckin.vo.AdminUserVO;

/**
 * 管理端用户管理（仅超级管理员可用）
 */
public interface AdminUserService {

    /**
     * 分页查询用户，支持用户名 / 姓名 / 手机号模糊搜索与角色、状态筛选
     */
    Page<AdminUserVO> pageUsers(long current, long size, String keyword, Integer role, Integer status);

    AdminUserVO createUser(AdminUserSaveDTO dto);

    AdminUserVO updateUser(Long id, AdminUserSaveDTO dto);

    void deleteUser(Long id);

    void updateStatus(Long id, Integer status);

    /**
     * 分页查询管理端操作日志
     */
    Page<AdminOperationLog> pageLogs(long current, long size);
}
