package com.estacionamento.api.domain.resident;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ResidentRepository extends JpaRepository<Resident, Long> {
    @EntityGraph(attributePaths = "apartment")
    List<Resident> findAllByApartmentIdOrderByFullNameAsc(Long apartmentId);
    List<Resident> findAllByIdIn(Collection<Long> ids);

    @EntityGraph(attributePaths = "apartment")
    Optional<Resident> findWithApartmentById(Long id);

    long countByActiveTrue();
}
