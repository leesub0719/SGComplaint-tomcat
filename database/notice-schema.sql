USE bus_complaint_db;

CREATE TABLE IF NOT EXISTS sgtransit_notice (
    notice_no       BIGINT       NOT NULL AUTO_INCREMENT COMMENT '공지사항 번호',
    admin_emp_no    BIGINT       NOT NULL COMMENT '등록 관리자 회원번호',
    admin_name      VARCHAR(50)  NOT NULL COMMENT '등록 관리자 이름',
    notice_category VARCHAR(20)  NOT NULL COMMENT 'GENERAL 일반, SYSTEM 점검, SERVICE 변경',
    notice_title    VARCHAR(100) NOT NULL COMMENT '공지사항 제목',
    notice_content  TEXT         NOT NULL COMMENT '공지사항 내용',
    is_pinned       CHAR(1)      NOT NULL DEFAULT 'N' COMMENT '상단 고정 여부 Y/N',
    is_popup        CHAR(1)      NOT NULL DEFAULT 'N' COMMENT '메인 팝업 노출 여부 Y/N',
    view_count      BIGINT       NOT NULL DEFAULT 0 COMMENT '조회수',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록일시',
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '수정일시',
    PRIMARY KEY (notice_no),
    KEY idx_notice_pinned_created (is_pinned, created_at),
    KEY idx_notice_popup_created (is_popup, created_at),
    CONSTRAINT fk_notice_admin FOREIGN KEY (admin_emp_no)
        REFERENCES sgtransit_employee (emp_no)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

SHOW CREATE TABLE sgtransit_notice;

CREATE TABLE IF NOT EXISTS sgtransit_notice_image (
    notice_image_no BIGINT       NOT NULL AUTO_INCREMENT COMMENT '공지사항 본문 이미지 번호',
    notice_no       BIGINT       NOT NULL COMMENT '공지사항 번호',
    original_name   VARCHAR(255) NOT NULL COMMENT '원본 파일명',
    stored_name     VARCHAR(255) NOT NULL COMMENT '서버 저장 파일명',
    file_path       VARCHAR(500) NOT NULL COMMENT '서버 상대경로',
    content_type    VARCHAR(100) NOT NULL COMMENT '이미지 MIME 형식',
    file_size       BIGINT       NOT NULL COMMENT '파일 크기(bytes)',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록일시',
    PRIMARY KEY (notice_image_no),
    KEY idx_notice_image_notice_no (notice_no),
    CONSTRAINT fk_notice_image_notice FOREIGN KEY (notice_no)
        REFERENCES sgtransit_notice (notice_no) ON DELETE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

SHOW CREATE TABLE sgtransit_notice_image;
