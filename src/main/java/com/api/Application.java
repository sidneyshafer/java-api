package com.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Main entry point for the Spring Boot API application.
 * 
 * Features:
 * - Multi-database support
 * - External SQL file execution
 * - Async processing support
 * - Caching layer (Redis-ready)
 * - Transaction management
 */
@SpringBootApplication
@EnableAsync
@EnableCaching
@EnableTransactionManagement
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
