package com.roze.platform;

import lombok.extern.slf4j.Slf4j;
import java.io.*;
import java.util.concurrent.TimeUnit;

@Slf4j
public class LinuxActivityMonitor implements ActivityMonitor {

    private boolean hasXdotool = false;
    private boolean hasXprop = false;
    private boolean hasWmctrl = false;
    private boolean isWayland = false;

    @Override
    public void init() {
        isWayland = isWaylandSession();

        if (!isWayland) {
            hasXdotool = commandExists("xdotool");
            hasXprop = commandExists("xprop");
            hasWmctrl = commandExists("wmctrl");
            log.info("Linux Monitor initialized - X11 mode");
            log.info("  xdotool: {}, xprop: {}, wmctrl: {}", hasXdotool, hasXprop, hasWmctrl);
        } else {
            log.warn("Wayland detected. Window tracking will be limited.");
            log.warn("For full functionality, switch to X11 session at login screen.");
        }
    }

    private boolean isWaylandSession() {
        String sessionType = System.getenv("XDG_SESSION_TYPE");
        if (sessionType != null && sessionType.equalsIgnoreCase("wayland")) {
            return true;
        }
        String waylandDisplay = System.getenv("WAYLAND_DISPLAY");
        return waylandDisplay != null && !waylandDisplay.isEmpty();
    }

    @Override
    public String getActiveWindowTitle() {
        if (isWayland) {
            return getActiveWindowTitleWayland();
        }
        return getActiveWindowTitleX11();
    }

