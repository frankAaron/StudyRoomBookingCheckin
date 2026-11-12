package com.tyut.studyroombookingcheckin.service;

import com.tyut.studyroombookingcheckin.dto.LoginDTO;
import com.tyut.studyroombookingcheckin.dto.RegisterDTO;
import com.tyut.studyroombookingcheckin.vo.LoginVO;

public interface AuthService {

    LoginVO login(LoginDTO dto);

    void register(RegisterDTO dto);
}