package com.roze.platform;

import java.io.*;
import java.util.concurrent.TimeUnit;

public class LinuxActivityMonitor implements ActivityMonitor {

    private boolean hasXdotool = false;
    private boolean hasXprintidle = false;

    @Override
    public void init() {
        hasXdotool = commandExists("xdotool");
        hasXprintidle = commandExists("xprintidle");
        
        // Check if running under Wayland
        boolean isWayland = System.getenv("XDG_SESSION_TYPE") != null &&
                System.getenv("XDG_SESSION_TYPE").equalsIgnoreCase("wayland");
        
        if (isWayland) {
            hasXdotool = false;
            hasXprintidle = false;
        }
    }

    @Override
    public String getActiveWindowTitle() {
        if (hasXdotool) {
            String windowId = executeCommand("xdotool", "getactivewindow");
            if (!windowId.isEmpty()) {
                return executeCommand("xdotool", "getwindowname", windowId.trim());
            }
        }
        
        // Try wmctrl as fallback
        String windowInfo = executeCommand("wmctrl", "-l", "-p");
        if (!windowInfo.isEmpty()) {
            String[] lines = windowInfo.split("\n");
            for (String line : lines) {
                if (line.contains(getActivePid())) {
                    String[] parts = line.split("\\s+", 5);
                    if (parts.length >= 5) {
                        return parts[4];
                    }
                }
            }
        }
        return "";
    }

    @Override
    public String getActiveProcessName() {
        String pid = getActivePid();
        if (!pid.isEmpty()) {
            String comm = executeCommand("cat", "/proc/" + pid + "/comm");
            return comm.trim();
        }
        return "";
    }

    @Override
    public long getIdleTimeMillis() {
        if (hasXprintidle) {
            String idleStr = executeCommand("xprintidle");
            try {
                return Long.parseLong(idleStr.trim());
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        
        // Fallback: check if X server is running
        String idleStr = executeCommand("xset", "-q");
        if (idleStr.contains("idle")) {
            try {
                String[] parts = idleStr.split("idle:");
                if (parts.length > 1) {
                    String idlePart = parts[1].trim().split(" ")[0];
                    return Long.parseLong(idlePart);
                }
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    private String getActivePid() {
        if (hasXdotool) {
            String windowId = executeCommand("xdotool", "getactivewindow");
            if (!windowId.isEmpty()) {
                String pidOutput = executeCommand("xdotool", "getwindowpid", windowId.trim());
                if (!pidOutput.isEmpty()) {
                    return pidOutput.trim();
                }
            }
        }
        
        // Try wmctrl
        String windowInfo = executeCommand("wmctrl", "-l", "-p");
        if (!windowInfo.isEmpty()) {
            String[] lines = windowInfo.split("\n");
            for (String line : lines) {
                if (line.contains(" 0 ")) { // Active window often has workspace 0
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 3) {
                        return parts[2];
                    }
                }
            }
        }
        return "";
    }

    private String executeCommand(String... command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (output.length() > 0) output.append("\n");
                    output.append(line);
                }
            }
            
            boolean finished = process.waitFor(2, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return "";
            }
            return output.toString().trim();
        } catch (Exception e) {
            return "";
        }
    }

    private boolean commandExists(String command) {
        try {
            Process process = new ProcessBuilder("which", command)
                    .redirectErrorStream(true)
                    .start();
            boolean exited = process.waitFor(1, TimeUnit.SECONDS);
            boolean exists = exited && process.exitValue() == 0;
            process.destroy();
            return exists;
        } catch (Exception e) {
            return false;
        }
    }
}