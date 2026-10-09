package com.example.controller;

import com.example.result.Result;
import com.example.service.AliSmsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AliSmsTest {

    @Autowired
    private AliSmsService aliSmsService;

    @GetMapping("/sms/send")
    public Result sendSms(@RequestParam(defaultValue = "19288315659") String phone) {
        try {
            String verifyCode = aliSmsService.sendVerifyCode(phone);
            return Result.success("验证码已发送：" + verifyCode);
        } catch (Exception e) {
            return Result.error("发送失败：" + e.getMessage());
        }
    }

    @GetMapping("/sms/verify")
    public Result verify(@RequestParam String phone, @RequestParam String code) {
        boolean ok = aliSmsService.verifyCode(phone, code);
        if (ok) {
            return Result.success("验证码正确");
        }
        return Result.error("验证码错误或已过期");
    }
}