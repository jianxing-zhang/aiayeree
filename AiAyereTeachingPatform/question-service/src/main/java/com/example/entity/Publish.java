package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_publish")
public class Publish {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long questionId;
    private String className;
    private Long publisherId;
    private String publisherName;
    private LocalDateTime publishTime;
}