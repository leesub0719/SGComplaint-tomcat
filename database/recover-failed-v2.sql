USE sgcomplaint;

-- 이 스크립트는 V2가 실패한 개발 DB에서만 실행합니다.
-- 성공한 V2 이력은 삭제하지 않습니다.
SELECT installed_rank, version, description, script, checksum, success
FROM flyway_schema_history
WHERE version = '2';

DELETE FROM flyway_schema_history
WHERE version = '2'
  AND success = 0;

SELECT ROW_COUNT() AS deleted_failed_v2_history;

-- 앞선 실패 때 일부 생성된 객체를 확인합니다.
SELECT TABLE_NAME
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN (
      'persistent_logins',
      'sgtransit_admin_delete_audit',
      'sgtransit_withdrawal_id_history'
  )
ORDER BY TABLE_NAME;

SELECT TABLE_NAME, INDEX_NAME,
       GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS indexed_columns
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND INDEX_NAME IN (
      'idx_employee_name', 'idx_employee_phone',
      'idx_complaint_title', 'idx_notice_title',
      'idx_partner_name', 'idx_partner_phone', 'idx_partner_site'
  )
GROUP BY TABLE_NAME, INDEX_NAME
ORDER BY TABLE_NAME, INDEX_NAME;
