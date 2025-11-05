package com.devstack.pos.view.tm;

import javafx.scene.control.Button;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryTm {
    private Integer id;
    private String name;
    private String description;
    private String status;
    private Button delete;
}

