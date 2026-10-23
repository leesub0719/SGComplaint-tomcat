package com.transit.SGComplaint.config;

import com.transit.SGComplaint.migration.DatabaseMigrationTool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "spring.flyway.enabled", havingValue = "true")
public class DatabaseMigrationConfiguration {
    @Bean
    public FlywayMigrationStrategy guardedFlywayMigration(
            @Value("${app.database.expected-name:sgcomplaint}") String databaseName) {
        return flyway -> {
            try {
                DatabaseMigrationTool.migrateGuarded(flyway, databaseName);
            } catch (java.sql.SQLException exception) {
                throw new IllegalStateException("DB 변경 이력을 확인할 수 없습니다.", exception);
            }
        };
    }
}
