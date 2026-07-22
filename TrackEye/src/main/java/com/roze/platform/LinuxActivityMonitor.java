package com.roze.platform;

import lombok.extern.slf4j.Slf4j;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class LinuxActivityMonitor implements ActivityMonitor {

    private boolean hasXdotool = false;
    private boolean hasXprop = false;
    private boolean hasWmctrl = false;
    private boolean isWayland = false;

    // Resolves a raw process/executable name (e.g. "brave", "code") to the
    // proper, human-readable app name from its .desktop file - the same
    // mechanism GNOME's own Activities view uses, so it works identically
    // whether the app came from apt, snap, flatpak, or a tar.gz you unpacked
    // yourself, since all of those register a .desktop entry somewhere.
    private final Map<String, String> desktopNameByKey = new ConcurrentHashMap<>();
    private volatile long desktopCacheBuiltAt = 0;
    private static final long DESKTOP_CACHE_TTL_MS = 5 * 60_000; // rescan periodically - catches newly installed apps
    private static final String[] DESKTOP_DIRS = {
            "/usr/share/applications",
            "/usr/local/share/applications",
            "/var/lib/snapd/desktop/applications",
            "/var/lib/flatpak/exports/share/applications",
            System.getProperty("user.home") + "/.local/share/applications",
            System.getProperty("user.home") + "/.local/share/flatpak/exports/share/applications"
    };

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

    /**
     * The OLD detection method - "whichever process is using the most CPU
     * right now" - kept only as an absolute last resort if GNOME's DBus
     * interface is unavailable (e.g. a non-GNOME Wayland compositor). This is
     * NOT focus-based and will misattribute activity; it exists only so the
     * agent still returns *something* rather than nothing on unsupported
     * compositors.
     */
    private String getActiveProcessNameWaylandLegacyFallback() {
        try {
            ProcessBuilder pb = new ProcessBuilder("sh", "-c",
                    "ps -eo pid,comm --sort=-%cpu | grep -v trackeye | grep -v java | head -5 | tail -1 | awk '{print $2}'");
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String processName = reader.readLine();
                if (processName != null && !processName.isEmpty()) {
                    return prettyAppName(processName.trim());
                }
            }
            process.waitFor(2, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.debug("Error getting Wayland process (legacy fallback): {}", e.getMessage());
        }
        return "";
    }

    private String getActiveProcessNameX11() {
        try {
            // FIX: was reading /proc/PID/comm directly (15-char kernel-imposed
            // truncation, e.g. "Unattended-upgr") and returning the raw name
            // as-is. Now reads the full, untruncated name and resolves it to
            // the proper app name via .desktop files, same as the Wayland path.
            if (hasXdotool) {
                String windowId = executeCommand("xdotool", "getactivewindow");
                if (windowId != null && !windowId.isEmpty() && !windowId.contains("Could not")) {
                    String pid = executeCommand("xdotool", "getwindowpid", windowId.trim());
                    if (pid != null && !pid.isEmpty() && !pid.contains("Failed")) {
                        String process = getFullProcessName(pid.trim());
                        if (process != null && !process.isEmpty()) {
                            return prettyAppName(process);
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

                            String process = getFullProcessName(pid);
                            if (process != null && !process.isEmpty()) {
                                return prettyAppName(process);
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
        // FIX: this used to run `ps --sort=-%cpu | head -5 | tail -1` - i.e.
        // "whichever process happens to be the 5th-highest CPU user right
        // now" - which has NOTHING to do with which window is actually
        // focused. That's exactly why Xwayland (busy compositing any X11 app,
        // including Brave) and background daemons (snapd, tracker-miner,
        // unattended-upgrades - anything that happened to spike CPU briefly)
        // were showing up as "active applications." Query the real focused
        // window's PID via the same GNOME Shell DBus interface already used
        // for the window title, then resolve that PID to its actual name.
        try {
            String pid = getFocusedWindowPidViaDBus();
            if (pid != null && !pid.isEmpty()) {
                String rawName = getFullProcessName(pid.trim());
                if (rawName != null && !rawName.isEmpty()) {
                    return prettyAppName(rawName);
                }
            }
        } catch (Exception e) {
            log.debug("Error getting Wayland process via DBus: {}", e.getMessage());
        }
        // Last resort only, if GNOME's DBus interface is ever unavailable
        // (e.g. a non-GNOME Wayland compositor) - far from ideal, but still
        // better than returning nothing.
        return getActiveProcessNameWaylandLegacyFallback();
    }

    private String getFocusedWindowPidViaDBus() {
        try {
            ProcessBuilder pb = new ProcessBuilder("gdbus", "call",
                    "--session", "--dest", "org.gnome.Shell",
                    "--object-path", "/org/gnome/Shell",
                    "--method", "org.gnome.Shell.Eval",
                    "global.get_window_actors().find(a => a.meta_window.has_focus())?.meta_window.get_pid()");
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine();
                if (line != null) {
                    // gdbus Eval returns something like (true, '1234')
                    Matcher m = Pattern.compile("'(\\d+)'").matcher(line);
                    if (m.find()) {
                        return m.group(1);
                    }
                }
            }
            process.waitFor(2, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.trace("DBus PID query failed: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Reads the FULL, untruncated process name for a PID. /proc/PID/comm (and
     * `ps comm`) are hard-capped at 15 characters by the kernel itself -
     * that's exactly why names like "Unattended-upgr" and "Tracker-miner-f"
     * were showing up truncated. /proc/PID/cmdline has the complete argv,
     * never truncated; /proc/PID/exe (the real binary path) is the fallback.
     */
    private String getFullProcessName(String pid) {
        try {
            byte[] raw = Files.readAllBytes(Paths.get("/proc/" + pid + "/cmdline"));
            String cmdline = new String(raw, StandardCharsets.UTF_8).split("\u0000")[0];
            if (!cmdline.isEmpty()) {
                return new File(cmdline).getName();
            }
        } catch (Exception ignored) {
        }
        try {
            Path target = Files.readSymbolicLink(Paths.get("/proc/" + pid + "/exe"));
            return target.getFileName().toString();
        } catch (Exception ignored) {
        }
        String truncated = readFileContent("/proc/" + pid + "/comm");
        return truncated == null ? "" : truncated.trim();
    }

    /**
     * Resolves a raw executable name to its proper, human-facing app name via
     * the freedesktop.org .desktop file standard - works the same regardless
     * of how the app was installed (apt/deb, snap, flatpak, AppImage, or a
     * tar.gz extracted by hand and launched directly), since all of those
     * register a .desktop entry somewhere in the standard search paths.
     */
    private String prettyAppName(String rawProcessName) {
        if (rawProcessName == null || rawProcessName.isEmpty()) return rawProcessName;
        ensureDesktopCacheFresh();
        String match = desktopNameByKey.get(rawProcessName.toLowerCase());
        return match != null ? match : capitalizeFirst(rawProcessName);
    }

    private void ensureDesktopCacheFresh() {
        long now = System.currentTimeMillis();
        if (!desktopNameByKey.isEmpty() && now - desktopCacheBuiltAt < DESKTOP_CACHE_TTL_MS) return;

        Map<String, String> fresh = new HashMap<>();
        for (String dirPath : DESKTOP_DIRS) {
            File dir = new File(dirPath);
            File[] files = dir.listFiles((d, name) -> name.endsWith(".desktop"));
            if (files == null) continue;
            for (File f : files) {
                parseDesktopFile(f, fresh);
            }
        }
        desktopNameByKey.putAll(fresh);
        desktopCacheBuiltAt = now;
        log.debug("Desktop app name cache built: {} entries", desktopNameByKey.size());
    }

    private void parseDesktopFile(File f, Map<String, String> out) {
        try {
            String name = null, execBinary = null, wmClass = null;
            boolean inDesktopEntrySection = false;
            for (String line : Files.readAllLines(f.toPath(), StandardCharsets.UTF_8)) {
                if (line.startsWith("[")) {
                    inDesktopEntrySection = line.equals("[Desktop Entry]");
                    if (!inDesktopEntrySection && name != null) break; // left the section we care about
                    continue;
                }
                if (!inDesktopEntrySection) continue;

                if (name == null && line.startsWith("Name=")) {
                    name = line.substring(5).trim();
                } else if (execBinary == null && line.startsWith("Exec=")) {
                    String exec = line.substring(5).trim();
                    String firstToken = exec.split("\\s+")[0];
                    execBinary = new File(firstToken).getName();
                } else if (wmClass == null && line.startsWith("StartupWMClass=")) {
                    wmClass = line.substring("StartupWMClass=".length()).trim();
                }
            }
            if (name != null) {
                if (execBinary != null) out.putIfAbsent(execBinary.toLowerCase(), name);
                if (wmClass != null) out.putIfAbsent(wmClass.toLowerCase(), name);
                String baseFileName = f.getName().replaceFirst("\\.desktop$", "");
                out.putIfAbsent(baseFileName.toLowerCase(), name);
            }
        } catch (Exception e) {
            log.trace("Could not parse .desktop file {}: {}", f.getName(), e.getMessage());
        }
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