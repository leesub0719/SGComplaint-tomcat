USE bus_complaint_db;

-- 로그인 회원번호와 기간을 함께 사용하는 민원 조회 인덱스입니다.
SET @member_date_index_exists = (
    SELECT COUNT(*)
      FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'sgtransit_complaint'
       AND INDEX_NAME = 'idx_complaint_emp_created'
);

SET @add_member_date_index_sql = IF(
    @member_date_index_exists = 0,
    'CREATE INDEX idx_complaint_emp_created
         ON sgtransit_complaint (emp_no, created_at)',
    'SELECT ''idx_complaint_emp_created index already exists'''
);

PREPARE add_member_date_index_statement FROM @add_member_date_index_sql;
EXECUTE add_member_date_index_statement;
DEALLOCATE PREPARE add_member_date_index_statement;
