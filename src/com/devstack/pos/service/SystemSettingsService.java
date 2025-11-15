package com.devstack.pos.service;

import com.devstack.pos.entity.SystemSettings;
import com.devstack.pos.repository.SystemSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class SystemSettingsService {
    
    private final SystemSettingsRepository systemSettingsRepository;
    
    public SystemSettings getSystemSettings() {
        return systemSettingsRepository.findByIsActiveTrue()
                .orElseGet(() -> {
                    // Create default settings if none exist
                    SystemSettings defaultSettings = new SystemSettings(
                            "Kumara Enterprises POS System",
                            "",
                            "",
                            "",
                            "",
                            "Thank you for your business!"
                    );
                    return systemSettingsRepository.save(defaultSettings);
                });
    }
    
    public SystemSettings saveOrUpdateSystemSettings(SystemSettings settings) {
        // Deactivate any existing active settings
        systemSettingsRepository.findByIsActiveTrue()
                .ifPresent(existing -> {
                    existing.setIsActive(false);
                    systemSettingsRepository.save(existing);
                });
        
        // Set new settings as active
        settings.setIsActive(true);
        return systemSettingsRepository.save(settings);
    }
    
    public boolean updateSystemSettings(SystemSettings settings) {
        SystemSettings existing = getSystemSettings();
        if (existing != null) {
            existing.setBusinessName(settings.getBusinessName());
            existing.setAddress(settings.getAddress());
            existing.setContactNumber(settings.getContactNumber());
            existing.setEmail(settings.getEmail());
            existing.setTaxNumber(settings.getTaxNumber());
            existing.setFooterMessage(settings.getFooterMessage());
            systemSettingsRepository.save(existing);
            return true;
        }
        return false;
    }
}

