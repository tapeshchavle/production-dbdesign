package com.ecom.common.event;

/**
 * All event type constants used across services.
 * Prevents typos and ensures consistency.
 */
public final class EventTypes {

    private EventTypes() {
    }

    // Order events
    public static final String ORDER_CREATED = "ORDER_CREATED";
    public static final String ORDER_CONFIRMED = "ORDER_CONFIRMED";
    public static final String ORDER_CANCELLED = "ORDER_CANCELLED";
    public static final String ORDER_SHIPPED = "ORDER_SHIPPED";
    public static final String ORDER_DELIVERED = "ORDER_DELIVERED";
    public static final String PAYMENT_SUCCESS = "PAYMENT_SUCCESS";
    public static final String PAYMENT_FAILED = "PAYMENT_FAILED";
    public static final String REFUND_INITIATED = "REFUND_INITIATED";
    public static final String REFUND_COMPLETED = "REFUND_COMPLETED";

    // User events
    public static final String USER_REGISTERED = "USER_REGISTERED";
    public static final String USER_VERIFIED = "USER_VERIFIED";
    public static final String PASSWORD_RESET = "PASSWORD_RESET";

    // Catalog events
    public static final String LOW_STOCK_ALERT = "LOW_STOCK_ALERT";
    public static final String OUT_OF_STOCK = "OUT_OF_STOCK";
    public static final String PRODUCT_CREATED = "PRODUCT_CREATED";
    public static final String PRODUCT_UPDATED = "PRODUCT_UPDATED";
}
