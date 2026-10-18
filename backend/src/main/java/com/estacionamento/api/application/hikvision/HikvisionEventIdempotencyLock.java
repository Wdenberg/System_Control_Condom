package com.estacionamento.api.application.hikvision;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Serializes the processing of the same Hikvision event inside PostgreSQL.
 *
 * The lock is transaction-scoped and is automatically released by PostgreSQL
 * on COMMIT or ROLLBACK.
 */
@Component
public class HikvisionEventIdempotencyLock {

    private final JdbcTemplate jdbcTemplate;

    public HikvisionEventIdempotencyLock(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void lock(String sourceIpAddress, String externalEventId) {
        Objects.requireNonNull(sourceIpAddress, "sourceIpAddress is required");
        Objects.requireNonNull(externalEventId, "externalEventId is required");

        String lockKey = sourceIpAddress + "|" + externalEventId;

        jdbcTemplate.queryForObject(
                """
                SELECT pg_advisory_xact_lock(
                    hashtextextended(?, 0)
                )
                """,
                (rs, rowNum) -> 1,
                lockKey
        );
    }
}
