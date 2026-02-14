package com.ecom.user.controller;

import com.ecom.common.dto.ApiResponse;
import com.ecom.user.dto.CreateAddressRequest;
import com.ecom.user.entity.Address;
import com.ecom.user.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users/{userId}/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @PostMapping
    public ResponseEntity<ApiResponse<Address>> createAddress(
            @PathVariable String userId, @Valid @RequestBody CreateAddressRequest request) {
        Address address = addressService.createAddress(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Address created", address));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Address>>> getAddresses(@PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.ok(addressService.getAddressesByUserId(userId)));
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @PathVariable String userId, @PathVariable String addressId) {
        addressService.deleteAddress(addressId);
        return ResponseEntity.ok(ApiResponse.ok("Address deleted", null));
    }
}
