
package com.example.bankapp.health;   

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseHealthIndicator implements HealthIndicator {
    private final JdbcTemplate jdbcTemplate;

    public DatabaseHealthIndicator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Health health() {
        try {
            // Test database connectivity with a simple query
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            
            // Get connection pool metrics
            int active = jdbcTemplate.getDataSource().unwrap(com.zaxxer.hikari.HikariDataSource.class).getHikariPoolMXBean().getActiveConnections();
            int idle = jdbcTemplate.getDataSource().unwrap(com.zaxxer.hikari.HikariDataSource.class).getHikariPoolMXBean().getIdleConnections();
            
            return Health.up()
                    .withDetail("database", "MySQL")
                    .withDetail("active_connections", active)
                    .withDetail("idle_connections", idle)
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("database", "MySQL")
                    .build();
        }
    }
}