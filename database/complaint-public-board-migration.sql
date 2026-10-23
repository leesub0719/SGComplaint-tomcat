-- 기존 sgtransit_complaint 테이블을 공개 비밀번호 게시판 구조로 변경합니다.
-- 반드시 적용 전 DB를 백업하세요.
USE bus_complaint_db;

SET @password_column_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'sgtransit_complaint'
       AND COLUMN_NAME = 'complaint_password'
);
SET @password_column_sql = IF(
    @password_column_exists = 0,
    'ALTER TABLE sgtransit_complaint ADD COLUMN complaint_password VARCHAR(100) NULL COMMENT ''게시글 열람 BCrypt 비밀번호'' AFTER complaint_content',
    'SELECT ''complaint_password column already exists'''
);
PREPARE password_column_statement FROM @password_column_sql;
EXECUTE password_column_statement;
DEALLOCATE PREPARE password_column_statement;

-- 이전 분류를 새 공개 게시판 분류로 이동합니다.
UPDATE sgtransit_complaint
   SET complaint_category = 'COMPLAINT'
 WHERE complaint_category IN ('DRIVER', 'BUS');

UPDATE sgtransit_complaint
   SET complaint_category = 'LOST'
 WHERE complaint_category = 'GENERAL';

ALTER TABLE sgtransit_complaint
    MODIFY COLUMN complaint_category VARCHAR(20) NOT NULL
    COMMENT 'PRAISE 칭찬, COMPLAINT 불편, LOST 분실물';

SET @category_index_exists = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'sgtransit_complaint'
       AND INDEX_NAME = 'idx_complaint_category_created'
);
SET @category_index_sql = IF(
    @category_index_exists = 0,
    'CREATE INDEX idx_complaint_category_created ON sgtransit_complaint (complaint_category, created_at)',
    'SELECT ''idx_complaint_category_created index already exists'''
);
PREPARE category_index_statement FROM @category_index_sql;
EXECUTE category_index_statement;
DEALLOCATE PREPARE category_index_statement;

-- 기존 게시글은 당시 비밀번호를 입력받지 않았으므로 NULL로 남습니다.
-- 새로 등록되는 게시글부터 BCrypt 비밀번호가 자동 저장됩니다.
SELECT complaint_no, complaint_category,
       IF(complaint_password IS NULL, 'PASSWORD_RESET_REQUIRED', 'READY') AS password_state
  FROM sgtransit_complaint
 ORDER BY complaint_no DESC;
