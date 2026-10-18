package com.estacionamento.api.application.hikvision;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class HikvisionEventIdempotencyLockConcurrencyTest {

    private static final String SOURCE_IP = "192.168.15.44";
    private static final String EXTERNAL_EVENT_ID = "event-concurrent-001";

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("garage_control_test")
                    .withUsername("garage")
                    .withPassword("garage");

    private static HikariDataSource dataSource;
    private static JdbcTemplate jdbcTemplate;
    private static TransactionTemplate transactionTemplate;
    private static HikvisionEventIdempotencyLock idempotencyLock;

    @BeforeAll
    static void setUpDatabase() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(POSTGRES.getJdbcUrl());
        config.setUsername(POSTGRES.getUsername());
        config.setPassword(POSTGRES.getPassword());
        config.setMaximumPoolSize(4);

        dataSource = new HikariDataSource(config);
        jdbcTemplate = new JdbcTemplate(dataSource);
        transactionTemplate =
                new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        idempotencyLock = new HikvisionEventIdempotencyLock(jdbcTemplate);

        jdbcTemplate.execute("""
                CREATE TABLE test_hikvision_events (
                    id BIGSERIAL PRIMARY KEY,
                    source_ip_address VARCHAR(64) NOT NULL,
                    external_event_id VARCHAR(255) NOT NULL,
                    CONSTRAINT uk_test_hikvision_event
                        UNIQUE (source_ip_address, external_event_id)
                )
                """);
    }

    @AfterAll
    static void tearDown() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    @Test
    void shouldSerializeConcurrentRequestsForTheSameHikvisionEvent() throws Exception {
        CyclicBarrier startBarrier = new CyclicBarrier(2);

        Callable<String> request = () -> {
            startBarrier.await(5, TimeUnit.SECONDS);

            return transactionTemplate.execute(status -> {
                idempotencyLock.lock(SOURCE_IP, EXTERNAL_EVENT_ID);

                Integer existing = jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM test_hikvision_events
                        WHERE source_ip_address = ?
                          AND external_event_id = ?
                        """,
                        Integer.class,
                        SOURCE_IP,
                        EXTERNAL_EVENT_ID
                );

                if (existing != null && existing > 0) {
                    return "DUPLICATE";
                }

                jdbcTemplate.update(
                        """
                        INSERT INTO test_hikvision_events(
                            source_ip_address,
                            external_event_id
                        )
                        VALUES (?, ?)
                        """,
                        SOURCE_IP,
                        EXTERNAL_EVENT_ID
                );

                return "RECEIVED";
            });
        };

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<String> first = executor.submit(request);
            Future<String> second = executor.submit(request);

            String firstResult = first.get(10, TimeUnit.SECONDS);
            String secondResult = second.get(10, TimeUnit.SECONDS);

            assertThat(List.of(firstResult, secondResult))
                    .containsExactlyInAnyOrder("RECEIVED", "DUPLICATE");

            Integer persisted = jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*)
                    FROM test_hikvision_events
                    WHERE source_ip_address = ?
                      AND external_event_id = ?
                    """,
                    Integer.class,
                    SOURCE_IP,
                    EXTERNAL_EVENT_ID
            );

            assertThat(persisted).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void shouldNotSerializeDifferentHikvisionEventsIntoOneDatabaseRecord() throws Exception {
        CyclicBarrier startBarrier = new CyclicBarrier(2);

        Callable<String> firstRequest = () -> processDistinctEvent(
                startBarrier,
                "event-A"
        );

        Callable<String> secondRequest = () -> processDistinctEvent(
                startBarrier,
                "event-B"
        );

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<String> first = executor.submit(firstRequest);
            Future<String> second = executor.submit(secondRequest);

            assertThat(first.get(10, TimeUnit.SECONDS)).isEqualTo("RECEIVED");
            assertThat(second.get(10, TimeUnit.SECONDS)).isEqualTo("RECEIVED");

            Integer persisted = jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*)
                    FROM test_hikvision_events
                    WHERE source_ip_address = ?
                      AND external_event_id IN ('event-A', 'event-B')
                    """,
                    Integer.class,
                    SOURCE_IP
            );

            assertThat(persisted).isEqualTo(2);
        } finally {
            executor.shutdownNow();
        }
    }

    private String processDistinctEvent(
            CyclicBarrier startBarrier,
            String externalEventId
    ) throws Exception {
        startBarrier.await(5, TimeUnit.SECONDS);

        return transactionTemplate.execute(status -> {
            idempotencyLock.lock(SOURCE_IP, externalEventId);

            jdbcTemplate.update(
                    """
                    INSERT INTO test_hikvision_events(
                        source_ip_address,
                        external_event_id
                    )
                    VALUES (?, ?)
                    """,
                    SOURCE_IP,
                    externalEventId
            );

            return "RECEIVED";
        });
    }
}
