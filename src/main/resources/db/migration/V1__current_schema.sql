-- Immutable V1 snapshot: MyBatis schema as of 2026-09-07.
-- Existing databases must pass the guarded baseline; do not replay this script manually.

CREATE TABLE sgtransit_employee (
    emp_no       BIGINT       NOT NULL AUTO_INCREMENT COMMENT '회원번호',
    emp_id       VARCHAR(50)  NOT NULL COMMENT '아이디',
    emp_password VARCHAR(100) NOT NULL COMMENT 'BCrypt 비밀번호',
    emp_name     VARCHAR(50)  NOT NULL COMMENT '이름',
    emp_email    VARCHAR(100) NOT NULL COMMENT '이메일',
    emp_phone    VARCHAR(20)  NOT NULL COMMENT '휴대전화',
    emp_address  VARCHAR(255) NULL COMMENT '주소',
    emp_role     CHAR(1)      NOT NULL DEFAULT 'U' COMMENT '사용자 U, 관리자 A, 마스터 M',
    emp_status   CHAR(1)      NOT NULL DEFAULT 'Y' COMMENT '이용중 Y, 탈퇴 N',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '가입일시',
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    PRIMARY KEY (emp_no),
    UNIQUE KEY uk_emp_id (emp_id),
    UNIQUE KEY uk_emp_email (emp_email)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

CREATE TABLE sgtransit_signup_consent (
    consent_no         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '동의 이력 번호',
    emp_no             BIGINT      NOT NULL COMMENT '회원번호',
    terms_version      VARCHAR(20) NOT NULL COMMENT '이용약관 버전',
    privacy_version    VARCHAR(20) NOT NULL COMMENT '개인정보 동의문 버전',
    terms_agreed_at    DATETIME    NOT NULL COMMENT '이용약관 동의일시',
    privacy_agreed_at  DATETIME    NOT NULL COMMENT '개인정보 동의일시',
    age_confirmed_at   DATETIME    NOT NULL COMMENT '만 14세 이상 확인일시',
    created_at         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록일시',
    PRIMARY KEY (consent_no),
    KEY idx_signup_consent_emp_no (emp_no),
    CONSTRAINT fk_signup_consent_employee
        FOREIGN KEY (emp_no) REFERENCES sgtransit_employee (emp_no)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

CREATE TABLE sgtransit_phone_verification (
    verification_no        BIGINT       NOT NULL AUTO_INCREMENT,
    phone                  VARCHAR(20)  NOT NULL,
    challenge_scope        CHAR(64)     NULL,
    code_hash              VARCHAR(100) NOT NULL,
    verification_token_hash CHAR(64)    NULL,
    failed_attempts        INT          NOT NULL DEFAULT 0,
    requested_at           DATETIME     NOT NULL,
    code_expires_at        DATETIME     NOT NULL,
    verified_at            DATETIME     NULL,
    token_expires_at       DATETIME     NULL,
    used_at                DATETIME     NULL,
    PRIMARY KEY (verification_no),
    UNIQUE KEY uk_phone_verification_token (verification_token_hash),
    KEY idx_phone_verification_latest (phone, requested_at)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

CREATE TABLE sgtransit_complaint (
    complaint_no       BIGINT       NOT NULL AUTO_INCREMENT COMMENT '민원번호',
    emp_no             BIGINT       NOT NULL COMMENT '접수 회원번호',
    emp_id             VARCHAR(20)  NOT NULL COMMENT '접수 당시 아이디',
    emp_name           VARCHAR(50)  NOT NULL COMMENT '접수 당시 이름',
    emp_phone          VARCHAR(20)  NOT NULL COMMENT '접수 당시 휴대전화',
    complaint_category VARCHAR(20)  NOT NULL COMMENT 'PRAISE 칭찬, COMPLAINT 불편, LOST 분실물',
    complaint_title    VARCHAR(100) NOT NULL COMMENT '제목',
    complaint_content  TEXT         NOT NULL COMMENT '내용',
    complaint_password VARCHAR(100) NULL COMMENT '게시글 열람 BCrypt 비밀번호',
    complaint_status   VARCHAR(20)  NOT NULL DEFAULT 'CHECKING' COMMENT 'CHECKING 확인중, PROCESSING 처리중, COMPLETED 답변완료',
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

CREATE TABLE sgtransit_complaint_attachment (
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

CREATE TABLE sgtransit_complaint_answer (
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
CREATE TABLE sgtransit_complaint_answer_attachment (
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


CREATE TABLE sgtransit_main_banner (
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

CREATE TABLE sgtransit_notice (
    notice_no       BIGINT       NOT NULL AUTO_INCREMENT COMMENT '공지사항 번호',
    admin_emp_no    BIGINT       NOT NULL COMMENT '등록 관리자 회원번호',
    admin_name      VARCHAR(50)  NOT NULL COMMENT '등록 관리자 이름',
    notice_category VARCHAR(20)  NOT NULL COMMENT 'GENERAL, SYSTEM, SERVICE',
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

CREATE TABLE sgtransit_notice_image (
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

CREATE TABLE sgtransit_partner (
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

CREATE TABLE sgtransit_route_operation (
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

CREATE TABLE sgtransit_ddokbus_guide_image (
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


CREATE TABLE sgtransit_abuse_limit (
    bucket_key VARCHAR(100) NOT NULL,
    hits INT NOT NULL DEFAULT 0,
    reset_at DATETIME NOT NULL,
    PRIMARY KEY (bucket_key),
    KEY idx_abuse_limit_reset (reset_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