    private String getActiveWindowTitleX11() {
        try {
            // Method 1: Using xdotool (fastest)
            if (hasXdotool) {
                String windowId = executeCommand("xdotool", "getactivewindow");
                if (windowId != null && !windowId.isEmpty() && !windowId.contains("Could not")) {
                    String title = executeCommand("xdotool", "getwindowname", windowId.trim());
                    if (title != null && !title.isEmpty() && !title.contains("xdotool:") && title.length() > 2) {
                        log.debug("Window title (xdotool): {}", title);
                        return title;
                    }
                }
            }

            // Method 2: Using xprop (more reliable for some apps)
            if (hasXprop) {
                String activeWindow = executeCommand("xprop", "-root", "_NET_ACTIVE_WINDOW");
                if (activeWindow != null && !activeWindow.isEmpty()) {
                    String windowId = activeWindow.substring(activeWindow.lastIndexOf("0x"));
                    String title = executeCommand("xprop", "-id", windowId, "WM_NAME");
                    if (title != null && !title.isEmpty()) {
                        int start = title.indexOf("\"");
                        int end = title.lastIndexOf("\"");
                        if (start != -1 && end != -1 && start < end) {
                            String parsedTitle = title.substring(start + 1, end);
                            if (!parsedTitle.isEmpty()) {
                                log.debug("Window title (xprop): {}", parsedTitle);
                                return parsedTitle;
                            }
                        }
                    }
                }
            }

            // Method 3: Using wmctrl
            if (hasWmctrl) {
                String output = executeCommand("wmctrl", "-l", "-p");
                if (output != null && !output.isEmpty()) {
                    String[] lines = output.split("\n");
                    for (String line : lines) {
                        if (line != null && line.contains(" 0 ")) {
                            String[] parts = line.split("\\s+", 5);
                            if (parts.length >= 5) {
                                String title = parts[4];
                                if (title != null && !title.isEmpty()) {
                                    log.debug("Window title (wmctrl): {}", title);
                                    return title;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Error getting window title: {}", e.getMessage());
        }
        return "";
    }

    private String getActiveWindowTitleWayland() {
        // On Wayland, we need to use alternative methods
        try {
            // Try to get active window via DBus (works on GNOME Wayland)
            String title = getActiveWindowViaDBus();
            if (title != null && !title.isEmpty()) {
                log.debug("Window title (DBus): {}", title);
                return title;
            }
        } catch (Exception e) {
            log.trace("DBus method failed: {}", e.getMessage());
        }

        // Fallback: Try to get from /proc based on active process
        try {
            String activeProcess = getActiveProcessName();
            if (activeProcess != null && !activeProcess.isEmpty()) {
                // Try to get window title from process
                String title = getWindowTitleFromProcess(activeProcess);
                if (title != null && !title.isEmpty()) {
                    return title;
                }
                return "Active: " + activeProcess;
            }
        } catch (Exception e) {
            log.trace("Process fallback failed: {}", e.getMessage());
        }
        return "";
    }

    private String getActiveWindowViaDBus() {
        try {
            // GNOME Shell DBus query for active window
            ProcessBuilder pb = new ProcessBuilder("gdbus", "call",
                    "--session", "--dest", "org.gnome.Shell",
                    "--object-path", "/org/gnome/Shell",
                    "--method", "org.gnome.Shell.Eval",
                    "global.get_window_actors().find(a => a.meta_window.has_focus())?.meta_window.get_title()");

            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine();
                if (line != null && line.contains("'")) {
                    int start = line.indexOf("'");
                    int end = line.lastIndexOf("'");
                    if (start != -1 && end > start) {
                        return line.substring(start + 1, end);
                    }
                }
            }
            process.waitFor(2, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.trace("DBus query failed: {}", e.getMessage());
        }
        return null;
    }

    private String getWindowTitleFromProcess(String processName) {
        // Try to get window title from process list
        try {
            // For known applications, map to common window titles
            switch (processName.toLowerCase()) {
                case "code":
                case "vscode":
                    return "Visual Studio Code";
                case "webstorm":
                    return "WebStorm";
                case "idea":
                case "intellij":
                    return "IntelliJ IDEA";
                case "postman":
                    return "Postman";
                case "libreoffice":
                case "soffice":
                    return "LibreOffice";
                case "nautilus":
                case "nemo":
                case "dolphin":
                    return "File Manager";
                case "gnome-terminal":
                case "konsole":
                case "terminator":
                    return "Terminal";
                default:
                    return capitalizeFirst(processName);
            }
        } catch (Exception e) {
            return processName;
        }
    }

    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    @Override
    public String getActiveProcessName() {
        if (isWayland) {
            return getActiveProcessNameWayland();
        }
        return getActiveProcessNameX11();
    }

    private String getActiveProcessNameX11() {
        try {
            if (hasXdotool) {
                String windowId = executeCommand("xdotool", "getactivewindow");
                if (windowId != null && !windowId.isEmpty() && !windowId.contains("Could not")) {
                    String pid = executeCommand("xdotool", "getwindowpid", windowId.trim());
                    if (pid != null && !pid.isEmpty() && !pid.contains("Failed")) {
                        String process = readFileContent("/proc/" + pid.trim() + "/comm");
                        if (process != null && !process.isEmpty()) {
                            return process.trim();
                        }
                    }
                }
            }

            if (hasXprop) {
                String activeWindow = executeCommand("xprop", "-root", "_NET_ACTIVE_WINDOW");
                if (activeWindow != null && !activeWindow.isEmpty()) {
                    String windowId = activeWindow.substring(activeWindow.lastIndexOf("0x"));
                    String pidHex = executeCommand("xprop", "-id", windowId, "_NET_WM_PID");
                    if (pidHex != null && !pidHex.isEmpty()) {
                        String[] parts = pidHex.split("=");
                        if (parts.length > 1) {
                            String pid = parts[1].trim();
                            String process = readFileContent("/proc/" + pid + "/comm");
                            if (process != null && !process.isEmpty()) {
                                return process.trim();
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Error getting process name: {}", e.getMessage());
        }
        return "";
    }

    private String getActiveProcessNameWayland() {
        try {
            // Get the process with highest CPU usage that's not TrackEye itself
            ProcessBuilder pb = new ProcessBuilder("sh", "-c",
                    "ps -eo pid,comm --sort=-%cpu | grep -v trackeye | grep -v java | head -5 | tail -1 | awk '{print $2}'");
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String processName = reader.readLine();
                if (processName != null && !processName.isEmpty()) {
                    return processName.trim();
                }
            }
            process.waitFor(2, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.debug("Error getting Wayland process: {}", e.getMessage());
        }
        return "";
    }

    @Override
    public long getIdleTimeMillis() {
        try {
            if (!isWayland) {
                String idleStr = executeCommand("xprintidle");
                if (idleStr != null && !idleStr.isEmpty()) {
                    try {
                        return Long.parseLong(idleStr.trim());
                    } catch (NumberFormatException e) {
                        // Fall through
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Error getting idle time: {}", e.getMessage());
        }
        return 0;
    }

    private String readFileContent(String path) {
        try {
            File file = new File(path);
            if (!file.exists() || !file.canRead()) return "";
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                return reader.readLine();
            }
        } catch (IOException e) {
            return "";
        }
    }

    private String executeCommand(String... command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
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

            String result = output.toString().trim();
            if (result.contains("Could not") || result.contains("Failed") ||
                    result.contains("Unable to") || result.contains("No such")) {
                return "";
            }
            return result;
        } catch (Exception e) {
            return "";
        }
    }

    private boolean commandExists(String command) {
        try {
            Process process = new ProcessBuilder("which", command).redirectErrorStream(true).start();
            boolean exited = process.waitFor(1, TimeUnit.SECONDS);
            int exitCode = process.exitValue();
            process.destroy();
            return exited && exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }
}