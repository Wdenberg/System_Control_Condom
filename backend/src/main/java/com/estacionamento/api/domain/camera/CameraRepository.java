package com.estacionamento.api.domain.camera;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CameraRepository extends JpaRepository<Camera, Long> {

    Optional<Camera> findByIpAddressAndEnabledTrue(String ipAddress);

    List<Camera> findAllByOrderByNameAsc();
}
