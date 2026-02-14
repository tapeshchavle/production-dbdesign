package com.ecom.notification.listener;

import com.ecom.common.event.BaseEvent;
import com.ecom.common.event.EventTypes;
import com.ecom.notification.entity.NotificationLog;
import com.ecom.notification.repository.NotificationLogRepository;
import com.ecom.notification.service.EmailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * SQS consumer that listens to events and triggers email notifications.
 *
 * Queue: notification-queue (subscribed to order-events, user-events,
 * catalog-events SNS topics)
 * DLQ: notification-dlq (messages go here after 3 failed retries)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EventListener {

    private final EmailService emailService;
    private final NotificationLogRepository notificationLogRepo;
    private final ObjectMapper objectMapper;

    @SqsListener("notification-queue")
    public void handleMessage(String rawMessage) {
        try {
            BaseEvent event = objectMapper.readValue(rawMessage, BaseEvent.class);
            log.info("Received event: type={}, eventId={}", event.getEventType(), event.getEventId());

            // Idempotency check — skip if already processed
            if (event.getIdempotencyKey() != null &&
                    notificationLogRepo.existsByIdempotencyKey(event.getIdempotencyKey())) {
                log.info("Duplicate event skipped: idempotencyKey={}", event.getIdempotencyKey());
                return;
            }

            processEvent(event, rawMessage);

        } catch (Exception e) {
            log.error("Failed to process SQS message: {}", e.getMessage(), e);
            throw new RuntimeException("Processing failed — will retry or DLQ", e);
        }
    }

    private void processEvent(BaseEvent event, String rawPayload) {
        Map<String, Object> data = event.getData();

        switch (event.getEventType()) {
            case EventTypes.ORDER_CREATED -> sendOrderConfirmation(event, data, rawPayload);
            case EventTypes.ORDER_CONFIRMED -> sendOrderConfirmed(event, data, rawPayload);
            case EventTypes.ORDER_SHIPPED -> sendShippingNotification(event, data, rawPayload);
            case EventTypes.ORDER_DELIVERED -> sendDeliveryNotification(event, data, rawPayload);
            case EventTypes.ORDER_CANCELLED -> sendCancellationNotification(event, data, rawPayload);
            case EventTypes.PAYMENT_SUCCESS -> sendPaymentReceipt(event, data, rawPayload);
            case EventTypes.PAYMENT_FAILED -> sendPaymentFailedNotification(event, data, rawPayload);
            case EventTypes.USER_REGISTERED -> sendWelcomeEmail(event, data, rawPayload);
            case EventTypes.LOW_STOCK_ALERT -> sendLowStockAlert(event, data, rawPayload);
            default -> log.warn("Unhandled event type: {}", event.getEventType());
        }
    }

    // ── Order Notifications ──

    private void sendOrderConfirmation(BaseEvent event, Map<String, Object> data, String rawPayload) {
        String email = (String) data.getOrDefault("email", "");
        String orderNumber = (String) data.getOrDefault("orderNumber", "");
        Object totalAmount = data.getOrDefault("totalAmount", "0");

        String subject = "Order Placed — " + orderNumber;
        String body = buildHtml(
                "Order Confirmed! 🎉",
                "Your order <strong>" + orderNumber + "</strong> has been placed successfully.",
                "Total: ₹" + totalAmount,
                "We'll notify you when it's shipped.");

        sendAndLog(event, email, subject, body, rawPayload);
    }

    private void sendOrderConfirmed(BaseEvent event, Map<String, Object> data, String rawPayload) {
        String email = (String) data.getOrDefault("email", "");
        String orderNumber = (String) data.getOrDefault("orderNumber", "");

        sendAndLog(event, email,
                "Order Confirmed — " + orderNumber,
                buildHtml("Your order is confirmed!", "We're preparing your order for shipment.", "", ""),
                rawPayload);
    }

    private void sendShippingNotification(BaseEvent event, Map<String, Object> data, String rawPayload) {
        String email = (String) data.getOrDefault("email", "");
        String orderNumber = (String) data.getOrDefault("orderNumber", "");

        sendAndLog(event, email,
                "Order Shipped — " + orderNumber,
                buildHtml("Your order has been shipped! 🚚",
                        "Order <strong>" + orderNumber + "</strong> is on its way.",
                        "", "You'll receive tracking details shortly."),
                rawPayload);
    }

    private void sendDeliveryNotification(BaseEvent event, Map<String, Object> data, String rawPayload) {
        String email = (String) data.getOrDefault("email", "");
        String orderNumber = (String) data.getOrDefault("orderNumber", "");

        sendAndLog(event, email,
                "Order Delivered — " + orderNumber,
                buildHtml("Delivered! 📦",
                        "Your order <strong>" + orderNumber + "</strong> has been delivered.",
                        "", "Thank you for shopping with us!"),
                rawPayload);
    }

    private void sendCancellationNotification(BaseEvent event, Map<String, Object> data, String rawPayload) {
        String email = (String) data.getOrDefault("email", "");
        String orderNumber = (String) data.getOrDefault("orderNumber", "");

        sendAndLog(event, email,
                "Order Cancelled — " + orderNumber,
                buildHtml("Order Cancelled",
                        "Your order <strong>" + orderNumber + "</strong> has been cancelled.",
                        "", "If you paid, your refund will be processed within 5-7 business days."),
                rawPayload);
    }

    // ── Payment Notifications ──

    private void sendPaymentReceipt(BaseEvent event, Map<String, Object> data, String rawPayload) {
        String email = (String) data.getOrDefault("email", "");
        Object amount = data.getOrDefault("amount", "0");

        sendAndLog(event, email,
                "Payment Received — ₹" + amount,
                buildHtml("Payment Successful! ✅",
                        "We've received your payment of <strong>₹" + amount + "</strong>.",
                        "", "Your order is being processed."),
                rawPayload);
    }

    private void sendPaymentFailedNotification(BaseEvent event, Map<String, Object> data, String rawPayload) {
        String email = (String) data.getOrDefault("email", "");

        sendAndLog(event, email,
                "Payment Failed",
                buildHtml("Payment Failed ❌",
                        "Your payment could not be processed.", "",
                        "Please try again or use a different payment method."),
                rawPayload);
    }

    // ── User Notifications ──

    private void sendWelcomeEmail(BaseEvent event, Map<String, Object> data, String rawPayload) {
        String email = (String) data.getOrDefault("email", "");
        String fullName = (String) data.getOrDefault("fullName", "");

        sendAndLog(event, email,
                "Welcome to E-Commerce! 🎉",
                buildHtml("Welcome, " + fullName + "!",
                        "Thank you for joining us.", "",
                        "Start exploring our products and enjoy shopping!"),
                rawPayload);
    }

    // ── Ops Notifications ──

    private void sendLowStockAlert(BaseEvent event, Map<String, Object> data, String rawPayload) {
        String opsEmail = "ops@ecommerce.com";
        String variantId = (String) data.getOrDefault("variantId", "?");
        Object stock = data.getOrDefault("availableStock", "?");

        sendAndLog(event, opsEmail,
                "⚠️ Low Stock Alert — Variant " + variantId,
                buildHtml("Low Stock Alert",
                        "Variant <strong>" + variantId + "</strong> has only <strong>" + stock
                                + "</strong> units left.",
                        "", "Please restock immediately."),
                rawPayload);
    }

    // ── Helpers ──

    private void sendAndLog(BaseEvent event, String to, String subject, String body, String rawPayload) {
        NotificationLog logEntry = NotificationLog.builder()
                .userId((String) event.getData().getOrDefault("userId", null))
                .eventType(event.getEventType())
                .recipient(to)
                .subject(subject)
                .body(body)
                .idempotencyKey(event.getIdempotencyKey())
                .eventPayload(rawPayload)
                .build();

        try {
            if (to != null && !to.isBlank()) {
                emailService.sendEmail(to, subject, body);
                logEntry.setStatus(NotificationLog.NotificationStatus.SENT);
                logEntry.setSentAt(LocalDateTime.now());
            } else {
                log.warn("No recipient email for event: type={}, eventId={}", event.getEventType(), event.getEventId());
                logEntry.setStatus(NotificationLog.NotificationStatus.FAILED);
                logEntry.setFailureReason("No recipient email");
            }
        } catch (Exception e) {
            logEntry.setStatus(NotificationLog.NotificationStatus.FAILED);
            logEntry.setFailureReason(e.getMessage());
            log.error("Notification send failed: event={}, to={}", event.getEventType(), to, e);
        }

        notificationLogRepo.save(logEntry);
    }

    private String buildHtml(String heading, String line1, String line2, String line3) {
        return """
                <html>
                <body style="font-family:Arial,sans-serif;padding:20px;background:#f9f9f9;">
                  <div style="max-width:600px;margin:0 auto;background:white;padding:30px;border-radius:10px;box-shadow:0 2px 10px rgba(0,0,0,0.1);">
                    <h2 style="color:#333;">%s</h2>
                    <p style="color:#555;font-size:16px;">%s</p>
                    <p style="color:#555;font-size:16px;font-weight:bold;">%s</p>
                    <p style="color:#888;font-size:14px;">%s</p>
                    <hr style="border:none;border-top:1px solid #eee;margin:20px 0;">
                    <p style="color:#aaa;font-size:12px;">E-Commerce Platform</p>
                  </div>
                </body>
                </html>
                """
                .formatted(heading, line1, line2, line3);
    }
}
