package com.ecom.order.controller;

import com.ecom.common.dto.ApiResponse;
import com.ecom.order.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> addItem(
            @PathVariable String userId, @RequestBody Map<String, Object> item) {
        cartService.addItem(
                userId,
                (String) item.get("productId"),
                (String) item.get("variantId"),
                (String) item.get("productName"),
                ((Number) item.get("price")).doubleValue(),
                ((Number) item.get("quantity")).intValue());
        return ResponseEntity.ok(ApiResponse.ok("Item added to cart", null));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<Map<Object, Object>>> getCart(@PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.ok(cartService.getCart(userId)));
    }

    @DeleteMapping("/{userId}/{productId}")
    public ResponseEntity<ApiResponse<Void>> removeItem(
            @PathVariable String userId, @PathVariable String productId,
            @RequestParam(required = false) String variantId) {
        cartService.removeItem(userId, productId, variantId);
        return ResponseEntity.ok(ApiResponse.ok("Item removed", null));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> clearCart(@PathVariable String userId) {
        cartService.clearCart(userId);
        return ResponseEntity.ok(ApiResponse.ok("Cart cleared", null));
    }
}
