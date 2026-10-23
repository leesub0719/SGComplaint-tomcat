USE bus_complaint_db;


CREATE TABLE IF NOT EXISTS sgtransit_main_banner (
    banner_no     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '메인 배너번호',
    original_name VARCHAR(255) NOT NULL COMMENT '원본 파일명',
    stored_name   VARCHAR(255) NOT NULL COMMENT '서버 저장 파일명',
    file_path     VARCHAR(500) NOT NULL COMMENT '서버 상대경로',
    content_type  VARCHAR(100) NOT NULL COMMENT '이미지 MIME 형식',
    file_size     BIGINT       NOT NULL COMMENT '파일 크기(bytes)',
    display_order INT          NOT NULL COMMENT '노출 순서',
    created_by    VARCHAR(20)  NOT NULL COMMENT '등록 관리자 아이디',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록일시',
    PRIMARY KEY (banner_no),
    KEY idx_main_banner_display_order (display_order, banner_no)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;
