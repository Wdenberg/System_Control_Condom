package com.estacionamento.api.domain.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

    @EntityGraph(attributePaths = "user")
    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = "user")
    Page<AuditLog> findAll(org.springframework.data.jpa.domain.Specification<AuditLog> specification, Pageable pageable);
}
