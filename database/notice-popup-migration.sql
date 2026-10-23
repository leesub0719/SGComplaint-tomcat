USE bus_complaint_db;

-- 기존 sgtransit_notice 테이블에 메인 팝업 사용 여부를 추가합니다.
-- 여러 번 실행해도 이미 컬럼/인덱스가 있으면 건너뜁니다.
SET @popup_column_exists = (
    SELECT COUNT(*)
      FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'sgtransit_notice'
       AND COLUMN_NAME = 'is_popup'
);
SET @popup_column_sql = IF(
    @popup_column_exists = 0,
    'ALTER TABLE sgtransit_notice ADD COLUMN is_popup CHAR(1) NOT NULL DEFAULT ''N'' COMMENT ''메인 팝업 노출 여부 Y/N'' AFTER is_pinned',
    'SELECT ''is_popup column already exists'''
);
PREPARE popup_column_statement FROM @popup_column_sql;
EXECUTE popup_column_statement;
DEALLOCATE PREPARE popup_column_statement;

SET @popup_index_exists = (
    SELECT COUNT(*)
      FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'sgtransit_notice'
       AND INDEX_NAME = 'idx_notice_popup_created'
);
SET @popup_index_sql = IF(
    @popup_index_exists = 0,
    'CREATE INDEX idx_notice_popup_created ON sgtransit_notice (is_popup, created_at)',
    'SELECT ''idx_notice_popup_created index already exists'''
);
PREPARE popup_index_statement FROM @popup_index_sql;
EXECUTE popup_index_statement;
DEALLOCATE PREPARE popup_index_statement;

SELECT notice_no, notice_title, is_popup
  FROM sgtransit_notice
 ORDER BY notice_no DESC;
