package com.api.service.external;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * External service for integrating with third-party payment providers.
 * All external API calls should be in the service/external package.
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final RestClient restClient;
    
    @Value("${app.external-api.timeout:5000}")
    private int timeout;

    @Value("${app.external-api.retry-attempts:3}")
    private int retryAttempts;

    public PaymentService(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder
                .baseUrl("https://api.payment-provider.example.com")
                .build();
    }

    /**
     * Process payment asynchronously.
     */
    @Async("externalApiExecutor")
    public CompletableFuture<PaymentResult> processPaymentAsync(PaymentRequest request) {
        log.info("Processing payment for order: {}", request.orderId());
        
        try {
            // Simulate external API call with retry logic
            PaymentResult result = executeWithRetry(() -> processPayment(request));
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            log.error("Payment processing failed for order: {}", request.orderId(), e);
            return CompletableFuture.completedFuture(
                    new PaymentResult("FAILED", null, e.getMessage()));
        }
    }

    /**
     * Process payment synchronously.
     */
    public PaymentResult processPayment(PaymentRequest request) {
        log.debug("Sending payment request: {}", request);
        
        // In a real implementation, this would call the external payment API
        // For demo purposes, we simulate a successful payment
        try {
            Thread.sleep(100); // Simulate API latency
            
            String transactionId = "TXN-" + System.currentTimeMillis();
            log.info("Payment processed successfully. Transaction ID: {}", transactionId);
            
            return new PaymentResult("SUCCESS", transactionId, null);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Payment processing interrupted", e);
        }
    }

    /**
     * Refund a payment.
     */
    @Async("externalApiExecutor")
    public CompletableFuture<PaymentResult> refundPaymentAsync(String transactionId) {
        log.info("Processing refund for transaction: {}", transactionId);
        
        try {
            PaymentResult result = executeWithRetry(() -> refundPayment(transactionId));
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            log.error("Refund failed for transaction: {}", transactionId, e);
            return CompletableFuture.completedFuture(
                    new PaymentResult("FAILED", null, e.getMessage()));
        }
    }

    /**
     * Refund a payment synchronously.
     */
    public PaymentResult refundPayment(String transactionId) {
        log.debug("Sending refund request for transaction: {}", transactionId);
        
        // Simulate refund processing
        try {
            Thread.sleep(100);
            
            String refundId = "REF-" + System.currentTimeMillis();
            log.info("Refund processed successfully. Refund ID: {}", refundId);
            
            return new PaymentResult("SUCCESS", refundId, null);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Refund processing interrupted", e);
        }
    }

    /**
     * Execute with retry logic.
     */
    private <T> T executeWithRetry(java.util.function.Supplier<T> operation) {
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= retryAttempts; attempt++) {
            try {
                return operation.get();
            } catch (Exception e) {
                lastException = e;
                log.warn("Attempt {} failed: {}", attempt, e.getMessage());
                
                if (attempt < retryAttempts) {
                    try {
                        Thread.sleep(Duration.ofMillis(1000 * attempt).toMillis());
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Retry interrupted", ie);
                    }
                }
            }
        }
        
        throw new RuntimeException("All retry attempts failed", lastException);
    }

    /**
     * Payment request DTO.
     */
    public record PaymentRequest(
            String orderId,
            java.math.BigDecimal amount,
            String currency,
            String paymentMethod
    ) {}

    /**
     * Payment result DTO.
     */
    public record PaymentResult(
            String status,
            String transactionId,
            String errorMessage
    ) {}
}
