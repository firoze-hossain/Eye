package com.roze;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.swing.*;

@SpringBootApplication
@EnableScheduling
public class TrackEyeApplication {

    public static void main(String[] args) {
        // Enable AWT for system tray
        System.setProperty("java.awt.headless", "false");

        // Set FlatLaf for modern UI
        try {
            UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatDarkLaf());
        } catch (Exception e) {
            // Fallback to system look and feel
        }

        SpringApplication.run(TrackEyeApplication.class, args);
    }
}