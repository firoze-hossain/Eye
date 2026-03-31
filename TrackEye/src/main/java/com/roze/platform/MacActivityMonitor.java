package com.roze.platform;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class MacActivityMonitor implements ActivityMonitor {

    @Override
    public String getActiveWindowTitle() {
        String script = "tell application \"System Events\"\n" +
                "    set frontApp to name of first application process whose frontmost is true\n" +
                "    tell process frontApp\n" +
                "        try\n" +
                "            return name of front window\n" +
                "        on error\n" +
                "            return frontApp\n" +
                "        end try\n" +
                "    end tell\n" +
                "end tell";
        return runAppleScript(script);
    }

    @Override
    public String getActiveProcessName() {
        String script = "tell application \"System Events\"\n" +
                "    return name of first application process whose frontmost is true\n" +
                "end tell";
        String name = runAppleScript(script);
        return name.isEmpty() ? "" : name;
    }

    @Override
    public long getIdleTimeMillis() {
        try {
            // Get HIDIdleTime in nanoseconds
            Process process = new ProcessBuilder("ioreg", "-c", "IOHIDSystem")
                    .redirectErrorStream(true)
                    .start();
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("HIDIdleTime")) {
                        String[] parts = line.split("=");
                        if (parts.length > 1) {
                            String idleStr = parts[1].trim();
                            try {
                                long nanoSeconds = Long.parseLong(idleStr, 16);
                                return nanoSeconds / 1_000_000; // Convert to milliseconds
                            } catch (NumberFormatException e) {
                                return 0;
                            }
                        }
                    }
                }
            }
            process.waitFor(2, TimeUnit.SECONDS);
        } catch (Exception e) {
            // Fallback to using AppleScript for idle time
            String idleSeconds = runAppleScript("tell application \"System Events\" to return (current application)'s idle time");
            try {
                return Long.parseLong(idleSeconds.trim()) * 1000;
            } catch (NumberFormatException e2) {
                return 0;
            }
        }
        return 0;
    }

    private String runAppleScript(String script) {
        try {
            Process process = new ProcessBuilder("osascript", "-e", script)
                    .redirectErrorStream(true)
                    .start();
            
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
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
}