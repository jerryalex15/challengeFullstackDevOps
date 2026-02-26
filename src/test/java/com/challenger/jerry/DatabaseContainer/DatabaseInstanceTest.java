package com.challenger.jerry.DatabaseContainer;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class DatabaseInstanceTest {

    @Container
    protected static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test");
    // Désactive Ryuk si tu es sur CI ou pour éviter des problèmes de firewall
    static {
        System.setProperty("TESTCONTAINERS_RYUK_DISABLED", "true");
        // Force Testcontainers à attendre le démarrage complet du conteneur
        postgres.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.properties.hibernate.dialect",
                () -> "org.hibernate.dialect.PostgreSQLDialect");

        // Réduire les problèmes de fermeture Hikari
        registry.add("spring.datasource.hikari.max-lifetime", () -> 60000); // 60s au lieu de 30s
        registry.add("spring.datasource.hikari.connection-timeout", () -> 60000); // timeout plus long
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> 5); // nombre max de connexions
        registry.add("spring.datasource.hikari.idle-timeout", () -> 30000); // idle timeout
        registry.add("spring.datasource.hikari.shutdown-timeout", () -> 1000);
        registry.add("spring.datasource.hikari.validation-timeout", () -> 1000);
    }
}
