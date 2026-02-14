package com.ecom.catalog.repository;

import com.ecom.catalog.entity.Seller;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SellerRepository extends JpaRepository<Seller, String> {
    Optional<Seller> findByUserId(String userId);

    Optional<Seller> findByStoreSlug(String storeSlug);

    boolean existsByUserId(String userId);
}
