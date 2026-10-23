USE bus_complaint_db;

-- 기존 sgtransit_complaint 테이블에 상태 컬럼이 없다면 추가합니다.
SET @status_column_exists = (
    SELECT COUNT(*)
      FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'sgtransit_complaint'
       AND COLUMN_NAME = 'complaint_status'
);

SET @add_status_column_sql = IF(
    @status_column_exists = 0,
    'ALTER TABLE sgtransit_complaint
         ADD COLUMN complaint_status VARCHAR(20) NOT NULL DEFAULT ''CHECKING''
         COMMENT ''CHECKING 확인중, PROCESSING 처리중, COMPLETED 답변완료''
         AFTER complaint_content',
    'SELECT ''complaint_status column already exists'''
);

PREPARE add_status_column_statement FROM @add_status_column_sql;
EXECUTE add_status_column_statement;
DEALLOCATE PREPARE add_status_column_statement;

-- 예전 코드에서 저장한 RECEIVED 상태는 화면에서 사용하는 CHECKING으로 변환합니다.
UPDATE sgtransit_complaint
   SET complaint_status = 'CHECKING'
 WHERE complaint_status IS NULL
    OR complaint_status = ''
    OR complaint_status = 'RECEIVED';

-- 기존 컬럼이 있더라도 기본값과 설명을 최신 상태로 맞춥니다.
ALTER TABLE sgtransit_complaint
    MODIFY COLUMN complaint_status VARCHAR(20) NOT NULL DEFAULT 'CHECKING'
    COMMENT 'CHECKING 확인중, PROCESSING 처리중, COMPLETED 답변완료';

-- 상태와 등록일을 이용한 조회 성능을 위해 복합 인덱스를 보장합니다.
SET @status_index_exists = (
    SELECT COUNT(*)
      FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'sgtransit_complaint'
       AND INDEX_NAME = 'idx_complaint_status_created'
);

SET @add_status_index_sql = IF(
    @status_index_exists = 0,
    'CREATE INDEX idx_complaint_status_created
         ON sgtransit_complaint (complaint_status, created_at)',
    'SELECT ''idx_complaint_status_created index already exists'''
);

PREPARE add_status_index_statement FROM @add_status_index_sql;
EXECUTE add_status_index_statement;
DEALLOCATE PREPARE add_status_index_statement;

-- 적용 결과 확인
SELECT complaint_status, COUNT(*) AS complaint_count
  FROM sgtransit_complaint
 GROUP BY complaint_status;
