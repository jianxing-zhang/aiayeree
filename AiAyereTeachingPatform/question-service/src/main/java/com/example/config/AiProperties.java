package com.example.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 对接 Python langgraph agent 服务的配置项
 */
@Data
@ConfigurationProperties(prefix = "ai.agent")
public class AiProperties {
    /** agent 服务地址，例如 http://127.0.0.1:2024 */
    private String baseUrl = "http://127.0.0.1:2024";
    /** langgraph.json 中配置的 graph 名称 */
    private String assistantId = "chief_agent";
    /** 模型名（透传，供日志与后续扩展） */
    private String model = "qwen-vl-plus";
    private int connectTimeoutMs = 30000;
    private int readTimeoutMs = 180000;
}