package com.devstack.pos.view.tm;

import javafx.scene.control.Button;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CustomerTm {
    private Long id;
    private String name;
    private String contact;
    private double totalSpent;
    private Button deleteButton;
}
