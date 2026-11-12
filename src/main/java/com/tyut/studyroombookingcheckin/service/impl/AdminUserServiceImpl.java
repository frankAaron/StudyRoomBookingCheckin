package com.tyut.studyroombookingcheckin.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tyut.studyroombookingcheckin.common.BusinessException;
import com.tyut.studyroombookingcheckin.common.ErrorCode;
import com.tyut.studyroombookingcheckin.dto.AdminUserSaveDTO;
import com.tyut.studyroombookingcheckin.entity.AdminOperationLog;
import com.tyut.studyroombookingcheckin.entity.User;
import com.tyut.studyroombookingcheckin.mapper.AdminOperationLogMapper;
import com.tyut.studyroombookingcheckin.mapper.UserMapper;
import com.tyut.studyroombookingcheckin.security.SecurityContextHolder;
import com.tyut.studyroombookingcheckin.service.AdminUserService;
import com.tyut.studyroombookingcheckin.vo.AdminUserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private static final int ROLE_ADMIN = 1;
    private static final int STATUS_ENABLED = 1;
    private static final int STATUS_DISABLED = 0;

    private final UserMapper userMapper;
    private final AdminOperationLogMapper operationLogMapper;

    @Override
    public Page<AdminUserVO> pageUsers(long current, long size, String keyword, Integer role, Integer status) {
        requireSuperAdmin();
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(q -> q.like(User::getUsername, kw)
                    .or().like(User::getNickname, kw)
                    .or().like(User::getPhone, kw)
                    .or().like(User::getStudentNo, kw));
        }
        if (role != null) {
            wrapper.eq(User::getRole, role);
        }
        if (status != null) {
            wrapper.eq(User::getStatus, status);
        }
        wrapper.orderByDesc(User::getCreateTime).orderByDesc(User::getId);

        Page<User> page = userMapper.selectPage(new Page<>(current, size), wrapper);
        Page<AdminUserVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<AdminUserVO> records = page.getRecords().stream().map(this::toVO).toList();
        result.setRecords(records);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminUserVO createUser(AdminUserSaveDTO dto) {
        requireSuperAdmin();
        if (dto.getRole() == null) {
            throw new BusinessException("请选择所属角色");
        }
        String username = dto.getUsername().trim();
        String password = dto.getPassword() == null ? "" : dto.getPassword();
        checkPassword(password, true);
        if (userMapper.countUsernameOrStudentNo(username, null) > 0) {
            throw new BusinessException("用户名已存在，请更换后重试");
        }

        User user = new User();
        user.setUsername(username);
        user.setStudentNo(username);
        user.setPassword(BCrypt.hashpw(password));
        user.setNickname(dto.getNickname().trim());
        user.setPhone(normalizePhone(dto.getPhone()));
        user.setRole(dto.getRole());
        user.setStatus(dto.getStatus() == null ? STATUS_ENABLED : dto.getStatus());
        user.setTotalStudyMinutes(0);
        user.setMaxStreakDays(0);
        LocalDateTime now = LocalDateTime.now();
        user.setCreateTime(now);
        user.setUpdateTime(now);
        userMapper.insert(user);

        writeLog("新增用户", user.getId(), username,
                "角色=" + roleText(user.getRole()) + "，状态=" + statusText(user.getStatus()));
        return toVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminUserVO updateUser(Long id, AdminUserSaveDTO dto) {
        requireSuperAdmin();
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (dto.getRole() == null) {
            throw new BusinessException("请选择所属角色");
        }
        String username = dto.getUsername().trim();
        if (userMapper.countUsernameOrStudentNo(username, id) > 0) {
            throw new BusinessException("用户名已存在，请更换后重试");
        }
        int newRole = dto.getRole();
        int newStatus = dto.getStatus() == null ? user.getStatus() : dto.getStatus();

        boolean wasEnabledAdmin = isEnabledAdmin(user.getRole(), user.getStatus());
        boolean willBeEnabledAdmin = isEnabledAdmin(newRole, newStatus);
        if (wasEnabledAdmin && !willBeEnabledAdmin && userMapper.countEnabledAdminsExcluding(id) == 0) {
            throw new BusinessException("系统需至少保留一个启用状态的管理员");
        }
        if (id.equals(SecurityContextHolder.getUserId()) && newStatus == STATUS_DISABLED) {
            throw new BusinessException("不能禁用当前登录账号");
        }

        User update = new User();
        update.setId(id);
        update.setUsername(username);
        update.setNickname(dto.getNickname().trim());
        update.setPhone(normalizePhone(dto.getPhone()));
        update.setRole(newRole);
        update.setStatus(newStatus);
        update.setUpdateTime(LocalDateTime.now());

        boolean passwordChanged = StringUtils.hasText(dto.getPassword());
        if (passwordChanged) {
            checkPassword(dto.getPassword(), true);
            update.setPassword(BCrypt.hashpw(dto.getPassword()));
        }
        userMapper.updateById(update);

        writeLog("编辑用户", id, username,
                "角色=" + roleText(newRole) + "，状态=" + statusText(newStatus) + (passwordChanged ? "，已重置密码" : ""));
        return toVO(userMapper.selectById(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        requireSuperAdmin();
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (id.equals(SecurityContextHolder.getUserId())) {
            throw new BusinessException("不能删除当前登录账号");
        }
        if (isEnabledAdmin(user.getRole(), user.getStatus())
                && userMapper.countEnabledAdminsExcluding(id) == 0) {
            throw new BusinessException("系统需至少保留一个启用状态的管理员");
        }
        userMapper.deleteById(id);
        writeLog("删除用户", id, user.getUsername(),
                "角色=" + roleText(user.getRole()) + "，删除后不可恢复");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        requireSuperAdmin();
        if (status == null || (status != STATUS_ENABLED && status != STATUS_DISABLED)) {
            throw new BusinessException("账号状态参数不合法");
        }
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (status.equals(user.getStatus())) {
            return;
        }
        if (status == STATUS_DISABLED) {
            if (id.equals(SecurityContextHolder.getUserId())) {
                throw new BusinessException("不能禁用当前登录账号");
            }
            if (isEnabledAdmin(user.getRole(), user.getStatus())
                    && userMapper.countEnabledAdminsExcluding(id) == 0) {
                throw new BusinessException("系统需至少保留一个启用状态的管理员");
            }
        }

        User update = new User();
        update.setId(id);
        update.setStatus(status);
        update.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(update);

        writeLog(status == STATUS_ENABLED ? "启用账号" : "禁用账号", id, user.getUsername(),
                statusText(status));
    }

    @Override
    public Page<AdminOperationLog> pageLogs(long current, long size) {
        requireSuperAdmin();
        LambdaQueryWrapper<AdminOperationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(AdminOperationLog::getCreateTime).orderByDesc(AdminOperationLog::getId);
        return operationLogMapper.selectPage(new Page<>(current, size), wrapper);
    }

    private void requireSuperAdmin() {
        if (!Integer.valueOf(ROLE_ADMIN).equals(SecurityContextHolder.getRole())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private void checkPassword(String password, boolean required) {
        if (!StringUtils.hasText(password)) {
            if (required) {
                throw new BusinessException("登录密码不能为空");
            }
            return;
        }
        if (password.length() < 6 || password.length() > 32) {
            throw new BusinessException("登录密码长度需为 6-32 位");
        }
    }

    private String normalizePhone(String phone) {
        String value = phone == null ? "" : phone.trim();
        return value.isEmpty() ? null : value;
    }

    private boolean isEnabledAdmin(Integer role, Integer status) {
        return Integer.valueOf(ROLE_ADMIN).equals(role) && Integer.valueOf(STATUS_ENABLED).equals(status);
    }

    private String roleText(Integer role) {
        return Integer.valueOf(ROLE_ADMIN).equals(role) ? "管理员" : "学生";
    }

    private String statusText(Integer status) {
        return Integer.valueOf(STATUS_ENABLED).equals(status) ? "正常" : "禁用";
    }

    private AdminUserVO toVO(User user) {
        AdminUserVO vo = new AdminUserVO();
        vo.setId(user.getId());
        vo.setStudentNo(user.getStudentNo());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setRole(user.getRole());
        vo.setPhone(user.getPhone());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }

    private void writeLog(String action, Long targetId, String targetName, String detail) {
        AdminOperationLog log = new AdminOperationLog();
        log.setOperatorId(SecurityContextHolder.getUserId());
        log.setOperatorName(SecurityContextHolder.getUsername());
        log.setAction(action);
        log.setTargetId(targetId);
        log.setTargetName(targetName);
        log.setDetail(detail);
        log.setCreateTime(LocalDateTime.now());
        operationLogMapper.insert(log);
    }
}
