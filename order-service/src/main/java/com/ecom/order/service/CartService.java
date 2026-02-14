package com.ecom.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Cart backed by Redis — no MySQL table needed.
 * Key format: cart:{userId}
 * Hash field: productId:variantId
 * Hash value: JSON with quantity, productName, price
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final Duration CART_TTL = Duration.ofHours(24);

    public void addItem(String userId, String productId, String variantId,
            String productName, double price, int quantity) {
        String cartKey = getCartKey(userId);
        String itemKey = productId + ":" + (variantId != null ? variantId : "default");

        Map<String, Object> item = new HashMap<>();
        item.put("productId", productId);
        item.put("variantId", variantId);
        item.put("productName", productName);
        item.put("price", price);
        item.put("quantity", quantity);

        try {
            redisTemplate.opsForHash().put(cartKey, itemKey, objectMapper.writeValueAsString(item));
            redisTemplate.expire(cartKey, CART_TTL);
            log.info("Cart item added: userId={}, product={}, qty={}", userId, productId, quantity);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize cart item", e);
        }
    }

    public void updateQuantity(String userId, String productId, String variantId, int quantity) {
        String cartKey = getCartKey(userId);
        String itemKey = productId + ":" + (variantId != null ? variantId : "default");

        String itemJson = (String) redisTemplate.opsForHash().get(cartKey, itemKey);
        if (itemJson == null) {
            throw new RuntimeException("Item not found in cart");
        }

        try {
            Map<String, Object> item = objectMapper.readValue(itemJson, Map.class);
            item.put("quantity", quantity);
            redisTemplate.opsForHash().put(cartKey, itemKey, objectMapper.writeValueAsString(item));
            log.info("Cart item updated: userId={}, product={}, qty={}", userId, productId, quantity);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to update cart item", e);
        }
    }

    public void removeItem(String userId, String productId, String variantId) {
        String cartKey = getCartKey(userId);
        String itemKey = productId + ":" + (variantId != null ? variantId : "default");
        redisTemplate.opsForHash().delete(cartKey, itemKey);
        log.info("Cart item removed: userId={}, product={}", userId, productId);
    }

    public Map<Object, Object> getCart(String userId) {
        return redisTemplate.opsForHash().entries(getCartKey(userId));
    }

    public void clearCart(String userId) {
        redisTemplate.delete(getCartKey(userId));
        log.info("Cart cleared: userId={}", userId);
    }

    public Long getCartSize(String userId) {
        return redisTemplate.opsForHash().size(getCartKey(userId));
    }

    private String getCartKey(String userId) {
        return "cart:" + userId;
    }
}
