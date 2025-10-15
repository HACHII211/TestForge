package com.example.service;
import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.ShearCaptcha;
import org.springframework.stereotype.Service;

@Service
public class CaptchaService {

    public ShearCaptcha generateCaptcha() {
        ShearCaptcha captcha = CaptchaUtil.createShearCaptcha(1000, 280, 4, 10);
        return captcha;
    }

    public boolean validate(String userInput, String captchaCode) {
        System.out.println(userInput + " " + captchaCode);
        userInput = userInput.trim().replace("\"", "");
        captchaCode = captchaCode.trim().replace("\"", "");
        return userInput.equalsIgnoreCase(captchaCode);
    }
}