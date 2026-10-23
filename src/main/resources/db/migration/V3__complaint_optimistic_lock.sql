SET @version_column_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'sgtransit_complaint'
      AND column_name = 'version'
);

SET @version_column_sql = IF(
    @version_column_exists = 0,
    'ALTER TABLE sgtransit_complaint ADD COLUMN version BIGINT NOT NULL DEFAULT 0 COMMENT ''동시 수정 충돌 감지용 버전'' AFTER complaint_status',
    'SELECT 1'
);

PREPARE version_column_statement FROM @version_column_sql;
EXECUTE version_column_statement;
DEALLOCATE PREPARE version_column_statement;
