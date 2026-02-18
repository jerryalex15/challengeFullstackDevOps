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

    // Désactive Ryuk si tu es sur CI ou pour éviter des problèmes de firewall
    static {
        System.clearProperty("TESTCONTAINERS_RYUK_DISABLED");
    }

    @Container
    protected static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test")
                    .withReuse(true); // option utile pour réutiliser le container entre plusieurs builds

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.properties.hibernate.dialect",
                () -> "org.hibernate.dialect.PostgreSQLDialect");

        // Réduire les problèmes de fermeture Hikari
        registry.add("spring.datasource.hikari.max-lifetime", () -> 30000); // 30s
        registry.add("spring.datasource.hikari.shutdown-timeout", () -> 1000); // 1s
        registry.add("spring.datasource.hikari.validation-timeout", () -> 1000);
    }
}
