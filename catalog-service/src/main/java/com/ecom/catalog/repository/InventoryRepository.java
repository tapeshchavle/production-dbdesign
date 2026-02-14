package com.ecom.catalog.repository;

import com.ecom.catalog.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, String> {
    Optional<Inventory> findByVariantIdAndSellerId(String variantId, String sellerId);

    List<Inventory> findByVariantId(String variantId);

    List<Inventory> findBySellerId(String sellerId);
}
