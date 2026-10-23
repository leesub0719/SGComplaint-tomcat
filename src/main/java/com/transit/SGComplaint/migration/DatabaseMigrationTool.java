package com.transit.SGComplaint.migration;

import org.flywaydb.core.Flyway;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;

/** Standalone entry point: no Spring application, HTTP server, schedulers or SMS sender is started. */
public final class DatabaseMigrationTool {
    public static final String HISTORY_TABLE = "flyway_schema_history";
    private static final Set<String> COMMANDS = Set.of("check", "baseline", "info", "migrate", "validate");

    private DatabaseMigrationTool() { }

    public static void main(String[] args) {
        try {
            if (args.length != 1 || !COMMANDS.contains(args[0])) {
                throw new IllegalArgumentException("사용법: check | baseline | info | migrate | validate (clean/repair 지원 안 함)");
            }
            String url = required("DB_URL");
            if (!url.startsWith("jdbc:mysql:") || url.matches("(?i).*([?&])(password|user)=.*")) {
                throw new IllegalArgumentException("DB_URL은 MySQL JDBC URL이어야 하며 계정/비밀번호는 별도 환경변수로 지정하세요.");
            }
            String database = System.getenv().getOrDefault("DB_NAME", "sgcomplaint");
            Flyway flyway = Flyway.configure().dataSource(url, required("DB_USERNAME"), required("DB_PASSWORD"))
                    .locations("classpath:db/migration").table(HISTORY_TABLE)
                    .baselineVersion("1").baselineDescription("Verified legacy MyBatis schema 2026-09-07")
                    .baselineOnMigrate(false).cleanDisabled(true).validateOnMigrate(true)
                    .validateMigrationNaming(true).outOfOrder(false).ignoreMigrationPatterns(new String[0])
                    .createSchemas(false).load();
            execute(args[0], flyway, database);
        } catch (Exception exception) {
            System.err.println("DB 작업 중단: " + exception.getMessage());
            System.exit(1);
        }
    }

    public static void execute(String command, Flyway flyway, String database) throws SQLException {
        if (!COMMANDS.contains(command)) throw new IllegalArgumentException("허용하지 않는 DB 작업입니다.");
        try (Connection connection = flyway.getConfiguration().getDataSource().getConnection()) {
            V1SchemaVerifier.verifyDatabaseName(connection, database);
            Set<String> tables = V1SchemaVerifier.tableNames(connection);
            if ("check".equals(command)) {
                if (tables.contains(HISTORY_TABLE)) {
                    throw new IllegalStateException("이미 이력 관리 중입니다. V1 check 대신 info/validate를 사용하세요.");
                }
                if (tables.isEmpty()) {
                    System.out.println("EMPTY_DB: baseline이 필요 없습니다. migrate로 V1을 생성하세요.");
                    return;
                }
                V1SchemaVerifier.requireMatch(connection);
                System.out.println("V1_CHECK_OK: 테이블/컬럼/기본값/키 구조 확인 완료. 아직 DB 변경은 없습니다.");
                return;
            }
            if ("baseline".equals(command)) {
                if (tables.contains(HISTORY_TABLE)) {
                    throw new IllegalStateException("이력 테이블이 이미 있습니다. baseline을 재실행하지 말고 info/validate를 사용하세요.");
                }
                if (tables.isEmpty()) throw new IllegalStateException("빈 DB에는 baseline 대신 migrate를 사용하세요.");
                V1SchemaVerifier.requireMatch(connection);
                requireSafeConfiguration(flyway);
                flyway.baseline();
                System.out.println("BASELINE_OK: 기존 업무 테이블/데이터는 유지하고 V1 이력만 등록했습니다.");
                return;
            }
        }
        if ("migrate".equals(command)) {
            migrateGuarded(flyway, database);
        } else if ("validate".equals(command)) {
            requireInitializedOrEmpty(flyway, database);
            flyway.validate();
            System.out.println("VALIDATE_OK");
        } else {
            for (var migration : flyway.info().all()) {
                System.out.printf("%s | %s | %s | %s%n", migration.getVersion(),
                        migration.getDescription(), migration.getType(), migration.getState());
            }
        }
    }

    public static void migrateGuarded(Flyway flyway, String database) throws SQLException {
        requireSafeConfiguration(flyway);
        requireInitializedOrEmpty(flyway, database);
        flyway.migrate();
    }

    private static void requireInitializedOrEmpty(Flyway flyway, String database) throws SQLException {
        try (Connection connection = flyway.getConfiguration().getDataSource().getConnection()) {
            V1SchemaVerifier.verifyDatabaseName(connection, database);
            Set<String> tables = V1SchemaVerifier.tableNames(connection);
            if (!tables.isEmpty() && !tables.contains(HISTORY_TABLE)) {
                throw new IllegalStateException("기존 DB에 Flyway 이력이 없습니다. 먼저 check 후 baseline을 실행하세요.");
            }
        }
    }

    private static void requireSafeConfiguration(Flyway flyway) {
        var config = flyway.getConfiguration();
        if (config.isBaselineOnMigrate() || !config.isCleanDisabled() || !config.isValidateOnMigrate()
                || config.isOutOfOrder() || !"1".equals(config.getBaselineVersion().getVersion())
                || !HISTORY_TABLE.equals(config.getTable())) {
            throw new IllegalStateException("Flyway 보호 설정이 변경되었습니다. baseline-on-migrate=false, "
                    + "clean-disabled=true, validate-on-migrate=true, out-of-order=false, baseline-version=1을 유지하세요.");
        }
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " 환경변수가 필요합니다.");
        return value;
    }
}
