package com.openfinova.banking.identity.repository;

import com.openfinova.banking.identity.entity.BankingRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<BankingRole, UUID> {
    Optional<BankingRole> findByName(String name);

    boolean existsByName(String name);

    Set<BankingRole> findByNameIn(Collection<String> names);
}
