package com.example.service;

import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.models.RuntimeOptions;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeRequest;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeResponse;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class AliSmsService {

    @Value("${aliyun.sms.access-key-id}")
    private String accessKeyId;

    @Value("${aliyun.sms.access-key-secret}")
    private String accessKeySecret;

    @Value("${aliyun.sms.endpoint}")
    private String endpoint;

    @Value("${aliyun.sms.sign-name}")
    private String signName;

    @Value("${aliyun.sms.template-code}")
    private String templateCode;

    private com.aliyun.dypnsapi20170525.Client client;

    private final ConcurrentHashMap<String, String> codeStore = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() throws Exception {
        Config config = new Config()
                .setAccessKeyId(accessKeyId)
                .setAccessKeySecret(accessKeySecret);
        config.endpoint = endpoint;
        client = new com.aliyun.dypnsapi20170525.Client(config);
    }

    public String sendVerifyCode(String phoneNumber) {
        // mock 兜底：未配置短信或调用失败时，返回 6 位随机码，保证本地演示可在无短信环境下跑通
        String fallback = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
        if (accessKeyId == null || accessKeyId.isBlank()) {
            codeStore.put(phoneNumber, fallback);
            return fallback;
        }
        try {
            SendSmsVerifyCodeRequest request = new SendSmsVerifyCodeRequest()
                    .setSignName(signName)
                    .setTemplateCode(templateCode)
                    .setPhoneNumber(phoneNumber)
                    .setTemplateParam("{\"code\":\"##code##\",\"min\":\"5\"}")
                    .setCountryCode("86")
                    .setSmsUpExtendCode("0000")
                    .setOutId("sms" + System.currentTimeMillis())
                    .setCodeLength(6L)
                    .setValidTime(300L)
                    .setDuplicatePolicy(2L)
                    .setInterval(60L)
                    .setCodeType(1L)
                    .setReturnVerifyCode(true)
                    .setAutoRetry(1L);

            RuntimeOptions runtime = new RuntimeOptions();
            SendSmsVerifyCodeResponse resp = client.sendSmsVerifyCodeWithOptions(request, runtime);

            String verifyCode = resp.getBody().getModel().getVerifyCode();
            if (verifyCode == null || verifyCode.isBlank()) {
                verifyCode = fallback;
            }
            codeStore.put(phoneNumber, verifyCode);
            return verifyCode;
        } catch (Exception e) {
            // 短信通道不可用时的兜底
            codeStore.put(phoneNumber, fallback);
            return fallback;
        }
    }

    public boolean verifyCode(String phoneNumber, String code) {
        String storedCode = codeStore.get(phoneNumber);
        if (storedCode != null && storedCode.equals(code)) {
            codeStore.remove(phoneNumber);
            return true;
        }
        return false;
    }

}