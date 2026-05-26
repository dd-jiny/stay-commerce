package com.staycommerce;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "spring.batch.job.enabled=false")
public class StayBatchApplicationTest {
    @Test
    void contextLoads() {}
}
