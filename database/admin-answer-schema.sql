USE bus_complaint_db;

CREATE TABLE IF NOT EXISTS sgtransit_complaint_answer (
    answer_no      BIGINT      NOT NULL AUTO_INCREMENT COMMENT '답변번호',
    complaint_no   BIGINT      NOT NULL COMMENT '민원번호',
    admin_emp_no   BIGINT      NOT NULL COMMENT '답변 관리자 회원번호',
    admin_name     VARCHAR(50) NOT NULL COMMENT '답변 당시 관리자 이름',
    answer_content TEXT        NOT NULL COMMENT '답변 내용',
    created_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '최초 답변일시',
    updated_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '답변 수정일시',
    PRIMARY KEY (answer_no),
    UNIQUE KEY uk_answer_complaint_no (complaint_no),
    KEY idx_answer_admin_emp_no (admin_emp_no),
    CONSTRAINT fk_answer_complaint
        FOREIGN KEY (complaint_no) REFERENCES sgtransit_complaint (complaint_no)
        ON DELETE CASCADE,
    CONSTRAINT fk_answer_admin
        FOREIGN KEY (admin_emp_no) REFERENCES sgtransit_employee (emp_no)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;
+

CREATE TABLE IF NOT EXISTS sgtransit_complaint_answer_attachment (
    answer_attachment_no BIGINT       NOT NULL AUTO_INCREMENT COMMENT '답변 첨부파일번호',
    answer_no            BIGINT       NOT NULL COMMENT '답변번호',
    original_name        VARCHAR(255) NOT NULL COMMENT '원본 파일명',
    stored_name          VARCHAR(255) NOT NULL COMMENT '서버 저장 파일명',
    file_path            VARCHAR(500) NOT NULL COMMENT '서버 상대경로',
    content_type         VARCHAR(100) NULL COMMENT 'MIME 형식',
    file_size            BIGINT       NOT NULL COMMENT '파일 크기(bytes)',
    created_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록일시',
    PRIMARY KEY (answer_attachment_no),
    KEY idx_answer_attachment_answer_no (answer_no),
    CONSTRAINT fk_answer_attachment_answer
        FOREIGN KEY (answer_no) REFERENCES sgtransit_complaint_answer (answer_no)
        ON DELETE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;
