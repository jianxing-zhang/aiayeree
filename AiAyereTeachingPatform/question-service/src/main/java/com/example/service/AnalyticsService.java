package com.example.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.entity.AnswerRecord;
import com.example.entity.Publish;
import com.example.entity.Question;
import com.example.entity.SubQuestion;
import com.example.mapper.AnswerRecordMapper;
import com.example.mapper.PublishMapper;
import com.example.mapper.QuestionMapper;
import com.example.mapper.SubQuestionMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnalyticsService {

    private final AnswerRecordMapper answerRecordMapper;
    private final SubQuestionMapper subQuestionMapper;
    private final PublishMapper publishMapper;
    private final QuestionMapper questionMapper;

    public AnalyticsService(AnswerRecordMapper answerRecordMapper,
                            SubQuestionMapper subQuestionMapper,
                            PublishMapper publishMapper,
                            QuestionMapper questionMapper) {
        this.answerRecordMapper = answerRecordMapper;
        this.subQuestionMapper = subQuestionMapper;
        this.publishMapper = publishMapper;
        this.questionMapper = questionMapper;
    }

    /** 班级学情：按难度统计正确率（可传老师ID限定其发布题目范围） */
    public Map<String, Object> classAnalytics(Long teacherId) {
        List<Long> publishedQuestionIds = new ArrayList<>();
        LambdaQueryWrapper<Publish> pw = new LambdaQueryWrapper<>();
        if (teacherId != null) pw.eq(Publish::getPublisherId, teacherId);
        for (Publish p : publishMapper.selectList(pw)) {
            publishedQuestionIds.add(p.getQuestionId());
        }
        return buildClassAnalytics(publishedQuestionIds);
    }

    /** 全部已发布题目统计（管理员视角） */
    public Map<String, Object> allPublishedAnalytics() {
        List<Long> ids = new ArrayList<>();
        for (Publish p : publishMapper.selectList(null)) {
            ids.add(p.getQuestionId());
        }
        return buildClassAnalytics(ids);
    }

    private Map<String, Object> buildClassAnalytics(List<Long> questionIds) {
        // 难度 -> 作答/正确 统计
        Map<String, long[]> diffStats = new LinkedHashMap<>();
        Map<String, Integer> diffTotals = new LinkedHashMap<>();
        long totalAnswers = 0, totalCorrect = 0;

        if (questionIds.isEmpty()) {
            return wrap(totalAnswers, totalCorrect, diffStats);
        }
        List<SubQuestion> sqs = subQuestionMapper.selectList(
                new LambdaQueryWrapper<SubQuestion>().in(SubQuestion::getQuestionId, questionIds));
        Map<Long, String> sqDifficulty = new HashMap<>();
        for (SubQuestion sq : sqs) sqDifficulty.put(sq.getId(), sq.getDifficulty());

        List<AnswerRecord> records = new ArrayList<>();
        for (SubQuestion sq : sqs) {
            records.addAll(answerRecordMapper.selectList(new LambdaQueryWrapper<AnswerRecord>()
                    .eq(AnswerRecord::getSubQuestionId, sq.getId())));
        }

        for (AnswerRecord r : records) {
            String diff = sqDifficulty.getOrDefault(r.getSubQuestionId(), "未知");
            long[] stat = diffStats.computeIfAbsent(diff, k -> new long[2]);
            totalAnswers++;
            if (Boolean.TRUE.equals(r.getCorrect())) {
                stat[0]++;
                totalCorrect++;
            }
            stat[1]++;
            diffTotals.merge(diff, 1, Integer::sum);
        }

        Map<String, Object> byDifficulty = new LinkedHashMap<>();
        for (Map.Entry<String, long[]> e : diffStats.entrySet()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("correct", e.getValue()[0]);
            m.put("total", e.getValue()[1]);
            long t = e.getValue()[1];
            m.put("rate", t == 0 ? 0.0 : Math.round(e.getValue()[0] * 1000.0 / t) / 10.0);
            byDifficulty.put(e.getKey(), m);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalAnswers", totalAnswers);
        result.put("totalCorrect", totalCorrect);
        result.put("overallRate", totalAnswers == 0 ? 0.0
                : Math.round(totalCorrect * 1000.0 / totalAnswers) / 10.0);
        result.put("byDifficulty", byDifficulty);
        return result;
    }

    private Map<String, Object> wrap(long totalAnswers, long totalCorrect, Map<String, long[]> diffStats) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalAnswers", totalAnswers);
        result.put("totalCorrect", totalCorrect);
        result.put("overallRate", 0.0);
        result.put("byDifficulty", new LinkedHashMap<>());
        return result;
    }

    /** 个人学情：该生的答题正确率 + 错题数 */
    public Map<String, Object> personalAnalytics(Long studentId) {
        List<AnswerRecord> records = answerRecordMapper.selectList(
                new LambdaQueryWrapper<AnswerRecord>().eq(AnswerRecord::getStudentId, studentId));
        long total = records.size();
        long correct = 0;
        for (AnswerRecord r : records) {
            if (Boolean.TRUE.equals(r.getCorrect())) correct++;
        }

        // 按难度统计
        Map<String, long[]> diffStats = new LinkedHashMap<>();
        for (AnswerRecord r : records) {
            SubQuestion sq = subQuestionMapper.selectById(r.getSubQuestionId());
            String diff = sq == null ? "未知" : sq.getDifficulty();
            long[] stat = diffStats.computeIfAbsent(diff, k -> new long[2]);
            stat[1]++;
            if (Boolean.TRUE.equals(r.getCorrect())) stat[0]++;
        }
        Map<String, Object> byDifficulty = new LinkedHashMap<>();
        for (Map.Entry<String, long[]> e : diffStats.entrySet()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("correct", e.getValue()[0]);
            m.put("total", e.getValue()[1]);
            long t = e.getValue()[1];
            m.put("rate", t == 0 ? 0.0 : Math.round(e.getValue()[0] * 1000.0 / t) / 10.0);
            byDifficulty.put(e.getKey(), m);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalAnswers", total);
        result.put("correct", correct);
        result.put("wrong", total - correct);
        result.put("correctRate", total == 0 ? 0.0 : Math.round(correct * 1000.0 / total) / 10.0);
        result.put("byDifficulty", byDifficulty);
        return result;
    }
}