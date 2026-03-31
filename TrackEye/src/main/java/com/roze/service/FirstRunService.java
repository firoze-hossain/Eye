package com.roze.service;

import com.roze.config.AppConfig;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class FirstRunService {

    private final AppConfig config;

    @PostConstruct
    public void checkFirstRun() {
        Path marker = Paths.get(config.getStoragePath(), ".installed");
        
        if (Files.exists(marker)) {
            log.debug("Not first run - skipping setup");
            return;
        }
        
        log.info("First run detected - performing initial setup");
        
        try {
            // Create storage directory
            Files.createDirectories(Paths.get(config.getStoragePath()));
            
            // Register auto-start
            registerAutoStart();
            
            // Create marker file
            Files.writeString(marker, Instant.now().toString());
            
            log.info("First run setup completed successfully");
        } catch (Exception e) {
            log.error("Failed to complete first run setup", e);
        }
    }
    
    private void registerAutoStart() {
        String os = System.getProperty("os.name").toLowerCase();
        
        try {
            if (os.contains("win")) {
                registerWindowsAutoStart();
            } else if (os.contains("mac")) {
                registerMacAutoStart();
            } else if (os.contains("nix") || os.contains("nux")) {
                registerLinuxAutoStart();
            }
        } catch (Exception e) {
            log.error("Failed to register auto-start", e);
        }
    }
    
    private void registerWindowsAutoStart() throws IOException {
        String appPath = ProcessHandle.current()
            .info()
            .command()
            .orElse("");
        
        if (appPath.isEmpty()) return;
        
        String[] cmd = {
            "reg", "add",
            "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run",
            "/v", "TrackEye",
            "/t", "REG_SZ",
            "/d", "\"" + appPath + "\"",
            "/f"
        };
        
        Runtime.getRuntime().exec(cmd);
        log.info("Windows auto-start registered");
    }
    
    private void registerMacAutoStart() throws IOException {
        Path plistPath = Paths.get(System.getProperty("user.home"),
            "Library", "LaunchAgents", "com.roze.trackeye.plist");
        
        String plist = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
            <plist version="1.0">
            <dict>
                <key>Label</key>
                <string>com.roze.trackeye</string>
                <key>ProgramArguments</key>
                <array>
                    <string>/Applications/TrackEye.app/Contents/MacOS/TrackEye</string>
                </array>
                <key>RunAtLoad</key>
                <true/>
                <key>KeepAlive</key>
                <true/>
            </dict>
            </plist>
            """;
        
        Files.createDirectories(plistPath.getParent());
        Files.writeString(plistPath, plist);
        
        try {
            new ProcessBuilder("launchctl", "load", plistPath.toString()).start();
        } catch (IOException e) {
            log.warn("Could not load launch agent: {}", e.getMessage());
        }
        
        log.info("macOS auto-start registered");
    }
    
    private void registerLinuxAutoStart() throws IOException {
        Path autostartDir = Paths.get(System.getProperty("user.home"),
            ".config", "autostart");
        Path desktopFile = autostartDir.resolve("trackeye.desktop");
        
        String appPath = ProcessHandle.current()
            .info()
            .command()
            .orElse("/opt/trackeye/bin/trackeye");
        
        String desktop = """
            [Desktop Entry]
            Type=Application
            Name=TrackEye
            Exec=%s
            Hidden=false
            NoDisplay=false
            X-GNOME-Autostart-enabled=true
            """.formatted(appPath);
        
        Files.createDirectories(autostartDir);
        Files.writeString(desktopFile, desktop);
        
        log.info("Linux auto-start registered");
    }
}