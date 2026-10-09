package com.example.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.dto.AnswerSubmitRequest;
import com.example.entity.AnswerRecord;
import com.example.entity.Question;
import com.example.entity.SubQuestion;
import com.example.entity.WrongBook;
import com.example.mapper.AnswerRecordMapper;
import com.example.mapper.QuestionMapper;
import com.example.mapper.SubQuestionMapper;
import com.example.mapper.WrongBookMapper;
import com.example.util.AnswerGrader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PracticeService {

    private final SubQuestionMapper subQuestionMapper;
    private final AnswerRecordMapper answerRecordMapper;
    private final WrongBookMapper wrongBookMapper;
    private final QuestionMapper questionMapper;

    public PracticeService(SubQuestionMapper subQuestionMapper,
                           AnswerRecordMapper answerRecordMapper,
                           WrongBookMapper wrongBookMapper,
                           QuestionMapper questionMapper) {
        this.subQuestionMapper = subQuestionMapper;
        this.answerRecordMapper = answerRecordMapper;
        this.wrongBookMapper = wrongBookMapper;
        this.questionMapper = questionMapper;
    }

    /** 获取某题目下的小问题用于练习 */
    public List<SubQuestion> listPractice(Long questionId) {
        return subQuestionMapper.selectList(new LambdaQueryWrapper<SubQuestion>()
                .eq(SubQuestion::getQuestionId, questionId)
                .orderByAsc(SubQuestion::getSortOrder));
    }

    /** 提交作答：自动批改 + 记错题 */
    @Transactional
    public List<Map<String, Object>> submit(AnswerSubmitRequest req) {
        List<Map<String, Object>> results = new ArrayList<>();
        if (req.getAnswers() == null) return results;

        for (AnswerSubmitRequest.Item item : req.getAnswers()) {
            SubQuestion sq = subQuestionMapper.selectById(item.getSubQuestionId());
            Map<String, Object> r = new HashMap<>();
            if (sq == null) {
                r.put("subQuestionId", item.getSubQuestionId());
                r.put("correct", false);
                continue;
            }
            boolean correct = AnswerGrader.isCorrect(item.getAnswer(), sq.getAnswer());
            r.put("subQuestionId", sq.getId());
            r.put("correct", correct);
            r.put("referenceAnswer", sq.getAnswer());
            r.put("solution", sq.getSolution());

            AnswerRecord record = new AnswerRecord();
            record.setStudentId(req.getStudentId());
            record.setSubQuestionId(sq.getId());
            record.setAnswer(item.getAnswer());
            record.setCorrect(correct);
            record.setAnswerTime(LocalDateTime.now());
            answerRecordMapper.insert(record);

            // 错题本维护
            List<WrongBook> existing = wrongBookMapper.selectList(new LambdaQueryWrapper<WrongBook>()
                    .eq(WrongBook::getStudentId, req.getStudentId())
                    .eq(WrongBook::getSubQuestionId, sq.getId()));
            if (!correct) {
                if (existing.isEmpty()) {
                    WrongBook wb = new WrongBook();
                    wb.setStudentId(req.getStudentId());
                    wb.setSubQuestionId(sq.getId());
                    wb.setWrongCount(1);
                    wb.setReviewStatus("NOT_REVIEWED");
                    wb.setCreateTime(LocalDateTime.now());
                    wb.setUpdateTime(LocalDateTime.now());
                    wrongBookMapper.insert(wb);
                } else {
                    WrongBook wb = existing.get(0);
                    wb.setWrongCount((wb.getWrongCount() == null ? 0 : wb.getWrongCount()) + 1);
                    wb.setReviewStatus("NOT_REVIEWED");
                    wb.setUpdateTime(LocalDateTime.now());
                    wrongBookMapper.updateById(wb);
                }
            } else if (!existing.isEmpty()) {
                // 答对则视为已复习掌握
                WrongBook wb = existing.get(0);
                wb.setReviewStatus("REVIEWED");
                wb.setUpdateTime(LocalDateTime.now());
                wrongBookMapper.updateById(wb);
            }
            results.add(r);
        }
        return results;
    }

    /** 错题本列表（带题干信息） */
    public List<Map<String, Object>> listWrongBook(Long studentId) {
        List<WrongBook> books = wrongBookMapper.selectList(new LambdaQueryWrapper<WrongBook>()
                .eq(WrongBook::getStudentId, studentId)
                .orderByDesc(WrongBook::getUpdateTime));
        List<Map<String, Object>> result = new ArrayList<>();
        for (WrongBook wb : books) {
            SubQuestion sq = subQuestionMapper.selectById(wb.getSubQuestionId());
            if (sq == null) continue;
            Map<String, Object> m = new HashMap<>();
            m.put("wrongBookId", wb.getId());
            m.put("wrongCount", wb.getWrongCount());
            m.put("reviewStatus", wb.getReviewStatus());
            m.put("subQuestionId", sq.getId());
            m.put("content", sq.getContent());
            m.put("answer", sq.getAnswer());
            m.put("solution", sq.getSolution());
            m.put("difficulty", sq.getDifficulty());
            Question q = questionMapper.selectById(sq.getQuestionId());
            m.put("subject", q == null ? null : q.getSubject());
            result.add(m);
        }
        return result;
    }

    public void markReviewed(Long wrongBookId) {
        WrongBook wb = wrongBookMapper.selectById(wrongBookId);
        if (wb != null) {
            wb.setReviewStatus("REVIEWED");
            wb.setUpdateTime(LocalDateTime.now());
            wrongBookMapper.updateById(wb);
        }
    }

    public void removeWrongBook(Long wrongBookId) {
        wrongBookMapper.deleteById(wrongBookId);
    }
}