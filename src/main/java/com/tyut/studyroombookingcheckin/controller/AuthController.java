package com.tyut.studyroombookingcheckin.controller;

import com.tyut.studyroombookingcheckin.common.Result;
import com.tyut.studyroombookingcheckin.dto.LoginDTO;
import com.tyut.studyroombookingcheckin.dto.RegisterDTO;
import com.tyut.studyroombookingcheckin.service.AuthService;
import com.tyut.studyroombookingcheckin.vo.LoginVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success(authService.login(dto));
    }

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterDTO dto) {
        authService.register(dto);
        return Result.success();
    }
}