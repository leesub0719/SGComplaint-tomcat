USE bus_complaint_db;

-- 기존 협력업체 테이블에 소프트 삭제 상태 컬럼을 추가합니다.
SET @partner_status_exists = (
    SELECT COUNT(*)
      FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'sgtransit_partner'
       AND COLUMN_NAME = 'partner_status'
);

SET @add_partner_status_sql = IF(
    @partner_status_exists = 0,
    'ALTER TABLE sgtransit_partner
         ADD COLUMN partner_status CHAR(1) NOT NULL DEFAULT ''Y''
         COMMENT ''노출 Y, 삭제 N'' AFTER partner_notes',
    'SELECT ''partner_status column already exists'''
);
PREPARE add_partner_status_statement FROM @add_partner_status_sql;
EXECUTE add_partner_status_statement;
DEALLOCATE PREPARE add_partner_status_statement;

UPDATE sgtransit_partner
   SET partner_status = 'Y'
 WHERE partner_status IS NULL OR partner_status = '';

ALTER TABLE sgtransit_partner
    MODIFY COLUMN partner_status CHAR(1) NOT NULL DEFAULT 'Y'
    COMMENT '노출 Y, 삭제 N';

SET @partner_status_index_exists = (
    SELECT COUNT(*)
      FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'sgtransit_partner'
       AND INDEX_NAME = 'idx_partner_status_created'
);
SET @add_partner_status_index_sql = IF(
    @partner_status_index_exists = 0,
    'CREATE INDEX idx_partner_status_created
         ON sgtransit_partner (partner_status, created_at)',
    'SELECT ''idx_partner_status_created index already exists'''
);
PREPARE add_partner_status_index_statement FROM @add_partner_status_index_sql;
EXECUTE add_partner_status_index_statement;
DEALLOCATE PREPARE add_partner_status_index_statement;

-- 기존 emp_role 컬럼은 CHAR(1)이므로 구조 변경 없이 M 값을 사용할 수 있습니다.
ALTER TABLE sgtransit_employee
    MODIFY COLUMN emp_role CHAR(1) NOT NULL DEFAULT 'U'
    COMMENT '사용자 U, 관리자 A, 마스터 M';

-- 아래 아이디를 실제 마스터로 사용할 관리자 아이디로 바꾼 후 실행하세요.
-- UPDATE sgtransit_employee
--    SET emp_role = 'M', updated_at = CURRENT_TIMESTAMP
--  WHERE emp_id = 'master_admin_id';

SELECT emp_no, emp_id, emp_name, emp_role, emp_status
  FROM sgtransit_employee
 WHERE emp_role IN ('A', 'M')
 ORDER BY emp_no;
