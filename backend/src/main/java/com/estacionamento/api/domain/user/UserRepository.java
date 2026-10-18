package com.estacionamento.api.domain.user;

import java.util.List;
import java.util.Optional;

import com.estacionamento.api.domain.role.RoleCode;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);

    List<User> findAllByOrderByUsernameAsc();

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from User u where u.role.code=:role and u.active=true order by u.id")
    List<User> lockActiveRoleUsers(@org.springframework.data.repository.query.Param("role") RoleCode role);

    long countByRole_CodeAndActiveTrue(RoleCode roleCode);
}
