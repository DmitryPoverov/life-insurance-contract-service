package io.github.dmitrypoverov.insurance.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({TestcontainersConfiguration.class, TestSecurityConfiguration.class})
public abstract class IntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @LocalServerPort
    private int port;

    protected RestTestClient client;

    @BeforeEach
    void setUp() {
        cleanDatabase();
        client = RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    private void cleanDatabase() {
        List<String> tables = jdbcTemplate.queryForList(
                "select tablename from pg_tables where schemaname = 'public' "
                        + "and tablename not like 'databasechange%'",
                String.class);
        if (!tables.isEmpty()) {
            jdbcTemplate.execute("truncate table " + String.join(", ", tables) + " restart identity cascade");
        }
    }
}
