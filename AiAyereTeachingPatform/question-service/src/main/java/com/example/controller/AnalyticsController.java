package com.example.controller;

import com.example.result.Result;
import com.example.service.AnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    @Autowired
    private AnalyticsService analyticsService;

    /** 班级学情：老师查看（可按老师ID限定） */
    @GetMapping("/class")
    public Result classAnalytics(@RequestParam(required = false) Long teacherId) {
        return Result.success(analyticsService.classAnalytics(teacherId));
    }

    /** 全部已发布题目统计（管理员视角） */
    @GetMapping("/all")
    public Result allAnalytics() {
        return Result.success(analyticsService.allPublishedAnalytics());
    }

    /** 个人学情：学生查看 */
    @GetMapping("/personal")
    public Result personalAnalytics(@RequestParam Long studentId) {
        return Result.success(analyticsService.personalAnalytics(studentId));
    }
}