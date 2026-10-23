CREATE TABLE IF NOT EXISTS persistent_logins (
    username VARCHAR(64) NOT NULL,
    series VARCHAR(64) NOT NULL,
    token VARCHAR(64) NOT NULL,
    last_used TIMESTAMP NOT NULL,
    PRIMARY KEY (series),
    KEY idx_persistent_logins_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sgtransit_admin_delete_audit (
    audit_no BIGINT NOT NULL AUTO_INCREMENT,
    admin_emp_no BIGINT NOT NULL,
    admin_emp_id VARCHAR(50) NOT NULL,
    target_type VARCHAR(30) NOT NULL,
    target_no BIGINT NOT NULL,
    target_summary VARCHAR(200) NOT NULL,
    snapshot_json JSON NOT NULL,
    deleted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (audit_no),
    KEY idx_delete_audit_target (target_type, target_no),
    KEY idx_delete_audit_admin_date (admin_emp_no, deleted_at),
    KEY idx_delete_audit_date (deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sgtransit_withdrawal_id_history (
    history_no BIGINT NOT NULL AUTO_INCREMENT,
    emp_no BIGINT NOT NULL,
    original_emp_id VARCHAR(50) NOT NULL,
    replacement_emp_id VARCHAR(50) NOT NULL,
    change_reason VARCHAR(30) NOT NULL,
    changed_by VARCHAR(50) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    purge_after DATETIME NOT NULL,
    PRIMARY KEY (history_no),
    KEY idx_withdrawal_history_emp (emp_no, created_at),
    KEY idx_withdrawal_history_original (original_emp_id),
    KEY idx_withdrawal_history_purge (purge_after)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET @sg_index_sql = IF(
    EXISTS(SELECT 1 FROM information_schema.STATISTICS
           WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='sgtransit_employee'
             AND INDEX_NAME='idx_employee_name'),
    'SELECT 1',
    'CREATE INDEX idx_employee_name ON sgtransit_employee (emp_name)');
PREPARE sg_index_statement FROM @sg_index_sql;
EXECUTE sg_index_statement;
DEALLOCATE PREPARE sg_index_statement;

SET @sg_index_sql = IF(
    EXISTS(SELECT 1 FROM information_schema.STATISTICS
           WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='sgtransit_employee'
             AND INDEX_NAME='idx_employee_phone'),
    'SELECT 1',
    'CREATE INDEX idx_employee_phone ON sgtransit_employee (emp_phone)');
PREPARE sg_index_statement FROM @sg_index_sql;
EXECUTE sg_index_statement;
DEALLOCATE PREPARE sg_index_statement;

SET @sg_index_sql = IF(
    EXISTS(SELECT 1 FROM information_schema.STATISTICS
           WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='sgtransit_complaint'
             AND INDEX_NAME='idx_complaint_title'),
    'SELECT 1',
    'CREATE INDEX idx_complaint_title ON sgtransit_complaint (complaint_title)');
PREPARE sg_index_statement FROM @sg_index_sql;
EXECUTE sg_index_statement;
DEALLOCATE PREPARE sg_index_statement;

SET @sg_index_sql = IF(
    EXISTS(SELECT 1 FROM information_schema.STATISTICS
           WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='sgtransit_notice'
             AND INDEX_NAME='idx_notice_title'),
    'SELECT 1',
    'CREATE INDEX idx_notice_title ON sgtransit_notice (notice_title)');
PREPARE sg_index_statement FROM @sg_index_sql;
EXECUTE sg_index_statement;
DEALLOCATE PREPARE sg_index_statement;

SET @sg_index_sql = IF(
    EXISTS(SELECT 1 FROM information_schema.STATISTICS
           WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='sgtransit_partner'
             AND INDEX_NAME='idx_partner_name'),
    'SELECT 1',
    'CREATE INDEX idx_partner_name ON sgtransit_partner (partner_name)');
PREPARE sg_index_statement FROM @sg_index_sql;
EXECUTE sg_index_statement;
DEALLOCATE PREPARE sg_index_statement;

SET @sg_index_sql = IF(
    EXISTS(SELECT 1 FROM information_schema.STATISTICS
           WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='sgtransit_partner'
             AND INDEX_NAME='idx_partner_phone'),
    'SELECT 1',
    'CREATE INDEX idx_partner_phone ON sgtransit_partner (partner_phone)');
PREPARE sg_index_statement FROM @sg_index_sql;
EXECUTE sg_index_statement;
DEALLOCATE PREPARE sg_index_statement;

SET @sg_index_sql = IF(
    EXISTS(SELECT 1 FROM information_schema.STATISTICS
           WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='sgtransit_partner'
             AND INDEX_NAME='idx_partner_site'),
    'SELECT 1',
    'CREATE INDEX idx_partner_site ON sgtransit_partner (partner_site)');
PREPARE sg_index_statement FROM @sg_index_sql;
EXECUTE sg_index_statement;
DEALLOCATE PREPARE sg_index_statement;
