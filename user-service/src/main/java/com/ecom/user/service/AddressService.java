package com.ecom.user.service;

import com.ecom.common.exception.ResourceNotFoundException;
import com.ecom.user.dto.CreateAddressRequest;
import com.ecom.user.entity.Address;
import com.ecom.user.entity.User;
import com.ecom.user.repository.AddressRepository;
import com.ecom.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    @Transactional
    public Address createAddress(String userId, CreateAddressRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // If this is set as default, unset all other defaults
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            addressRepository.findByUserIdAndIsDefaultTrue(userId)
                    .forEach(addr -> {
                        addr.setIsDefault(false);
                        addressRepository.save(addr);
                    });
        }

        Address address = Address.builder()
                .user(user)
                .label(request.getLabel())
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .addressLine1(request.getAddressLine1())
                .addressLine2(request.getAddressLine2())
                .city(request.getCity())
                .state(request.getState())
                .pincode(request.getPincode())
                .country(request.getCountry())
                .isDefault(request.getIsDefault())
                .build();

        address = addressRepository.save(address);
        log.info("Address created: id={} for userId={}", address.getId(), userId);
        return address;
    }

    @Transactional(readOnly = true)
    public List<Address> getAddressesByUserId(String userId) {
        return addressRepository.findByUserId(userId);
    }

    @Transactional
    public void deleteAddress(String addressId) {
        if (!addressRepository.existsById(addressId)) {
            throw new ResourceNotFoundException("Address", addressId);
        }
        addressRepository.deleteById(addressId);
        log.info("Address deleted: id={}", addressId);
    }
}
