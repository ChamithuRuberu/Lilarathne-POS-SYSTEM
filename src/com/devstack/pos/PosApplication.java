package com.devstack.pos;

import javafx.application.Application;
import lombok.Getter;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
public class PosApplication {

    @Getter
    private static ConfigurableApplicationContext applicationContext;

    public static void main(String[] args) {
        // Start Spring Boot context (non-web mode, headless=false for JavaFX)
        applicationContext = new SpringApplicationBuilder(PosApplication.class)
                .headless(false)
                .run(args);

        // Set Spring context in Initialize class before launching JavaFX
        Initialize.setSpringContext(applicationContext);

        // Launch JavaFX application
        // This will block until JavaFX application exits
        Application.launch(Initialize.class, args);
    }

}
