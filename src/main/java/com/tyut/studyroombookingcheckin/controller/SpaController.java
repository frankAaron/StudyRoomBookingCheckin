package com.tyut.studyroombookingcheckin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 前端 History 路由入口。
 * <p>
 * 直接访问 /api/login、/api/home 等地址（或在这些页面刷新）时，转发到 Vue 入口页 index.html，
 * 由前端路由接管，避免出现 404。
 */
@Controller
public class SpaController {

    @GetMapping({"/login", "/home", "/reserve", "/bookings", "/study",
            "/stats", "/goals", "/ranking", "/admin", "/users"})
    public String index() {
        return "forward:/index.html";
    }
}
