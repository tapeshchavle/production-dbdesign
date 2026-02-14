package com.ecom.catalog.service;

import com.ecom.common.event.BaseEvent;
import com.ecom.common.event.EventTypes;
import com.ecom.common.event.TopicNames;
import com.ecom.common.exception.ConflictException;
import com.ecom.common.exception.ResourceNotFoundException;
import com.ecom.catalog.entity.Inventory;
import com.ecom.catalog.repository.InventoryRepository;
import io.awspring.cloud.sns.core.SnsTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final StringRedisTemplate redisTemplate;
    private final SnsTemplate snsTemplate;

    /**
     * Reserve stock for an order — uses distributed lock to prevent overselling.
     */
    @Transactional
    public void reserveStock(String variantId, String sellerId, int quantity) {
        String lockKey = "lock:inventory:" + variantId + ":" + sellerId;

        // Acquire distributed lock
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "locked", Duration.ofSeconds(10));

        if (!Boolean.TRUE.equals(locked)) {
            throw new ConflictException("Unable to acquire lock. Please retry.");
        }

        try {
            Inventory inventory = inventoryRepository.findByVariantIdAndSellerId(variantId, sellerId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Inventory not found for variant=" + variantId + ", seller=" + sellerId));

            if (inventory.getAvailableStock() < quantity) {
                throw new ConflictException(
                        "Insufficient stock. Available: " + inventory.getAvailableStock() + ", Requested: " + quantity);
            }

            inventory.setReserved(inventory.getReserved() + quantity);
            inventoryRepository.save(inventory);

            log.info("Stock reserved: variantId={}, qty={}, available={}",
                    variantId, quantity, inventory.getAvailableStock());

            // Check low stock alert
            if (inventory.getAvailableStock() <= inventory.getReorderLevel()) {
                publishLowStockAlert(inventory);
            }
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    /**
     * Release previously reserved stock (e.g., on order cancellation).
     */
    @Transactional
    public void releaseStock(String variantId, String sellerId, int quantity) {
        Inventory inventory = inventoryRepository.findByVariantIdAndSellerId(variantId, sellerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inventory not found for variant=" + variantId + ", seller=" + sellerId));

        inventory.setReserved(Math.max(0, inventory.getReserved() - quantity));
        inventoryRepository.save(inventory);
        log.info("Stock released: variantId={}, qty={}", variantId, quantity);
    }

    /**
     * Confirm reserved stock (deduct from total after payment).
     */
    @Transactional
    public void confirmReservation(String variantId, String sellerId, int quantity) {
        Inventory inventory = inventoryRepository.findByVariantIdAndSellerId(variantId, sellerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inventory not found for variant=" + variantId + ", seller=" + sellerId));

        inventory.setQuantity(inventory.getQuantity() - quantity);
        inventory.setReserved(Math.max(0, inventory.getReserved() - quantity));
        inventoryRepository.save(inventory);
        log.info("Reservation confirmed: variantId={}, qty={}", variantId, quantity);
    }

    @Transactional(readOnly = true)
    public Inventory getInventory(String variantId, String sellerId) {
        return inventoryRepository.findByVariantIdAndSellerId(variantId, sellerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inventory not found for variant=" + variantId + ", seller=" + sellerId));
    }

    private void publishLowStockAlert(Inventory inventory) {
        try {
            BaseEvent event = BaseEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType(EventTypes.LOW_STOCK_ALERT)
                    .source("catalog-service")
                    .timestamp(Instant.now())
                    .data(Map.of(
                            "variantId", inventory.getVariant().getId(),
                            "sellerId", inventory.getSellerId(),
                            "availableStock", inventory.getAvailableStock(),
                            "reorderLevel", inventory.getReorderLevel()))
                    .build();

            snsTemplate.convertAndSend(TopicNames.CATALOG_EVENTS, event);
            log.warn("LOW_STOCK_ALERT: variantId={}, stock={}", inventory.getVariant().getId(),
                    inventory.getAvailableStock());
        } catch (Exception e) {
            log.error("Failed to publish LOW_STOCK_ALERT", e);
        }
    }
}
