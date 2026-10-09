package com.example.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 验证码存储：优先用 Redis，Redis 不可用时自动降级到本地内存。
 * 内存兜底同样带过期时间，保证无 Redis 环境下注册/找回密码流程仍可演示。
 */
@Component
public class VerifyCodeStore {

    private final StringRedisTemplate stringRedisTemplate;

    /** 内存兜底：key -> [value, 过期时间戳(ms)] */
    private final ConcurrentHashMap<String, MemEntry> fallback = new ConcurrentHashMap<>();

    public VerifyCodeStore(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /** 保存验证码（分钟），Redis 与内存双写 */
    public void save(String key, String code, long minutes) {
        long expireAt = System.currentTimeMillis() + minutes * 60_000L;
        fallback.put(key, new MemEntry(code, expireAt));
        try {
            stringRedisTemplate.opsForValue().set(key, code, minutes, TimeUnit.MINUTES);
        } catch (Exception ignored) {
            // Redis 不可用，仅保留内存
        }
    }

    /** 读取验证码（不删除）；优先 Redis，读不到/异常时走内存 */
    public String get(String key) {
        String v = null;
        try {
            v = stringRedisTemplate.opsForValue().get(key);
        } catch (Exception ignored) {
            // Redis 不可用
        }
        if (v != null) return v;

        MemEntry entry = fallback.get(key);
        if (entry == null) return null;
        if (System.currentTimeMillis() > entry.expireAt) {
            fallback.remove(key);
            return null;
        }
        return entry.value;
    }

    /** 删除验证码，Redis 与内存都清 */
    public void remove(String key) {
        fallback.remove(key);
        try {
            stringRedisTemplate.delete(key);
        } catch (Exception ignored) {
            // Redis 不可用
        }
    }

    private static class MemEntry {
        final String value;
        final long expireAt;

        MemEntry(String value, long expireAt) {
            this.value = value;
            this.expireAt = expireAt;
        }
    }
}