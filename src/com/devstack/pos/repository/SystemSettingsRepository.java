package com.devstack.pos.repository;

import com.devstack.pos.entity.SystemSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SystemSettingsRepository extends JpaRepository<SystemSettings, Long> {
    
    Optional<SystemSettings> findByIsActiveTrue();
    
    boolean existsByIsActiveTrue();
}

