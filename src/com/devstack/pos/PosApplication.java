package com.devstack.pos;

import javafx.application.Application;
import lombok.Getter;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class PosApplication {

    private static ConfigurableApplicationContext applicationContext;
    
    public static ConfigurableApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public static void main(String[] args) {
        // Capture ALL exceptions
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            System.err.println("═══════════════════════════════════════════");
            System.err.println("UNCAUGHT EXCEPTION in thread: " + thread.getName());
            System.err.println("═══════════════════════════════════════════");
            throwable.printStackTrace(System.err);
            System.err.println("═══════════════════════════════════════════");
        });

        try {
            // Start Spring Boot context (non-web mode, headless=false for JavaFX)
            applicationContext = new SpringApplicationBuilder(PosApplication.class)
                    .headless(false)
                    .run(args);

            // Set Spring context in Initialize class before launching JavaFX
            Initialize.setSpringContext(applicationContext);

            // Launch JavaFX application
            Application.launch(Initialize.class, args);
        } catch (Exception e) {
            System.err.println("═══════════════════════════════════════════");
            System.err.println("EXCEPTION IN MAIN:");
            System.err.println("═══════════════════════════════════════════");
            e.printStackTrace(System.err);
            System.err.println("═══════════════════════════════════════════");
        }
    }
}