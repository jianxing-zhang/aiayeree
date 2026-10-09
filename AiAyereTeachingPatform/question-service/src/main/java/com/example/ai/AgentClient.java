package com.example.ai;

import com.example.config.AiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通过 HTTP 调用 Python langgraph agent 服务（默认 http://127.0.0.1:2024）。
 * 使用无状态接口 POST /runs/wait，同步返回最终结果。
 */
@Service
public class AgentClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final AiProperties props;

    public AgentClient(RestTemplate restTemplate, ObjectMapper objectMapper, AiProperties props) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.props = props;
    }

    /** 文本题目：知识点拆分 + 分级出题 */
    public String decomposeText(String text) {
        Map<String, Object> content = new HashMap<>();
        content.put("role", "user");
        content.put("content", text);

        Map<String, Object> payload = new HashMap<>();
        payload.put("assistant_id", props.getAssistantId());
        payload.put("input", Map.of("messages", List.of(content)));
        payload.put("stream_mode", "values");
        return call(payload);
    }

    /** 图片题目：qwen-vl-plus 直接读图（OCR + 拆分一体） */
    public String decomposeImage(String prompt, String base64) {
        List<Object> chunks = new ArrayList<>();
        Map<String, Object> textChunk = new HashMap<>();
        textChunk.put("type", "text");
        textChunk.put("text", prompt == null || prompt.isBlank()
                ? "请识别下图题目并完成知识拆分与分级出题" : prompt);
        chunks.add(textChunk);

        Map<String, Object> imageChunk = new HashMap<>();
        imageChunk.put("type", "image");
        imageChunk.put("data", base64);
        chunks.add(imageChunk);

        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", chunks);

        Map<String, Object> payload = new HashMap<>();
        payload.put("assistant_id", props.getAssistantId());
        payload.put("input", Map.of("messages", List.of(message)));
        payload.put("stream_mode", "values");
        return call(payload);
    }

    /** 按需生成某个知识点主题的详细内容 */
    public String generateOutlineTopic(String questionText, String topic) {
        String userMsg = String.format(
                "请作为知识点讲解助手，针对以下题目涉及的主题「%s」，列出核心知识点（定义、公式、求解方法）。"
                + "每个知识点用 - 开头，用纯文本格式，不要做三级拆分。题目原文：%s",
                topic, questionText == null ? "" : questionText
        );
        Map<String, Object> content = new HashMap<>();
        content.put("role", "user");
        content.put("content", userMsg);

        Map<String, Object> payload = new HashMap<>();
        payload.put("assistant_id", props.getAssistantId());
        payload.put("input", Map.of("messages", List.of(content)));
        payload.put("stream_mode", "values");
        return call(payload);
    }

    private String call(Map<String, Object> payload) {
        String url = props.getBaseUrl() + "/runs/wait";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        ResponseEntity<String> resp = restTemplate.postForEntity(url, entity, String.class);
        return extractAiContent(resp.getBody());
    }

    /** 从 LangGraph 返回的 {messages:[...]} 中取出最后一条 AI 消息的文本内容 */
    private String extractAiContent(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode messages = root.get("messages");
            if (messages == null || !messages.isArray()) {
                return body;
            }
            String aiText = null;
            for (JsonNode m : messages) {
                if ("ai".equals(m.path("type").asText())) {
                    JsonNode c = m.get("content");
                    if (c != null) {
                        aiText = nodeToText(c);
                    }
                }
            }
            return aiText != null ? aiText : body;
        } catch (Exception e) {
            return body;
        }
    }

    private String nodeToText(JsonNode c) {
        if (c.isTextual()) return c.asText();
        if (c.isArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode n : c) {
                if (n.has("text")) sb.append(n.get("text").asText()).append("\n");
                else if (n.isTextual()) sb.append(n.asText()).append("\n");
            }
            return sb.toString();
        }
        return c.toString();
    }
}