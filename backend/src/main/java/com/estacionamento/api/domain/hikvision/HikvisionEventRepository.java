package com.estacionamento.api.domain.hikvision;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HikvisionEventRepository extends JpaRepository<HikvisionEvent, Long> {

    Optional<HikvisionEvent> findByCamera_IdAndExternalEventId(Long cameraId, String externalEventId);

    boolean existsBySourceIpAddressAndExternalEventId(String sourceIpAddress, String externalEventId);

    Optional<HikvisionEvent> findBySourceIpAddressAndExternalEventId(String sourceIpAddress, String externalEventId);

    Optional<HikvisionEvent> findTop1ByCameraIdOrderByOccurredAtDesc(Long cameraId);

    List<HikvisionEvent> findTop20ByOrderByOccurredAtDesc();
}
