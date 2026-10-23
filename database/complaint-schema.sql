USE bus_complaint_db;

CREATE TABLE IF NOT EXISTS sgtransit_complaint (
    complaint_no       BIGINT       NOT NULL AUTO_INCREMENT COMMENT '민원번호',
    emp_no             BIGINT       NOT NULL COMMENT '접수 회원번호',
    emp_id             VARCHAR(20)  NOT NULL COMMENT '접수 당시 아이디',
    emp_name           VARCHAR(50)  NOT NULL COMMENT '접수 당시 이름',
    emp_phone          VARCHAR(20)  NOT NULL COMMENT '접수 당시 휴대전화',
    complaint_category VARCHAR(20)  NOT NULL COMMENT 'PRAISE 칭찬, COMPLAINT 불편, LOST 분실물',
    complaint_title    VARCHAR(100) NOT NULL COMMENT '제목',
    complaint_content  TEXT         NOT NULL COMMENT '내용',
    complaint_password VARCHAR(100) NOT NULL COMMENT '게시글 열람 BCrypt 비밀번호',
    complaint_status   VARCHAR(20)  NOT NULL DEFAULT 'CHECKING' COMMENT 'CHECKING 확인중, PROCESSING 처리중, COMPLETED 답변완료',
    version            BIGINT       NOT NULL DEFAULT 0 COMMENT '동시 수정 충돌 감지용 버전',
    created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '접수일시',
    updated_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '수정일시',
    PRIMARY KEY (complaint_no),
    KEY idx_complaint_emp_no (emp_no),
    KEY idx_complaint_emp_created (emp_no, created_at),
    KEY idx_complaint_status_created (complaint_status, created_at),
    KEY idx_complaint_category_created (complaint_category, created_at),
    CONSTRAINT fk_complaint_employee
        FOREIGN KEY (emp_no) REFERENCES sgtransit_employee (emp_no)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sgtransit_complaint_attachment (
    attachment_no BIGINT       NOT NULL AUTO_INCREMENT COMMENT '첨부파일번호',
    complaint_no  BIGINT       NOT NULL COMMENT '민원번호',
    original_name VARCHAR(255) NOT NULL COMMENT '원본 파일명',
    stored_name   VARCHAR(255) NOT NULL COMMENT '서버 저장 파일명',
    file_path     VARCHAR(500) NOT NULL COMMENT '서버 상대경로',
    content_type  VARCHAR(100) NULL COMMENT 'MIME 형식',
    file_size     BIGINT       NOT NULL COMMENT '파일 크기(bytes)',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록일시',
    PRIMARY KEY (attachment_no),
    KEY idx_attachment_complaint_no (complaint_no),
    CONSTRAINT fk_attachment_complaint
        FOREIGN KEY (complaint_no) REFERENCES sgtransit_complaint (complaint_no)
        ON DELETE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

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
