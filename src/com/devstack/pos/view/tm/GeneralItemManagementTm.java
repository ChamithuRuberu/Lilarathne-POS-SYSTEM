package com.devstack.pos.view.tm;

import javafx.scene.control.Button;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeneralItemManagementTm {
    private Long id;
    private String name;
    private Button delete;
}

