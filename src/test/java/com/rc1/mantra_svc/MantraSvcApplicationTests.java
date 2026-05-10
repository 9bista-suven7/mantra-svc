package com.rc1.mantra_svc;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.data.mongodb.host=localhost",
        "spring.data.mongodb.port=27017",
        "spring.data.mongodb.database=mantra_test_db",
        "app.jwt.secret=TestSecretKey2026ForUnitTestingOnly_32chars!",
        "app.jwt.expiration=3600000",
        "app.cors.allowed-origins=http://localhost:4200"
})
class MantraSvcApplicationTests {

    @Test
    void contextLoads() {
        // Verifies the Spring application context starts successfully
    }

}

