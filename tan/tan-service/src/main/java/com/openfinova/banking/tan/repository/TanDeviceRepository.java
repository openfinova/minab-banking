package com.openfinova.banking.tan.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.openfinova.banking.tan.entity.TanDevice;
import com.openfinova.banking.tan.entity.TanDeviceStatus;

public interface TanDeviceRepository extends JpaRepository<TanDevice, UUID> {

    List<TanDevice> findByUserIdAndStatusNot(UUID userId, TanDeviceStatus status);

    long countByUserIdAndStatusNot(UUID userId, TanDeviceStatus status);

    Optional<TanDevice> findByIdAndUserId(UUID id, UUID userId);

    Optional<TanDevice> findFirstByUserIdAndStatusOrderByEnrolledAtDesc(UUID userId, TanDeviceStatus status);
}
