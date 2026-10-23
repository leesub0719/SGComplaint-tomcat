-- 노선운행안내 기능 배포 전에 운영 DB(sgcomplaint)에서 한 번 실행합니다.
USE sgcomplaint;

CREATE TABLE IF NOT EXISTS sgtransit_route_operation (
    route_no          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '운행안내 번호',
    route_type        VARCHAR(20)  NOT NULL COMMENT 'VILLAGE 마을버스, DDOK 똑버스',
    bus_name          VARCHAR(100) NOT NULL COMMENT '버스 이름',
    terminal_info     VARCHAR(200) NOT NULL COMMENT '기점-종점',
    dispatch_interval VARCHAR(200) NOT NULL COMMENT '배차간격',
    inquiry_phone     VARCHAR(100) NOT NULL COMMENT '문의전화',
    route_url         VARCHAR(500) NOT NULL COMMENT '노선안내 링크',
    route_status      CHAR(1)      NOT NULL DEFAULT 'Y' COMMENT '노출 Y, 삭제 N',
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록일시',
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '수정일시',
    PRIMARY KEY (route_no),
    KEY idx_route_operation_type_status_no (route_type, route_status, route_no),
    KEY idx_route_operation_status_no (route_status, route_no)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

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
