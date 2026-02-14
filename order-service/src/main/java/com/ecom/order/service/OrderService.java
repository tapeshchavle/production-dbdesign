package com.ecom.order.service;

import com.ecom.common.dto.ApiResponse;
import com.ecom.common.event.BaseEvent;
import com.ecom.common.event.EventTypes;
import com.ecom.common.event.TopicNames;
import com.ecom.common.exception.ConflictException;
import com.ecom.common.exception.ResourceNotFoundException;
import com.ecom.order.entity.Order;
import com.ecom.order.entity.OrderStatusHistory;
import com.ecom.order.repository.OrderRepository;
import com.ecom.order.repository.OrderStatusHistoryRepository;
import io.awspring.cloud.sns.core.SnsTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository statusHistoryRepo;
    private final SnsTemplate snsTemplate;

    /**
     * Create a new order — idempotent via idempotency key.
     */
    @Transactional
    public Order createOrder(Order order) {
        // Idempotency check
        if (order.getIdempotencyKey() != null) {
            Optional<Order> existing = orderRepository.findByIdempotencyKey(order.getIdempotencyKey());
            if (existing.isPresent()) {
                log.info("Duplicate order detected, returning existing: idempotencyKey={}",
                        order.getIdempotencyKey());
                return existing.get();
            }
        }

        order = orderRepository.save(order);
        log.info("Order created: id={}, number={}, total={}",
                order.getId(), order.getOrderNumber(), order.getTotalAmount());

        // Record status history
        recordStatusChange(order.getId(), null, "PENDING", null, "Order placed");

        // Publish ORDER_CREATED event
        publishOrderEvent(order, EventTypes.ORDER_CREATED);

        return order;
    }

    @Transactional(readOnly = true)
    public Order getOrderById(String id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));
    }

    @Transactional(readOnly = true)
    public Order getOrderByNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderNumber));
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersByUser(String userId) {
        return orderRepository.findByUserId(userId);
    }

    /**
     * Update order status and record in history.
     */
    @Transactional
    public Order updateStatus(String orderId, Order.OrderStatus newStatus, String changedBy, String note) {
        Order order = getOrderById(orderId);
        String oldStatus = order.getStatus().name();
        order.setStatus(newStatus);
        order = orderRepository.save(order);

        recordStatusChange(orderId, oldStatus, newStatus.name(), changedBy, note);

        // Publish appropriate event
        String eventType = switch (newStatus) {
            case CONFIRMED -> EventTypes.ORDER_CONFIRMED;
            case SHIPPED -> EventTypes.ORDER_SHIPPED;
            case DELIVERED -> EventTypes.ORDER_DELIVERED;
            case CANCELLED -> EventTypes.ORDER_CANCELLED;
            default -> null;
        };

        if (eventType != null) {
            publishOrderEvent(order, eventType);
        }

        log.info("Order status updated: id={}, {} → {}", orderId, oldStatus, newStatus);
        return order;
    }

    @Transactional(readOnly = true)
    public List<OrderStatusHistory> getOrderTimeline(String orderId) {
        return statusHistoryRepo.findByOrderIdOrderByCreatedAtAsc(orderId);
    }

    // ── Helpers ──

    private void recordStatusChange(String orderId, String from, String to, String changedBy, String note) {
        statusHistoryRepo.save(OrderStatusHistory.builder()
                .orderId(orderId)
                .fromStatus(from)
                .toStatus(to)
                .changedBy(changedBy)
                .note(note)
                .build());
    }

    private void publishOrderEvent(Order order, String eventType) {
        try {
            BaseEvent event = BaseEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType(eventType)
                    .source("order-service")
                    .timestamp(Instant.now())
                    .correlationId(UUID.randomUUID().toString())
                    .idempotencyKey(order.getIdempotencyKey())
                    .data(Map.of(
                            "orderId", order.getId(),
                            "orderNumber", order.getOrderNumber(),
                            "userId", order.getUserId(),
                            "totalAmount", order.getTotalAmount(),
                            "status", order.getStatus().name()))
                    .build();

            snsTemplate.convertAndSend(TopicNames.ORDER_EVENTS, event);
            log.info("Published {} event for orderId={}", eventType, order.getId());
        } catch (Exception e) {
            log.error("Failed to publish {} event for orderId={}", eventType, order.getId(), e);
        }
    }
}
