// 路径：com/example/demo/interceptor/LoginInterceptor.java
package com.example.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.PrintWriter;

public class LoginInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 获取Session
        HttpSession session = request.getSession();
        // 2. 判断是否已登录（匹配LoginController中写入的key）
        if (session.getAttribute("loginUser") != null) {
            return true; // 已登录，放行跳转
        }
        // 3. 未登录，返回JSON提示（避免前端白屏）
        response.setContentType("application/json;charset=utf-8");
        PrintWriter out = response.getWriter();
        out.write("{\"code\":401,\"msg\":\"未登录，请先登录！\"}");
        out.flush();
        out.close();
        return false; // 拦截跳转
    }
}