package com.transit.SGComplaint.migration;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

/** Read-only comparison before admitting a legacy MySQL schema as baseline V1. */
public final class V1SchemaVerifier {
    private V1SchemaVerifier() { }

    public static Set<String> expected() {
        try (InputStream input = V1SchemaVerifier.class.getResourceAsStream("/db/baseline/v1-schema.tsv")) {
            if (input == null) throw new IllegalStateException("V1 schema contract is missing.");
            return new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8)).lines()
                    .filter(line -> !line.isBlank() && !line.startsWith("#"))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot load V1 schema contract.", exception);
        }
    }

    public static void verifyDatabaseName(Connection connection, String expectedName) throws SQLException {
        String actual = connection.getCatalog();
        if (expectedName == null || expectedName.isBlank() || !expectedName.equals(actual)) {
            throw new IllegalStateException("DB_NAME과 실제 접속 DB가 다릅니다. 예상=" + expectedName + ", 실제=" + actual);
        }
    }

    public static Set<String> tableNames(Connection connection) throws SQLException {
        Set<String> tables = new LinkedHashSet<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE()")) {
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) tables.add(result.getString(1));
            }
        }
        return tables;
    }

    public static Set<String> actual(Connection connection) throws SQLException {
        Set<String> rows = new LinkedHashSet<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT TABLE_NAME, ENGINE FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE()"
                        + " AND TABLE_NAME <> 'flyway_schema_history'");
             ResultSet result = statement.executeQuery()) {
            while (result.next()) rows.add(row("T", result.getString(1), lower(result.getString(2))));
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT, EXTRA, CHARACTER_SET_NAME"
                        + " FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()"
                        + " AND TABLE_NAME <> 'flyway_schema_history' ORDER BY TABLE_NAME, ORDINAL_POSITION");
             ResultSet result = statement.executeQuery()) {
            while (result.next()) rows.add(row("C", result.getString(1), result.getString(2),
                    normalizeType(result.getString(3)), result.getString(4),
                    normalizeDefault(result.getString(5)), normalizeExtra(result.getString(6)),
                    result.getString(7) == null ? "-" : lower(result.getString(7))));
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT TABLE_NAME, INDEX_NAME, NON_UNIQUE,"
                        + " GROUP_CONCAT(CONCAT(COALESCE(COLUMN_NAME,'<expression>'),"
                        + " IF(SUB_PART IS NULL,'',CONCAT('(',SUB_PART,')'))) ORDER BY SEQ_IN_INDEX SEPARATOR ',')"
                        + " FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE()"
                        + " AND TABLE_NAME <> 'flyway_schema_history' GROUP BY TABLE_NAME, INDEX_NAME, NON_UNIQUE");
             ResultSet result = statement.executeQuery()) {
            while (result.next()) rows.add(row("I", result.getString(1),
                    "PRIMARY".equals(result.getString(2)) ? "PRIMARY" : result.getInt(3) == 0 ? "UNIQUE" : "INDEX",
                    result.getString(4)));
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT k.TABLE_NAME, GROUP_CONCAT(k.COLUMN_NAME ORDER BY k.ORDINAL_POSITION SEPARATOR ','),"
                        + " k.REFERENCED_TABLE_NAME,"
                        + " GROUP_CONCAT(k.REFERENCED_COLUMN_NAME ORDER BY k.ORDINAL_POSITION SEPARATOR ','),"
                        + " r.DELETE_RULE, r.UPDATE_RULE, k.REFERENCED_TABLE_SCHEMA"
                        + " FROM information_schema.KEY_COLUMN_USAGE k"
                        + " JOIN information_schema.REFERENTIAL_CONSTRAINTS r"
                        + " ON r.CONSTRAINT_SCHEMA=k.CONSTRAINT_SCHEMA AND r.TABLE_NAME=k.TABLE_NAME"
                        + " AND r.CONSTRAINT_NAME=k.CONSTRAINT_NAME"
                        + " WHERE k.TABLE_SCHEMA=DATABASE() AND k.REFERENCED_TABLE_NAME IS NOT NULL"
                        + " GROUP BY k.TABLE_NAME, k.CONSTRAINT_NAME, k.REFERENCED_TABLE_NAME,"
                        + " r.DELETE_RULE, r.UPDATE_RULE, k.REFERENCED_TABLE_SCHEMA");
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                String referenced = result.getString(3);
                if (!connection.getCatalog().equals(result.getString(7))) referenced = result.getString(7) + "." + referenced;
                rows.add(row("F", result.getString(1), result.getString(2), referenced,
                        result.getString(4), normalizeRule(result.getString(5)), normalizeRule(result.getString(6))));
            }
        }
        return rows;
    }

    public static List<String> differences(Set<String> expected, Set<String> actual) {
        List<String> result = new ArrayList<>();
        expected.stream().filter(line -> !actual.contains(line)).sorted()
                .forEach(line -> result.add("MISSING/DIFFERENT: " + line.replace('\t', ' ')));
        actual.stream().filter(line -> !expected.contains(line))
                .filter(line -> !line.startsWith("I\t") || !line.contains("\tINDEX\t")).sorted()
                .forEach(line -> result.add("UNEXPECTED/DIFFERENT: " + line.replace('\t', ' ')));
        return result;
    }

    public static void requireMatch(Connection connection) throws SQLException {
        List<String> differences = differences(expected(), actual(connection));
        if (!differences.isEmpty()) {
            throw new IllegalStateException("V1 기준 구조와 일치하지 않아 baseline을 중단했습니다. "
                    + "업무 데이터/테이블은 변경하지 않았습니다.\n" + String.join("\n", differences));
        }
    }

    static String normalizeType(String value) {
        return lower(value).replaceAll("\\b(tinyint|smallint|mediumint|int|bigint)\\(\\d+\\)", "$1");
    }
    static String normalizeDefault(String value) {
        if (value == null) return "<NULL>";
        return value.equalsIgnoreCase("current_timestamp()") || value.equalsIgnoreCase("current_timestamp")
                ? "current_timestamp" : value;
    }
    static String normalizeExtra(String value) {
        return lower(value).replace("default_generated", "").replace("current_timestamp()", "current_timestamp")
                .trim().replaceAll("\\s+", " ");
    }
    private static String normalizeRule(String rule) { return "NO ACTION".equals(rule) ? "RESTRICT" : rule; }
    private static String lower(String value) { return value == null ? "<NULL>" : value.toLowerCase(Locale.ROOT); }
    private static String row(String... columns) { return String.join("\t", columns); }
}
