package com.roze.platform;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class PlatformFactory {

    @Bean
    public ActivityMonitor activityMonitor() {
        String os = System.getProperty("os.name").toLowerCase();
        ActivityMonitor monitor;
        
        if (os.contains("win")) {
            log.info("Detected Windows OS");
            monitor = new WindowsActivityMonitor();
        } else if (os.contains("mac") || os.contains("darwin")) {
            log.info("Detected macOS");
            monitor = new MacActivityMonitor();
        } else if (os.contains("nix") || os.contains("nux")) {
            log.info("Detected Linux OS");
            monitor = new LinuxActivityMonitor();
        } else {
            log.warn("Unsupported OS: {}, using fallback", os);
            monitor = new LinuxActivityMonitor(); // fallback
        }
        
        monitor.init();
        return monitor;
    }
}