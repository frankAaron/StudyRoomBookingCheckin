package com.tyut.studyroombookingcheckin.service;

import com.tyut.studyroombookingcheckin.dto.LoginDTO;
import com.tyut.studyroombookingcheckin.dto.RegisterDTO;
import com.tyut.studyroombookingcheckin.vo.LoginVO;

public interface AuthService {

    LoginVO login(LoginDTO dto);

    /**
     * 带客户端 IP 的登录入口，用于登录限流。
     */
    LoginVO login(LoginDTO dto, String ip);

    void register(RegisterDTO dto);
}
