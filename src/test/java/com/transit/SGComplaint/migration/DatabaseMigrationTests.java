package com.transit.SGComplaint.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DatabaseMigrationTests {
    @TempDir Path scripts;

    @Test void baselineContractContainsEveryCurrentTableAndColumn() throws Exception {
        var contract = V1SchemaVerifier.expected();
        assertEquals(14, contract.stream().filter(s -> s.startsWith("T\t")).count());
        assertEquals(121, contract.stream().filter(s -> s.startsWith("C\t")).count());
        assertEquals(38, contract.stream().filter(s -> s.startsWith("I\t")).count());
        assertEquals(8, contract.stream().filter(s -> s.startsWith("F\t")).count());
        String sql;
        try (var input = getClass().getResourceAsStream("/db/migration/V1__current_schema.sql")) {
            assertNotNull(input);
            sql = new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
        assertFalse(sql.contains("IF NOT EXISTS"));
        assertFalse(sql.contains("CREATE DATABASE"));
        assertFalse(sql.contains("USE sgcomplaint"));
        assertFalse(sql.matches("(?s).*(?i:DROP TABLE|TRUNCATE|UPDATE sgtransit|INSERT INTO).*"));
        for (String line : contract) {
            String[] parts = line.split("\t", -1);
            if ("T".equals(parts[0])) assertTrue(sql.contains("CREATE TABLE " + parts[1] + " ("), parts[1]);
            if ("C".equals(parts[0])) {
                int tableStart = sql.indexOf("CREATE TABLE " + parts[1] + " (");
                String definition = sql.substring(tableStart, sql.indexOf(") ENGINE=", tableStart));
                assertTrue(java.util.regex.Pattern.compile("(?im)^\\s*" + java.util.regex.Pattern.quote(parts[2])
                        + "\\s+" + java.util.regex.Pattern.quote(parts[3]) + "\\s+").matcher(definition).find(), line);
            }
        }
    }

    @Test void startupMigrationIsOptIn() {
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withUserConfiguration(com.transit.SGComplaint.config.DatabaseMigrationConfiguration.class)
                .withPropertyValues("spring.flyway.enabled=false")
                .run(context -> assertFalse(context.containsBean("guardedFlywayMigration")));
    }

    @Test void enabledStartupRegistersTheGuardedStrategy() {
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withUserConfiguration(com.transit.SGComplaint.config.DatabaseMigrationConfiguration.class)
                .withPropertyValues("spring.flyway.enabled=true", "app.database.expected-name=sgcomplaint")
                .run(context -> assertNotNull(context.getBean(
                        org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy.class)));
    }

    @Test void baselineMatchesUserProvidedLegacyDefinitions() {
        var contract = V1SchemaVerifier.expected();
        assertTrue(contract.contains("C\tsgtransit_employee\temp_id\tvarchar(50)\tNO\t<NULL>\t\tutf8mb4"));
        assertTrue(contract.contains("C\tsgtransit_employee\temp_address\tvarchar(255)\tYES\t<NULL>\t\tutf8mb4"));
        assertTrue(contract.contains("C\tsgtransit_employee\tupdated_at\tdatetime\tNO\tcurrent_timestamp\ton update current_timestamp\t-"));
        assertTrue(contract.contains("I\tsgtransit_employee\tUNIQUE\temp_email"));
        assertTrue(contract.contains("C\tsgtransit_complaint\tcomplaint_password\tvarchar(100)\tYES\t<NULL>\t\tutf8mb4"));
        // Do not widen the complaint's historical login ID while adopting the existing schema.
        assertTrue(contract.contains("C\tsgtransit_complaint\temp_id\tvarchar(20)\tNO\t<NULL>\t\tutf8mb4"));
    }

    @Test void missingStructureBlocksBaseline() {
        Set<String> expected = V1SchemaVerifier.expected();
        Set<String> actual = new LinkedHashSet<>(expected);
        actual.removeIf(s -> s.startsWith("C\tsgtransit_phone_verification\tchallenge_scope\t"));
        assertFalse(V1SchemaVerifier.differences(expected, actual).isEmpty());
    }

    @Test void extraNonUniqueIndexIsAllowedButExtraUniqueKeyIsNot() {
        Set<String> expected = V1SchemaVerifier.expected();
        Set<String> actual = new LinkedHashSet<>(expected);
        actual.add("I\tsgtransit_employee\tINDEX\temp_name");
        assertTrue(V1SchemaVerifier.differences(expected, actual).isEmpty());
        actual.add("I\tsgtransit_employee\tUNIQUE\temp_name");
        assertFalse(V1SchemaVerifier.differences(expected, actual).isEmpty());
    }

    @Test void extraColumnsAndChangedDefaultsAreNotSilentlyAccepted() {
        Set<String> expected = V1SchemaVerifier.expected();
        Set<String> actual = new LinkedHashSet<>(expected);
        actual.add("C\tsgtransit_employee\textra\tint\tYES\t<NULL>\t\t-");
        assertFalse(V1SchemaVerifier.differences(expected, actual).isEmpty());
        actual = new LinkedHashSet<>(expected);
        String role = actual.stream().filter(s -> s.startsWith("C\tsgtransit_employee\temp_role\t")).findFirst().orElseThrow();
        actual.remove(role);
        actual.add(role.replace("\tU\t", "\tA\t"));
        assertEquals(2, V1SchemaVerifier.differences(expected, actual).size());
    }

    @Test void normalizesMySqlMetadataWithoutHidingUnsignedOrLengthChanges() {
        assertEquals("bigint", V1SchemaVerifier.normalizeType("BIGINT(20)"));
        assertEquals("bigint unsigned", V1SchemaVerifier.normalizeType("bigint(20) unsigned"));
        assertEquals("varchar(100)", V1SchemaVerifier.normalizeType("VARCHAR(100)"));
        assertEquals("current_timestamp", V1SchemaVerifier.normalizeDefault("CURRENT_TIMESTAMP()"));
        assertEquals("", V1SchemaVerifier.normalizeExtra("DEFAULT_GENERATED"));
        assertEquals("auto_increment", V1SchemaVerifier.normalizeExtra("auto_increment"));
        assertEquals("on update current_timestamp",
                V1SchemaVerifier.normalizeExtra("DEFAULT_GENERATED on update CURRENT_TIMESTAMP()"));
    }

    @Test void wrongDatabaseNameIsRejected() throws Exception {
        var connection = mock(Connection.class);
        when(connection.getCatalog()).thenReturn("other_database");
        assertThrows(IllegalStateException.class, () -> V1SchemaVerifier.verifyDatabaseName(connection, "sgcomplaint"));
    }

    @Test void normalStartupCannotBaselineAnExistingUnversionedDatabase() throws Exception {
        var flyway = fakeFlyway(Set.of("sgtransit_employee"), Set.of());
        assertThrows(IllegalStateException.class, () -> DatabaseMigrationTool.migrateGuarded(flyway, "sgcomplaint"));
        verify(flyway, never()).baseline();
        verify(flyway, never()).migrate();
    }

    @Test void baselineOnlyRunsAfterFullV1Comparison() throws Exception {
        var contract = V1SchemaVerifier.expected();
        var tables = new LinkedHashSet<String>();
        contract.stream().filter(s -> s.startsWith("T\t")).forEach(s -> tables.add(s.split("\t")[1]));
        var flyway = fakeFlyway(tables, contract);
        DatabaseMigrationTool.execute("baseline", flyway, "sgcomplaint");
        verify(flyway).baseline();
        verify(flyway, never()).migrate();
    }

    @Test void incompleteBaselineDoesNotWriteHistory() throws Exception {
        var contract = new LinkedHashSet<>(V1SchemaVerifier.expected());
        contract.removeIf(s -> s.startsWith("F\t"));
        var flyway = fakeFlyway(Set.of("sgtransit_employee"), contract);
        assertThrows(IllegalStateException.class, () -> DatabaseMigrationTool.execute("baseline", flyway, "sgcomplaint"));
        verify(flyway, never()).baseline();
    }

    @Test void emptyDatabaseUsesMigrationNotBaseline() throws Exception {
        var flyway = fakeFlyway(Set.of(), Set.of());
        assertThrows(IllegalStateException.class, () -> DatabaseMigrationTool.execute("baseline", flyway, "sgcomplaint"));
        DatabaseMigrationTool.migrateGuarded(flyway, "sgcomplaint");
        verify(flyway, never()).baseline();
        verify(flyway).migrate();
    }

    @Test void existingHistoryCannotBeRebaselined() throws Exception {
        var flyway = fakeFlyway(Set.of("sgtransit_employee", "flyway_schema_history"), Set.of());
        assertThrows(IllegalStateException.class, () -> DatabaseMigrationTool.execute("baseline", flyway, "sgcomplaint"));
        DatabaseMigrationTool.migrateGuarded(flyway, "sgcomplaint");
        verify(flyway, never()).baseline();
        verify(flyway).migrate();
    }

    @Test void unsafeConfigurationAndDestructiveCommandsAreRejected() throws Exception {
        var flyway = fakeFlyway(Set.of(), Set.of());
        when(flyway.getConfiguration().isBaselineOnMigrate()).thenReturn(true);
        assertThrows(IllegalStateException.class, () -> DatabaseMigrationTool.migrateGuarded(flyway, "sgcomplaint"));
        assertThrows(IllegalArgumentException.class, () -> DatabaseMigrationTool.execute("clean", flyway, "sgcomplaint"));
        assertThrows(IllegalArgumentException.class, () -> DatabaseMigrationTool.execute("repair", flyway, "sgcomplaint"));
        verify(flyway, never()).migrate();
    }

    // These integration tests use an isolated in-memory H2 DB, not the user's MySQL.
    @Test void flywayRecordsVersionsAndDoesNotReplayAppliedSql() throws Exception {
        var flyway = h2Flyway();
        assertEquals(2, flyway.migrate().migrationsExecuted);
        assertEquals(0, flyway.migrate().migrationsExecuted);
        assertEquals(2, flyway.info().applied().length);
        flyway.validate();
        try (var connection = flyway.getConfiguration().getDataSource().getConnection();
             var statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT COUNT(*) FROM sample")) {
            assertTrue(rows.next());
            assertEquals(1, rows.getInt(1));
        }
    }

    @Test void baselinePreservesExistingDataAndSkipsV1() throws Exception {
        var flyway = h2Flyway();
        try (var connection = flyway.getConfiguration().getDataSource().getConnection();
             var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE sample (id INT PRIMARY KEY, title VARCHAR(50))");
            statement.execute("INSERT INTO sample VALUES (99, 'existing')");
        }
        flyway.baseline();
        assertEquals(1, flyway.migrate().migrationsExecuted);
        try (var connection = flyway.getConfiguration().getDataSource().getConnection();
             var statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT id, title, status FROM sample")) {
            assertTrue(rows.next());
            assertEquals(99, rows.getInt(1));
            assertEquals("existing", rows.getString(2));
            assertEquals("new", rows.getString(3));
            assertFalse(rows.next());
        }
    }

    @Test void changedAppliedSqlFailsChecksumValidation() throws Exception {
        var flyway = h2Flyway();
        flyway.migrate();
        Files.writeString(scripts.resolve("V2__status.sql"), "ALTER TABLE sample ADD status VARCHAR(50) DEFAULT 'changed';");
        assertThrows(FlywayException.class, flyway::validate);
    }

    @Test void cleanIsDisabledInMigrationRuntime() throws Exception {
        var flyway = h2Flyway();
        flyway.migrate();
        assertThrows(FlywayException.class, flyway::clean);
        assertEquals(2, flyway.info().applied().length);
    }

    @Test void removingAnAppliedSqlFileFailsValidation() throws Exception {
        var flyway = h2Flyway();
        flyway.migrate();
        Files.delete(scripts.resolve("V2__status.sql"));
        assertThrows(FlywayException.class, flyway::validate);
    }

    private Flyway h2Flyway() throws Exception {
        Files.writeString(scripts.resolve("V1__create.sql"),
                "CREATE TABLE sample (id INT PRIMARY KEY, title VARCHAR(50)); INSERT INTO sample VALUES (1, 'new data');");
        Files.writeString(scripts.resolve("V2__status.sql"),
                "ALTER TABLE sample ADD status VARCHAR(50) DEFAULT 'new';");
        return Flyway.configure().dataSource("jdbc:h2:mem:migration_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "")
                .locations("filesystem:" + scripts.toAbsolutePath()).baselineVersion("1")
                .baselineOnMigrate(false).cleanDisabled(true).validateOnMigrate(true).outOfOrder(false)
                .ignoreMigrationPatterns(new String[0]).load();
    }

    private Flyway fakeFlyway(Set<String> tables, Set<String> contract) throws Exception {
        var flyway = mock(Flyway.class, RETURNS_DEEP_STUBS);
        var connection = mock(Connection.class);
        when(connection.getCatalog()).thenReturn("sgcomplaint");
        when(flyway.getConfiguration().getDataSource().getConnection()).thenReturn(connection);
        when(flyway.getConfiguration().isCleanDisabled()).thenReturn(true);
        when(flyway.getConfiguration().isValidateOnMigrate()).thenReturn(true);
        when(flyway.getConfiguration().getBaselineVersion().getVersion()).thenReturn("1");
        when(flyway.getConfiguration().getTable()).thenReturn("flyway_schema_history");
        when(connection.prepareStatement(anyString())).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            List<String[]> rows = new ArrayList<>();
            if (sql.startsWith("SELECT TABLE_NAME FROM")) {
                tables.forEach(table -> rows.add(new String[]{table}));
            } else {
                String kind = sql.contains("information_schema.COLUMNS") ? "C" : sql.contains("information_schema.STATISTICS")
                        ? "I" : sql.contains("KEY_COLUMN_USAGE") ? "F" : "T";
                contract.stream().filter(line -> line.startsWith(kind + "\t")).forEach(line -> {
                    String[] parts = line.split("\t", -1);
                    switch (kind) {
                        case "T" -> rows.add(new String[]{parts[1], parts[2]});
                        case "C" -> rows.add(new String[]{parts[1], parts[2], parts[3], parts[4],
                                parts[5].equals("<NULL>") ? null : parts[5], parts[6], parts[7].equals("-") ? null : parts[7]});
                        case "I" -> rows.add(new String[]{parts[1], parts[2].equals("PRIMARY") ? "PRIMARY" : "custom_name",
                                parts[2].equals("INDEX") ? "1" : "0", parts[3]});
                        case "F" -> rows.add(new String[]{parts[1], parts[2], parts[3], parts[4], parts[5], parts[6], "sgcomplaint"});
                    }
                });
            }
            var statement = mock(PreparedStatement.class);
            var result = mock(ResultSet.class);
            int[] index = {-1};
            when(result.next()).thenAnswer(ignored -> ++index[0] < rows.size());
            when(result.getString(anyInt())).thenAnswer(call -> rows.get(index[0])[(int) call.getArgument(0) - 1]);
            when(result.getInt(anyInt())).thenAnswer(call -> Integer.parseInt(rows.get(index[0])[(int) call.getArgument(0) - 1]));
            when(statement.executeQuery()).thenReturn(result);
            return statement;
        });
        return flyway;
    }
}
