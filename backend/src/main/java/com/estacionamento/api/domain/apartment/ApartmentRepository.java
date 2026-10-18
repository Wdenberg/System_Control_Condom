package com.estacionamento.api.domain.apartment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApartmentRepository extends JpaRepository<Apartment, Long> {
    boolean existsByNumber(String number);
    boolean existsByNumberAndIdNot(String number, Long id);
    Optional<Apartment> findByNumber(String number);
    List<Apartment> findAllByOrderByNumberAsc();
    long countByActiveTrue();
}
