USE bus_complaint_db;

CREATE TABLE IF NOT EXISTS sgtransit_partner (
    partner_no    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '협력업체 번호',
    partner_name  VARCHAR(100) NOT NULL COMMENT '협력업체 이름',
    partner_phone VARCHAR(30)  NOT NULL COMMENT '전화번호',
    partner_site  VARCHAR(500) NOT NULL DEFAULT '' COMMENT '사이트 URL',
    partner_notes TEXT         NOT NULL COMMENT '기타사항',
    partner_status CHAR(1)     NOT NULL DEFAULT 'Y' COMMENT '노출 Y, 삭제 N',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록일시',
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '수정일시',
    PRIMARY KEY (partner_no),
    KEY idx_partner_name (partner_name),
    KEY idx_partner_status_created (partner_status, created_at),
    KEY idx_partner_created_at (created_at)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

SELECT partner_no, partner_name, partner_phone, partner_site, partner_status, created_at
  FROM sgtransit_partner
 ORDER BY partner_no DESC;
