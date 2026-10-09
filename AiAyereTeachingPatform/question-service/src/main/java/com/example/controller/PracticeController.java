package com.example.controller;

import com.example.dto.AnswerSubmitRequest;
import com.example.result.Result;
import com.example.service.PracticeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequestMapping("/api")
public class PracticeController {

    @Autowired
    private PracticeService practiceService;

    /** 进入练习：获取某题目下的小问题 */
    @GetMapping("/practice/question/{questionId}")
    public Result listPractice(@PathVariable Long questionId) {
        return Result.success(practiceService.listPractice(questionId));
    }

    /** 提交作答：自动批改并返回解析 */
    @PostMapping("/practice/submit")
    public Result submit(@RequestBody AnswerSubmitRequest req) {
        try {
            return Result.success(practiceService.submit(req));
        } catch (Exception e) {
            return Result.error("提交失败：" + e.getMessage());
        }
    }

    // ===== 错题本 =====
    @GetMapping("/wrongbook/list")
    public Result listWrongBook(@RequestParam Long studentId) {
        return Result.success(practiceService.listWrongBook(studentId));
    }

    @PostMapping("/wrongbook/review")
    public Result markReviewed(@RequestParam Long wrongBookId) {
        practiceService.markReviewed(wrongBookId);
        return Result.success("已标记复习");
    }

    @DeleteMapping("/wrongbook/{wrongBookId}")
    public Result removeWrongBook(@PathVariable Long wrongBookId) {
        practiceService.removeWrongBook(wrongBookId);
        return Result.success("已移除错题");
    }
}