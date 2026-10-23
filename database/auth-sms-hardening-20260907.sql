USE sgcomplaint;

CREATE TABLE IF NOT EXISTS sgtransit_abuse_limit (
    bucket_key VARCHAR(100) NOT NULL,
    hits INT NOT NULL DEFAULT 0,
    reset_at DATETIME NOT NULL,
    PRIMARY KEY (bucket_key),
    KEY idx_abuse_limit_reset (reset_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 기존 인증은 scope가 NULL이므로 더 이상 사용할 수 없습니다. 새 인증을 요청하세요.
SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
        AND table_name = 'sgtransit_phone_verification' AND column_name = 'challenge_scope'),
    'SELECT 1',
    'ALTER TABLE sgtransit_phone_verification ADD COLUMN challenge_scope CHAR(64) NULL');
PREPARE migration FROM @ddl;
EXECUTE migration;
DEALLOCATE PREPARE migration;
