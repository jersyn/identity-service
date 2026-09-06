package com.identity.service;

import com.identity.service.testcontainers.PostgresTestcontainer;
import com.identity.service.testcontainers.RedisTestcontainer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({PostgresTestcontainer.class, RedisTestcontainer.class})
@Testcontainers
public abstract class AbstractIntegrationTest {
}
