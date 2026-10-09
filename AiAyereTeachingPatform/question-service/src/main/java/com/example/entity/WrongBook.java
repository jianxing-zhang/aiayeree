package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_wrong_book")
public class WrongBook {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private Long subQuestionId;
    private Integer wrongCount;
    private String reviewStatus;  // NOT_REVIEWED / REVIEWED
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}