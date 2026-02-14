package com.ecom.catalog.controller;

import com.ecom.common.dto.ApiResponse;
import com.ecom.catalog.entity.Inventory;
import com.ecom.catalog.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Internal inventory endpoints — called by Order Service.
 */
@RestController
@RequestMapping("/internal/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping("/reserve")
    public ResponseEntity<ApiResponse<Void>> reserveStock(@RequestBody Map<String, Object> request) {
        inventoryService.reserveStock(
                (String) request.get("variantId"),
                (String) request.get("sellerId"),
                (Integer) request.get("quantity"));
        return ResponseEntity.ok(ApiResponse.ok("Stock reserved", null));
    }

    @PostMapping("/release")
    public ResponseEntity<ApiResponse<Void>> releaseStock(@RequestBody Map<String, Object> request) {
        inventoryService.releaseStock(
                (String) request.get("variantId"),
                (String) request.get("sellerId"),
                (Integer) request.get("quantity"));
        return ResponseEntity.ok(ApiResponse.ok("Stock released", null));
    }

    @GetMapping("/{variantId}/{sellerId}")
    public ResponseEntity<ApiResponse<Inventory>> getInventory(
            @PathVariable String variantId, @PathVariable String sellerId) {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.getInventory(variantId, sellerId)));
    }
}
