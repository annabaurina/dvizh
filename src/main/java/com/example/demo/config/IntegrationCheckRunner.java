package com.example.demo.config;

import com.example.demo.integration.IntegrationFlowChecks;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

// Runs IntegrationFlowChecks on startup (profile integration-check). Aborts boot on failure.
@Component
@Profile("integration-check")
@Order(Integer.MAX_VALUE)
public class IntegrationCheckRunner implements CommandLineRunner {

    private final TransactionTemplate transactionTemplate;
    private final IntegrationFlowChecks integrationFlowChecks;

    public IntegrationCheckRunner(
            TransactionTemplate transactionTemplate,
            IntegrationFlowChecks integrationFlowChecks
    ) {
        this.transactionTemplate = transactionTemplate;
        this.integrationFlowChecks = integrationFlowChecks;
    }

    @Override
    public void run(String... args) {
        Boolean ok = transactionTemplate.execute(status -> {
            try {
                integrationFlowChecks.runAll();
                return true;
            } catch (Exception e) {
                System.err.println("Integration check FAILED: " + e.getMessage());
                e.printStackTrace(System.err);
                return false;
            } finally {
                status.setRollbackOnly();
            }
        });
        if (!Boolean.TRUE.equals(ok)) {
            throw new IllegalStateException(
                    "Integration checks failed, application startup aborted");
        }
        System.out.println("Integration checks PASSED (all changes rolled back)");
    }
}
