package com.ecom.order.controller;

import com.ecom.common.dto.ApiResponse;
import com.ecom.order.entity.Order;
import com.ecom.order.entity.OrderStatusHistory;
import com.ecom.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<ApiResponse<Order>> createOrder(@RequestBody Order order) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Order placed", orderService.createOrder(order)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Order>> getOrder(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getOrderById(id)));
    }

    @GetMapping("/number/{orderNumber}")
    public ResponseEntity<ApiResponse<Order>> getOrderByNumber(@PathVariable String orderNumber) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getOrderByNumber(orderNumber)));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<Order>>> getUserOrders(@PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getOrdersByUser(userId)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Order>> updateStatus(
            @PathVariable String id,
            @RequestParam Order.OrderStatus status,
            @RequestParam(required = false) String changedBy,
            @RequestParam(required = false) String note) {
        return ResponseEntity.ok(
                ApiResponse.ok("Status updated", orderService.updateStatus(id, status, changedBy, note)));
    }

    @GetMapping("/{id}/timeline")
    public ResponseEntity<ApiResponse<List<OrderStatusHistory>>> getTimeline(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getOrderTimeline(id)));
    }
}
