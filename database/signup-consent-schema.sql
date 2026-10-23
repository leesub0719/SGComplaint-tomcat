-- 회원가입 전 필수 동의 이력을 저장하기 위한 운영 DB 반영 스크립트입니다.
USE sgcomplaint;

CREATE TABLE IF NOT EXISTS sgtransit_signup_consent (
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

SHOW CREATE TABLE sgtransit_signup_consent;
