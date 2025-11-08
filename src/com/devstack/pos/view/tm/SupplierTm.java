package com.devstack.pos.view.tm;

import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SupplierTm {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String address;
    private String contactPerson;
    private String status;
    private String productName;
    private HBox actionButtons;
}

