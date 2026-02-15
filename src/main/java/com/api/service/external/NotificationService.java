package com.api.service.external;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * External service for sending notifications (email, SMS, push).
 * All external API calls should be in the service/external package.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    /**
     * Send email notification asynchronously.
     */
    @Async("externalApiExecutor")
    public CompletableFuture<Boolean> sendEmailAsync(EmailRequest request) {
        log.info("Sending email to: {}", request.to());
        
        try {
            // In a real implementation, this would call an email service (SendGrid, SES, etc.)
            Thread.sleep(50); // Simulate API latency
            
            log.info("Email sent successfully to: {}", request.to());
            return CompletableFuture.completedFuture(true);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Email sending interrupted for: {}", request.to());
            return CompletableFuture.completedFuture(false);
        } catch (Exception e) {
            log.error("Failed to send email to: {}", request.to(), e);
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * Send SMS notification asynchronously.
     */
    @Async("externalApiExecutor")
    public CompletableFuture<Boolean> sendSmsAsync(SmsRequest request) {
        log.info("Sending SMS to: {}", request.phoneNumber());
        
        try {
            // In a real implementation, this would call an SMS service (Twilio, etc.)
            Thread.sleep(50);
            
            log.info("SMS sent successfully to: {}", request.phoneNumber());
            return CompletableFuture.completedFuture(true);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("SMS sending interrupted for: {}", request.phoneNumber());
            return CompletableFuture.completedFuture(false);
        } catch (Exception e) {
            log.error("Failed to send SMS to: {}", request.phoneNumber(), e);
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * Send order confirmation notification.
     */
    @Async("externalApiExecutor")
    public CompletableFuture<Void> sendOrderConfirmation(String email, String orderNumber) {
        log.info("Sending order confirmation for order: {} to: {}", orderNumber, email);
        
        EmailRequest emailRequest = new EmailRequest(
                email,
                "Order Confirmation - " + orderNumber,
                "Thank you for your order! Your order number is: " + orderNumber
        );
        
        return sendEmailAsync(emailRequest).thenAccept(success -> {
            if (!success) {
                log.warn("Failed to send order confirmation for: {}", orderNumber);
            }
        });
    }

    /**
     * Send shipping notification.
     */
    @Async("externalApiExecutor")
    public CompletableFuture<Void> sendShippingNotification(String email, String orderNumber, String trackingNumber) {
        log.info("Sending shipping notification for order: {} to: {}", orderNumber, email);
        
        EmailRequest emailRequest = new EmailRequest(
                email,
                "Your Order Has Shipped - " + orderNumber,
                "Great news! Your order " + orderNumber + " has shipped. " +
                "Track your package with tracking number: " + trackingNumber
        );
        
        return sendEmailAsync(emailRequest).thenAccept(success -> {
            if (!success) {
                log.warn("Failed to send shipping notification for: {}", orderNumber);
            }
        });
    }

    /**
     * Email request DTO.
     */
    public record EmailRequest(
            String to,
            String subject,
            String body
    ) {}

    /**
     * SMS request DTO.
     */
    public record SmsRequest(
            String phoneNumber,
            String message
    ) {}
}
