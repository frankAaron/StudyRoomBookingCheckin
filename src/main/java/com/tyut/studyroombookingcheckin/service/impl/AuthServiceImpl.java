package com.tyut.studyroombookingcheckin.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tyut.studyroombookingcheckin.common.BusinessException;
import com.tyut.studyroombookingcheckin.common.ErrorCode;
import com.tyut.studyroombookingcheckin.dto.LoginDTO;
import com.tyut.studyroombookingcheckin.dto.RegisterDTO;
import com.tyut.studyroombookingcheckin.entity.User;
import com.tyut.studyroombookingcheckin.mapper.UserMapper;
import com.tyut.studyroombookingcheckin.security.JwtUtil;
import com.tyut.studyroombookingcheckin.security.LoginRateLimiter;
import com.tyut.studyroombookingcheckin.service.AuthService;
import com.tyut.studyroombookingcheckin.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final LoginRateLimiter loginRateLimiter;

    @Override
    public LoginVO login(LoginDTO dto) {
        return login(dto, "unknown");
    }

    @Override
    public LoginVO login(LoginDTO dto, String ip) {
        try {
            loginRateLimiter.checkBlocked(dto.getUsername(), ip);
        } catch (IllegalStateException e) {
            throw new BusinessException(429, e.getMessage());
        }

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getStudentNo, dto.getUsername())
                .or()
                .eq(User::getUsername, dto.getUsername()));
        if (user == null) {
            loginRateLimiter.onFailure(dto.getUsername(), ip);
            throw new BusinessException(ErrorCode.USER_NOT_EXIST);
        }
        if (!BCrypt.checkpw(dto.getPassword(), user.getPassword())) {
            loginRateLimiter.onFailure(dto.getUsername(), ip);
            throw new BusinessException(ErrorCode.PASSWORD_ERROR);
        }
        if (Integer.valueOf(0).equals(user.getStatus())) {
            throw new BusinessException("账号已被禁用，请联系管理员");
        }
        loginRateLimiter.onSuccess(dto.getUsername(), ip);
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setRole(user.getRole());
        vo.setAvatar(user.getAvatar());
        return vo;
    }

    @Override
    public void register(RegisterDTO dto) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getStudentNo, dto.getStudentNo())
                .or()
                .eq(User::getUsername, dto.getUsername()));
        if (count > 0) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXIST);
        }
        User user = new User();
        user.setStudentNo(dto.getStudentNo());
        user.setUsername(dto.getUsername());
        user.setPassword(BCrypt.hashpw(dto.getPassword()));
        user.setNickname(dto.getNickname() != null ? dto.getNickname() : dto.getUsername());
        user.setFaculty(dto.getFaculty());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setRole(0);
        user.setStatus(1);
        user.setTotalStudyMinutes(0);
        user.setMaxStreakDays(0);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);
    }
}
