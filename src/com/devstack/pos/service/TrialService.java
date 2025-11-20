package com.devstack.pos.service;

import com.devstack.pos.entity.SystemSettings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Service to manage trial version functionality
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TrialService {
    
    private final SystemSettingsService systemSettingsService;
    
    /**
     * Check if trial version is enabled
     * @return true if trial is enabled, false otherwise
     */
    public boolean isTrialEnabled() {
        try {
            SystemSettings settings = systemSettingsService.getSystemSettings();
            return settings != null && Boolean.TRUE.equals(settings.getTrialEnabled());
        } catch (Exception e) {
            log.error("Error checking trial enabled status", e);
            return false;
        }
    }
    
    /**
     * Check if trial period has expired
     * @return true if trial is expired, false otherwise
     */
    public boolean isTrialExpired() {
        try {
            SystemSettings settings = systemSettingsService.getSystemSettings();
            if (settings == null || !Boolean.TRUE.equals(settings.getTrialEnabled())) {
                return false; // Trial not enabled, so not expired
            }
            
            LocalDate trialEndDate = settings.getTrialEndDate();
            if (trialEndDate == null) {
                return false; // No end date set, consider as not expired
            }
            
            LocalDate today = LocalDate.now();
            boolean expired = today.isAfter(trialEndDate);
            
            if (expired) {
                log.warn("Trial period expired. End date: {}, Today: {}", trialEndDate, today);
            }
            
            return expired;
        } catch (Exception e) {
            log.error("Error checking trial expiration", e);
            return false; // On error, don't block access
        }
    }
    
    /**
     * Check if system is in trial mode and still valid
     * @return true if trial is active and not expired, false otherwise
     */
    public boolean isTrialActive() {
        return isTrialEnabled() && !isTrialExpired();
    }
    
    /**
     * Get trial end date as formatted string
     * @return formatted date string or "Not set" if null
     */
    public String getTrialEndDateFormatted() {
        try {
            SystemSettings settings = systemSettingsService.getSystemSettings();
            if (settings != null && settings.getTrialEndDate() != null) {
                return settings.getTrialEndDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            }
            return "Not set";
        } catch (Exception e) {
            log.error("Error getting trial end date", e);
            return "Not set";
        }
    }
    
    /**
     * Get days remaining in trial
     * @return number of days remaining, or -1 if trial not enabled/expired
     */
    public long getDaysRemaining() {
        try {
            SystemSettings settings = systemSettingsService.getSystemSettings();
            if (settings == null || !Boolean.TRUE.equals(settings.getTrialEnabled())) {
                return -1; // Trial not enabled
            }
            
            LocalDate trialEndDate = settings.getTrialEndDate();
            if (trialEndDate == null) {
                return -1; // No end date set
            }
            
            LocalDate today = LocalDate.now();
            if (today.isAfter(trialEndDate)) {
                return 0; // Expired
            }
            
            return java.time.temporal.ChronoUnit.DAYS.between(today, trialEndDate);
        } catch (Exception e) {
            log.error("Error calculating days remaining", e);
            return -1;
        }
    }
    
    /**
     * Check if trial is in warning period (7 days or less remaining)
     * @return true if 7 days or less remain, false otherwise
     */
    public boolean isTrialWarningPeriod() {
        if (!isTrialEnabled() || isTrialExpired()) {
            return false;
        }
        
        long daysRemaining = getDaysRemaining();
        return daysRemaining >= 0 && daysRemaining <= 7;
    }
    
    /**
     * Get trial status message
     * @return status message string
     */
    public String getTrialStatusMessage() {
        if (!isTrialEnabled()) {
            return "Trial version is disabled";
        }
        
        if (isTrialExpired()) {
            long daysOverdue = java.time.temporal.ChronoUnit.DAYS.between(
                systemSettingsService.getSystemSettings().getTrialEndDate(),
                LocalDate.now()
            );
            return String.format("Trial period expired %d day(s) ago", daysOverdue);
        }
        
        long daysRemaining = getDaysRemaining();
        if (daysRemaining > 0) {
            return String.format("Trial active - %d day(s) remaining", daysRemaining);
        } else if (daysRemaining == 0) {
            return "Trial expires today";
        }
        
        return "Trial status unknown";
    }
}

