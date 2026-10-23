-- 똑버스 이미지 안내 기능 배포 전에 운영 DB(sgcomplaint)에서 한 번 실행합니다.
USE sgcomplaint;

CREATE TABLE IF NOT EXISTS sgtransit_ddokbus_guide_image (
    image_no     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '똑버스 안내 이미지 번호',
    original_name VARCHAR(255) NOT NULL COMMENT '원본 파일명',
    stored_name   VARCHAR(255) NOT NULL COMMENT '서버 저장 파일명',
    file_path     VARCHAR(500) NOT NULL COMMENT '서버 상대경로',
    content_type  VARCHAR(100) NOT NULL COMMENT '이미지 MIME 형식',
    file_size     BIGINT       NOT NULL COMMENT '파일 크기(bytes)',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록일시',
    PRIMARY KEY (image_no),
    KEY idx_ddokbus_guide_image_no (image_no)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;
