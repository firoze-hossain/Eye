package com.roze;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "java.awt.headless=true")
class TrackEyeApplicationTests {

    @BeforeAll
    static void setup() {
        System.setProperty("java.awt.headless", "true");
    }

    @Test
    void contextLoads() {
    }
}
