package api.it;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public abstract class AbstractPostgresIT {

    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test");

    @BeforeAll
    static void startContainer() {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        r.add("spring.datasource.username", POSTGRES::getUsername);
        r.add("spring.datasource.password", POSTGRES::getPassword);

        // JPA
        r.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        r.add("spring.jpa.show-sql", () -> "false");

        // JWT (important pour JwtService)
        // secret doit être BASE64 sinon Decoders.BASE64.decode() plante
        r.add("jwt.secret", () -> "bXlzZWNyZXRrZXlteXNlY3JldGtleW15c2VjcmV0a2V5"); // "mysecretkeymysecretkeymysecretkey" en base64
        r.add("jwt.expiration", () -> "3600000"); // 1h
    }
}
