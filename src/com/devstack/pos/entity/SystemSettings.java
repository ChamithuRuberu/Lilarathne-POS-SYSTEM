package com.devstack.pos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "system_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemSettings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @Column(name = "business_name", length = 200)
    private String businessName;
    
    @Column(name = "address", length = 500)
    private String address;
    
    @Column(name = "contact_number", length = 50)
    private String contactNumber;
    
    @Column(name = "email", length = 100)
    private String email;
    
    @Column(name = "tax_number", length = 50)
    private String taxNumber;
    
    @Column(name = "footer_message", length = 200)
    private String footerMessage;
    
    // Single row constraint - only one settings record should exist
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    public SystemSettings(String businessName, String address, String contactNumber, 
                         String email, String taxNumber, String footerMessage) {
        this.businessName = businessName;
        this.address = address;
        this.contactNumber = contactNumber;
        this.email = email;
        this.taxNumber = taxNumber;
        this.footerMessage = footerMessage;
        this.isActive = true;
    }
}

