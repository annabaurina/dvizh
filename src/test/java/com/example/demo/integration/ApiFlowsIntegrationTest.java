package com.example.demo.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("integration")
@Transactional
class ApiFlowsIntegrationTest {

    @Autowired
    private IntegrationFlowChecks integrationFlowChecks;

    @Test
    void allMeAndAdminFlowsWork() {
        integrationFlowChecks.runAll();
    }
}
