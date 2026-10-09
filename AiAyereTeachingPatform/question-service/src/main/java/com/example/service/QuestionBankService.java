package com.example.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.ai.AgentClient;
import com.example.dto.CalibrateRequest;
import com.example.dto.ParsedAiItem;
import com.example.dto.UploadQuestionRequest;
import com.example.entity.Favorite;
import com.example.entity.KnowledgePoint;
import com.example.entity.Publish;
import com.example.entity.Question;
import com.example.entity.SubQuestion;
import com.example.mapper.FavoriteMapper;
import com.example.mapper.KnowledgePointMapper;
import com.example.mapper.PublishMapper;
import com.example.mapper.QuestionMapper;
import com.example.mapper.SubQuestionMapper;
import com.example.util.AiResultParser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class QuestionBankService {

    private final QuestionMapper questionMapper;
    private final KnowledgePointMapper knowledgePointMapper;
    private final SubQuestionMapper subQuestionMapper;
    private final PublishMapper publishMapper;
    private final FavoriteMapper favoriteMapper;
    private final AgentClient agentClient;

    public QuestionBankService(QuestionMapper questionMapper,
                               KnowledgePointMapper knowledgePointMapper,
                               SubQuestionMapper subQuestionMapper,
                               PublishMapper publishMapper,
                               FavoriteMapper favoriteMapper,
                               AgentClient agentClient) {
        this.questionMapper = questionMapper;
        this.knowledgePointMapper = knowledgePointMapper;
        this.subQuestionMapper = subQuestionMapper;
        this.publishMapper = publishMapper;
        this.favoriteMapper = favoriteMapper;
        this.agentClient = agentClient;
    }

    /** 上传并智能出题：识别 + 拆知识点 + 分级生成小问题 */
    @Transactional
    public Question uploadAndDecompose(UploadQuestionRequest req) {
        Question q = new Question();
        q.setUploaderId(req.getUploaderId());
        q.setUploaderName(req.getUploaderName());
        q.setSubject(req.getSubject());
        q.setSourceType(req.getSourceType());
        q.setOriginalAnswer(req.getOriginalAnswer());
        q.setCreateTime(LocalDateTime.now());
        q.setUpdateTime(LocalDateTime.now());
        q.setStatus("DRAFT");

        String aiResult;
        if ("IMAGE".equals(req.getSourceType())) {
            q.setSourceImage(req.getImageBase64());
            q.setSourceText(req.getText());
            aiResult = agentClient.decomposeImage(req.getText(), req.getImageBase64());
        } else if ("DOC".equals(req.getSourceType())) {
            q.setSourceText(req.getText());
            q.setRecognizedText(req.getText());
            aiResult = agentClient.decomposeText(req.getText());
        } else {
            q.setSourceText(req.getText());
            aiResult = agentClient.decomposeText(req.getText());
        }
        q.setAiResult(aiResult);
        questionMapper.insert(q);
        persistDecomposed(q.getId(), aiResult);
        return q;
    }

    /** 把 AI 拆分结果重新解析并落库（用于首次生成与重新生成） */
    @Transactional
    public void persistDecomposed(Long questionId, String aiResult) {
        // 清空旧的
        knowledgePointMapper.delete(new LambdaQueryWrapper<KnowledgePoint>()
                .eq(KnowledgePoint::getQuestionId, questionId));
        subQuestionMapper.delete(new LambdaQueryWrapper<SubQuestion>()
                .eq(SubQuestion::getQuestionId, questionId));

        List<ParsedAiItem> items = AiResultParser.parse(aiResult);
        int i = 0;
        for (ParsedAiItem item : items) {
            KnowledgePoint kp = new KnowledgePoint();
            kp.setQuestionId(questionId);
            kp.setName(item.getKnowledgeName());
            kp.setDifficulty(item.getDifficulty());
            kp.setSortOrder(i);
            knowledgePointMapper.insert(kp);

            SubQuestion sq = new SubQuestion();
            sq.setQuestionId(questionId);
            sq.setKnowledgePointId(kp.getId());
            sq.setContent(item.getContent());
            sq.setSolution(item.getSolution());
            sq.setAnswer(item.getAnswer());
            sq.setDifficulty(item.getDifficulty());
            sq.setSortOrder(i);
            subQuestionMapper.insert(sq);
            i++;
        }
    }

    /** 人工校准：用编辑后的内容整体替换知识点与小问题 */
    @Transactional
    public void calibrate(CalibrateRequest req) {
        Long questionId = req.getQuestionId();
        knowledgePointMapper.delete(new LambdaQueryWrapper<KnowledgePoint>()
                .eq(KnowledgePoint::getQuestionId, questionId));
        subQuestionMapper.delete(new LambdaQueryWrapper<SubQuestion>()
                .eq(SubQuestion::getQuestionId, questionId));

        // 重新插入知识点，记录 难度 -> 新ID 的映射
        Map<String, Long> difficultyToKpId = new LinkedHashMap<>();
        int kpOrder = 0;
        if (req.getKnowledgePoints() != null) {
            for (KnowledgePoint kp : req.getKnowledgePoints()) {
                kp.setId(null);
                kp.setQuestionId(questionId);
                kp.setSortOrder(kpOrder++);
                knowledgePointMapper.insert(kp);
                if (kp.getDifficulty() != null) {
                    difficultyToKpId.putIfAbsent(kp.getDifficulty(), kp.getId());
                }
            }
        }

        int sqOrder = 0;
        if (req.getSubQuestions() != null) {
            for (SubQuestion sq : req.getSubQuestions()) {
                sq.setId(null);
                sq.setQuestionId(questionId);
                sq.setSortOrder(sqOrder++);
                Long kpId = difficultyToKpId.get(sq.getDifficulty());
                if (kpId == null && !difficultyToKpId.isEmpty()) {
                    kpId = difficultyToKpId.values().iterator().next();
                }
                sq.setKnowledgePointId(kpId);
                subQuestionMapper.insert(sq);
            }
        }

        Question q = questionMapper.selectById(questionId);
        if (q != null) {
            q.setStatus("CALIBRATED");
            q.setUpdateTime(LocalDateTime.now());
            questionMapper.updateById(q);
        }
    }

    /** 发布题目到班级 */
    @Transactional
    public void publish(Long questionId, String className, Long publisherId, String publisherName) {
        Question q = questionMapper.selectById(questionId);
        if (q == null) throw new RuntimeException("题目不存在");
        q.setStatus("PUBLISHED");
        q.setUpdateTime(LocalDateTime.now());
        questionMapper.updateById(q);

        Publish p = new Publish();
        p.setQuestionId(questionId);
        p.setClassName(className);
        p.setPublisherId(publisherId);
        p.setPublisherName(publisherName);
        p.setPublishTime(LocalDateTime.now());
        publishMapper.insert(p);
    }

    public Map<String, Object> detail(Long id) {
        Question q = questionMapper.selectById(id);
        if (q == null) return null;
        List<KnowledgePoint> kps = knowledgePointMapper.selectList(
                new LambdaQueryWrapper<KnowledgePoint>()
                        .eq(KnowledgePoint::getQuestionId, id)
                        .orderByAsc(KnowledgePoint::getSortOrder));
        List<SubQuestion> sqs = subQuestionMapper.selectList(
                new LambdaQueryWrapper<SubQuestion>()
                        .eq(SubQuestion::getQuestionId, id)
                        .orderByAsc(SubQuestion::getSortOrder));
        Map<String, Object> map = new HashMap<>();
        map.put("question", q);
        map.put("knowledgePoints", kps);
        map.put("subQuestions", sqs);
        return map;
    }

    public List<Question> search(String keyword, String difficulty, String subject, Long uploaderId) {
        LambdaQueryWrapper<Question> w = new LambdaQueryWrapper<>();
        if (subject != null && !subject.isBlank()) w.eq(Question::getSubject, subject);
        if (uploaderId != null) w.eq(Question::getUploaderId, uploaderId);
        if (keyword != null && !keyword.isBlank()) {
            w.and(qw -> qw.like(Question::getSourceText, keyword)
                    .or().like(Question::getRecognizedText, keyword)
                    .or().like(Question::getAiResult, keyword));
        }
        w.orderByDesc(Question::getCreateTime);
        List<Question> list = questionMapper.selectList(w);

        // 难度过滤：保留包含该难度子题的题目
        if (difficulty != null && !difficulty.isBlank()) {
            List<Question> filtered = new ArrayList<>();
            for (Question q : list) {
                Long cnt = subQuestionMapper.selectCount(new LambdaQueryWrapper<SubQuestion>()
                        .eq(SubQuestion::getQuestionId, q.getId())
                        .eq(SubQuestion::getDifficulty, difficulty));
                if (cnt != null && cnt > 0) filtered.add(q);
            }
            return filtered;
        }
        return list;
    }

    @Transactional
    public void delete(Long id) {
        knowledgePointMapper.delete(new LambdaQueryWrapper<KnowledgePoint>().eq(KnowledgePoint::getQuestionId, id));
        subQuestionMapper.delete(new LambdaQueryWrapper<SubQuestion>().eq(SubQuestion::getQuestionId, id));
        publishMapper.delete(new LambdaQueryWrapper<Publish>().eq(Publish::getQuestionId, id));
        favoriteMapper.delete(new LambdaQueryWrapper<Favorite>().eq(Favorite::getQuestionId, id));
        questionMapper.deleteById(id);
    }

    /** 重新生成：再次调用 agent 覆盖原结果 */
    @Transactional
    public Question regenerate(Long id) {
        Question q = questionMapper.selectById(id);
        if (q == null) throw new RuntimeException("题目不存在");
        String aiResult;
        if ("IMAGE".equals(q.getSourceType()) && q.getSourceImage() != null) {
            aiResult = agentClient.decomposeImage(q.getSourceText(), q.getSourceImage());
        } else {
            aiResult = agentClient.decomposeText(q.getSourceText());
        }
        q.setAiResult(aiResult);
        q.setUpdateTime(LocalDateTime.now());
        questionMapper.updateById(q);
        persistDecomposed(id, aiResult);
        return q;
    }

    // ===== 收藏 =====
    public void addFavorite(Long userId, Long questionId, String category) {
        Long cnt = favoriteMapper.selectCount(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId).eq(Favorite::getQuestionId, questionId));
        if (cnt == null || cnt == 0) {
            Favorite f = new Favorite();
            f.setUserId(userId);
            f.setQuestionId(questionId);
            f.setCategory(category);
            f.setCreateTime(LocalDateTime.now());
            favoriteMapper.insert(f);
        } else if (category != null && !category.isBlank()) {
            // 已收藏时更新分类
            Favorite update = new Favorite();
            update.setCategory(category);
            favoriteMapper.update(update, new LambdaQueryWrapper<Favorite>()
                    .eq(Favorite::getUserId, userId).eq(Favorite::getQuestionId, questionId));
        }
    }

    public void removeFavorite(Long userId, Long questionId) {
        favoriteMapper.delete(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId).eq(Favorite::getQuestionId, questionId));
    }

    public List<Question> listFavorites(Long userId, String category) {
        LambdaQueryWrapper<Favorite> w = new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId);
        if (category != null && !category.isBlank()) {
            w.eq(Favorite::getCategory, category);
        }
        w.orderByDesc(Favorite::getCreateTime);
        List<Favorite> favs = favoriteMapper.selectList(w);
        List<Question> result = new ArrayList<>();
        for (Favorite f : favs) {
            Question q = questionMapper.selectById(f.getQuestionId());
            if (q != null) result.add(q);
        }
        return result;
    }

    /** 该用户已用的收藏分类 */
    public List<String> listFavoriteCategories(Long userId) {
        List<Favorite> favs = favoriteMapper.selectList(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId).orderByDesc(Favorite::getCreateTime));
        List<String> cats = new ArrayList<>();
        for (Favorite f : favs) {
            String c = f.getCategory();
            if (c != null && !c.isBlank() && !cats.contains(c)) cats.add(c);
        }
        return cats;
    }
}