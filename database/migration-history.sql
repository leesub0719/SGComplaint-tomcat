-- Read-only queries. Run against the intended database in DBeaver.
SELECT DATABASE() AS current_database;

SELECT installed_rank, version, description, type, script, checksum,
       installed_by, installed_on, execution_time, success
FROM flyway_schema_history
ORDER BY installed_rank;

-- Any result here requires investigation. Do not automatically repair/delete history.
SELECT installed_rank, version, description, script, installed_on
FROM flyway_schema_history
WHERE success = 0
ORDER BY installed_rank;
