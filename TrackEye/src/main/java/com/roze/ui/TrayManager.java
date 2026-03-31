package com.roze.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.net.URI;

@Slf4j
@Component
public class TrayManager {

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        if (!SystemTray.isSupported() || GraphicsEnvironment.isHeadless()) {
            log.info("System tray not supported - running headless");
            return;
        }
        
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(new FlatDarkLaf());
                
                SystemTray tray = SystemTray.getSystemTray();
                Image icon = createTrayIcon();
                
                PopupMenu menu = new PopupMenu();
                
                MenuItem dashboardItem = new MenuItem("Open Dashboard");
                dashboardItem.addActionListener(e -> openBrowser("http://localhost:8765/api/stats/today"));
                
                MenuItem statusItem = new MenuItem("Status: Active");
                statusItem.setEnabled(false);
                
                MenuItem exitItem = new MenuItem("Exit");
                exitItem.addActionListener(e -> {
                    log.info("Exiting TrackEye");
                    System.exit(0);
                });
                
                menu.add(dashboardItem);
                menu.addSeparator();
                menu.add(statusItem);
                menu.addSeparator();
                menu.add(exitItem);
                
                TrayIcon trayIcon = new TrayIcon(icon, "TrackEye", menu);
                trayIcon.setImageAutoSize(true);
                trayIcon.addActionListener(e -> openBrowser("http://localhost:8765/api/stats/today"));
                
                tray.add(trayIcon);
                log.info("System tray icon installed");
                
            } catch (Exception e) {
                log.error("Failed to setup system tray", e);
            }
        });
    }
    
    private Image createTrayIcon() {
        // Create a simple 16x16 icon programmatically
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(0x1D9E75));
        g.fillRoundRect(0, 0, 16, 16, 4, 4);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 10));
        g.drawString("T", 5, 12);
        g.dispose();
        return image;
    }
    
    private void openBrowser(String url) {
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception e) {
            log.error("Failed to open browser", e);
        }
    }
}