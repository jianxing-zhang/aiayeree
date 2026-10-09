package com.example.controller;

import com.example.ai.AgentClient;
import com.example.dto.CalibrateRequest;
import com.example.dto.UploadQuestionRequest;
import com.example.entity.Question;
import com.example.result.Result;
import com.example.service.QuestionBankService;
import com.example.util.FileTextExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("/api/question")
public class QuestionController {

    @Autowired
    private QuestionBankService questionBankService;

    @Autowired
    private AgentClient agentClient;

    /** 文本录入智能出题 */
    @PostMapping("/upload-text")
    public Result uploadText(@RequestBody UploadQuestionRequest req) {
        try {
            Question q = questionBankService.uploadAndDecompose(req);
            return Result.success(questionBankService.detail(q.getId()));
        } catch (Exception e) {
            return Result.error("智能出题失败：" + e.getMessage());
        }
    }

    /** 图片上传（拍照/截图），qwen-vl 直接读图识别 + 出题 */
    @PostMapping("/upload-image")
    public Result uploadImage(@RequestParam("file") MultipartFile file,
                              @RequestParam(required = false) Long uploaderId,
                              @RequestParam(required = false) String uploaderName,
                              @RequestParam(required = false) String subject) {
        try {
            String base64 = Base64.getEncoder().encodeToString(file.getBytes());
            UploadQuestionRequest req = new UploadQuestionRequest();
            req.setUploaderId(uploaderId);
            req.setUploaderName(uploaderName);
            req.setSubject(subject);
            req.setSourceType("IMAGE");
            req.setImageBase64(base64);
            Question q = questionBankService.uploadAndDecompose(req);
            return Result.success(questionBankService.detail(q.getId()));
        } catch (Exception e) {
            return Result.error("图片识别失败：" + e.getMessage());
        }
    }

    /** 文档上传（docx/pdf/txt），先抽取文字再智能出题 */
    @PostMapping("/upload-doc")
    public Result uploadDoc(@RequestParam("file") MultipartFile file,
                            @RequestParam(required = false) Long uploaderId,
                            @RequestParam(required = false) String uploaderName,
                            @RequestParam(required = false) String subject) {
        try {
            String text = FileTextExtractor.extract(file.getBytes(), file.getOriginalFilename());
            UploadQuestionRequest req = new UploadQuestionRequest();
            req.setUploaderId(uploaderId);
            req.setUploaderName(uploaderName);
            req.setSubject(subject);
            req.setSourceType("DOC");
            req.setText(text);
            Question q = questionBankService.uploadAndDecompose(req);
            return Result.success(questionBankService.detail(q.getId()));
        } catch (Exception e) {
            return Result.error("文档解析失败：" + e.getMessage());
        }
    }

    /** 搜索 / 列表 */
    @GetMapping("/list")
    public Result list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String difficulty,
                       @RequestParam(required = false) String subject,
                       @RequestParam(required = false) Long uploaderId) {
        return Result.success(questionBankService.search(keyword, difficulty, subject, uploaderId));
    }

    /** 题目详情（含知识点与小问题） */
    @GetMapping("/{id}")
    public Result detail(@PathVariable Long id) {
        Map<String, Object> detail = questionBankService.detail(id);
        return detail == null ? Result.error("题目不存在") : Result.success(detail);
    }

    /** 人工校准：保存编辑后的知识点与小问题 */
    @PostMapping("/calibrate")
    public Result calibrate(@RequestBody CalibrateRequest req) {
        try {
            questionBankService.calibrate(req);
            return Result.success(questionBankService.detail(req.getQuestionId()));
        } catch (Exception e) {
            return Result.error("校准失败：" + e.getMessage());
        }
    }

    /** 发布到班级 */
    @PostMapping("/publish")
    public Result publish(@RequestParam Long questionId,
                          @RequestParam(required = false) String className,
                          @RequestParam(required = false) Long publisherId,
                          @RequestParam(required = false) String publisherName) {
        try {
            questionBankService.publish(questionId, className, publisherId, publisherName);
            return Result.success("发布成功");
        } catch (Exception e) {
            return Result.error("发布失败：" + e.getMessage());
        }
    }

    /** 删除题目（学生可管理个人题库） */
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id) {
        questionBankService.delete(id);
        return Result.success("删除成功");
    }

    /** 重新生成（重新调用 AI） */
    @PostMapping("/{id}/regen")
    public Result regenerate(@PathVariable Long id) {
        try {
            Question q = questionBankService.regenerate(id);
            return Result.success(questionBankService.detail(q.getId()));
        } catch (Exception e) {
            return Result.error("重新生成失败：" + e.getMessage());
        }
    }

    /** 按需生成知识点大纲中某个主题的详细内容 */
    @GetMapping("/{id}/outline")
    public Result outline(@PathVariable Long id, @RequestParam String topic) {
        try {
            Map<String, Object> detail = questionBankService.detail(id);
            if (detail == null) return Result.error("题目不存在");
            Question q = (Question) detail.get("question");
            String questionText = q.getSourceText() != null ? q.getSourceText()
                    : (q.getRecognizedText() != null ? q.getRecognizedText() : "");
            String aiText = agentClient.generateOutlineTopic(questionText, topic);
            return Result.success(aiText);
        } catch (Exception e) {
            return Result.error("知识点生成失败：" + e.getMessage());
        }
    }

    // ===== 收藏 =====
    @PostMapping("/favorite")
    public Result addFavorite(@RequestParam Long userId, @RequestParam Long questionId,
                              @RequestParam(required = false) String category) {
        questionBankService.addFavorite(userId, questionId, category);
        return Result.success("已收藏");
    }

    @DeleteMapping("/favorite")
    public Result removeFavorite(@RequestParam Long userId, @RequestParam Long questionId) {
        questionBankService.removeFavorite(userId, questionId);
        return Result.success("已取消收藏");
    }

    @GetMapping("/favorite/list")
    public Result listFavorites(@RequestParam Long userId,
                                @RequestParam(required = false) String category) {
        return Result.success(questionBankService.listFavorites(userId, category));
    }

    @GetMapping("/favorite/categories")
    public Result listFavoriteCategories(@RequestParam Long userId) {
        return Result.success(questionBankService.listFavoriteCategories(userId));
    }
}