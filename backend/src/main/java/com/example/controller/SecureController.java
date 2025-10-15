package com.example.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Slf4j
@RestController
public class SecureController {

    @RequestMapping("/secure/getUserInfo")
    public String getUserInfo(HttpServletRequest request) {
        Integer id = (Integer) request.getAttribute("id");
        String userName = (String) request.getAttribute("userName");
        String password = (String) request.getAttribute("password");

        if (id == null || userName == null || password == null) {
            log.warn("请求缺少必要的用户信息：id={}, userName={}, password={}", id, userName, password);
            return "缺少必要的用户信息，请重新登录";
        }

        return "当前用户信息id=" + id + ", userName=" + userName + ", password=" + password;
    }
}
