package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_sub_question")
public class SubQuestion {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long questionId;
    private Long knowledgePointId;
    private String content;      // 题干
    private String options;      // 选项(JSON)
    private String solution;     // 解析/解法
    private String answer;       // 参考答案
    private String difficulty;   // 基础 / 进阶 / 挑战
    private Integer sortOrder;
}