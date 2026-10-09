-- 智能题库系统 数据库初始化脚本
-- 数据库：ai_teaching_db（与 user-service 共用）
-- 执行方式：mysql -uroot -p123456 < schema.sql

CREATE DATABASE IF NOT EXISTS ai_teaching_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE ai_teaching_db;

-- 用户表：用户服务(t_user)
CREATE TABLE IF NOT EXISTS t_user (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(64)  COMMENT '用户名',
    password    VARCHAR(64)  COMMENT '密码(明文)',
    role        VARCHAR(20) DEFAULT 'student' COMMENT '角色：student/teacher/admin',
    phone       VARCHAR(20)  COMMENT '手机号',
    subject     VARCHAR(32)  COMMENT '科目',
    create_time DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 题目表：老师/学生上传的原始题目
CREATE TABLE IF NOT EXISTS t_question (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    uploader_id     BIGINT          COMMENT '上传者用户ID',
    uploader_name   VARCHAR(64)     COMMENT '上传者姓名',
    subject         VARCHAR(32)     COMMENT '学科：物理/数学',
    source_type     VARCHAR(20)     COMMENT '来源类型：TEXT/IMAGE/DOC',
    source_image    LONGTEXT        COMMENT '上传图片(base64)或文件路径',
    source_text     TEXT            COMMENT '题目原文/文本录入',
    original_answer TEXT            COMMENT '参考答案(可选)',
    recognized_text TEXT            COMMENT 'OCR 识别出的文本',
    ai_result       LONGTEXT        COMMENT 'AI 拆分原始结果(markdown)',
    status          VARCHAR(20) DEFAULT 'DRAFT' COMMENT '状态：DRAFT/CALIBRATED/PUBLISHED',
    create_time     DATETIME,
    update_time     DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 知识点表：AI 拆分出的知识点
CREATE TABLE IF NOT EXISTS t_knowledge_point (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT          COMMENT '所属题目ID',
    name        VARCHAR(128)    COMMENT '知识点名称',
    difficulty  VARCHAR(20)     COMMENT '难度：基础/进阶/挑战',
    sort_order  INT DEFAULT 0   COMMENT '排序'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 小问题表：每个知识点下按难度生成的小问题
CREATE TABLE IF NOT EXISTS t_sub_question (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id        BIGINT       COMMENT '所属题目ID',
    knowledge_point_id BIGINT       COMMENT '所属知识点ID',
    content            TEXT         COMMENT '题干',
    options            TEXT         COMMENT '选项(JSON，如选择题)',
    solution           TEXT         COMMENT '解析/解法',
    answer             TEXT         COMMENT '参考答案',
    difficulty         VARCHAR(20)  COMMENT '难度：基础/进阶/挑战',
    sort_order         INT DEFAULT 0 COMMENT '排序'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 答题记录表：学生作答记录
CREATE TABLE IF NOT EXISTS t_answer_record (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id      BIGINT      COMMENT '学生用户ID',
    sub_question_id BIGINT      COMMENT '小问题ID',
    answer          TEXT        COMMENT '学生答案',
    correct         TINYINT(1)  COMMENT '是否正确：1对 0错',
    answer_time     DATETIME    COMMENT '作答时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 错题本表
CREATE TABLE IF NOT EXISTS t_wrong_book (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id      BIGINT      COMMENT '学生用户ID',
    sub_question_id BIGINT      COMMENT '小问题ID',
    wrong_count     INT DEFAULT 1 COMMENT '错误次数',
    review_status   VARCHAR(20) DEFAULT 'NOT_REVIEWED' COMMENT '复习状态：NOT_REVIEWED/REVIEWED',
    create_time     DATETIME,
    update_time     DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 发布记录表：老师把题目发布到班级
CREATE TABLE IF NOT EXISTS t_publish (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id    BIGINT      COMMENT '题目ID',
    class_name     VARCHAR(64) COMMENT '班级',
    publisher_id   BIGINT      COMMENT '发布老师ID',
    publisher_name VARCHAR(64) COMMENT '发布老师姓名',
    publish_time   DATETIME    COMMENT '发布时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 收藏表
CREATE TABLE IF NOT EXISTS t_favorite (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT   COMMENT '用户ID',
    question_id BIGINT   COMMENT '题目ID',
    category    VARCHAR(64) COMMENT '收藏分类',
    create_time DATETIME COMMENT '收藏时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;