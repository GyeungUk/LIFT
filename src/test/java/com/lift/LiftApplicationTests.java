package com.lift;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "lift.auth.jwt-secret=test-jwt-secret-32-bytes-minimum-value")
class LiftApplicationTests {

    @Test
    void contextLoads() {
    }

}
