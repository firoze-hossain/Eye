package com.roze.service;

import com.roze.model.BrowserActivity;
import com.roze.platform.AtspiNative;
import com.roze.repository.SessionRepository;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Reads the browser's address bar text directly via AT-SPI (Linux's
 * accessibility API - the same infrastructure GNOME's own screen reader,
 * Orca, is built on). This is the actual technique real monitoring tools use
 * to get URLs: find the address-bar UI element by its accessible name/role
 * and read its text, rather than reading the browser's history file.
 *
 * Why this matters over BrowserTrackingService's history-file approach:
 * Private/Incognito windows deliberately never write to the history file -
 * that's the whole point of private browsing - but the address bar is still
 * a completely normal, visible UI element in a private window too, so this
 * keeps working there.
 *
 * No packet capture, no elevated privilege, no kernel driver: AT-SPI is
 * accessed as a normal desktop user, the same way any screen reader or UI
 * automation tool works. On most GNOME desktops the required library is
 * already present (accessibility infrastructure ships with the desktop); if
 * it's missing, this detects that on startup, logs one clear message, and
 * disables itself - it never crashes the agent or retries aggressively.
 *
 * Observed URLs are written straight into the SAME local table and SAME sync
 * path as BrowserTrackingService's history-based readings (browser_activities
 * -> /api/sync/browser-activities -> the existing policy engine). No new
 * server-side code, no new rule type - it's just another source feeding the
 * same pipeline.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccessibilityUrlMonitorService {

    private final SessionRepository repository;

    private static final int POLL_INTERVAL_SECONDS = 2;
    private static final int MAX_TRAVERSAL_DEPTH = 6;
    private static final int MAX_CHILDREN_PER_NODE = 40;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private volatile boolean available = false;

    private String lastUrl = "";
    private long lastUrlStartTime = 0;

    @PostConstruct
    public void start() {
        if (!System.getProperty("os.name", "").toLowerCase().contains("linux")) {
            log.debug("AccessibilityUrlMonitorService: AT-SPI support in this build is Linux-only, skipping on this OS.");
            return;
        }
        AtspiNative atspi = AtspiNative.INSTANCE;
        if (atspi == null || AtspiNative.GLib.INSTANCE == null) {
            log.warn("AccessibilityUrlMonitorService: libatspi/libgobject not found - address-bar reading " +
                    "disabled. If you want this feature, install the desktop accessibility stack " +
                    "(e.g. 'sudo apt install at-spi2-core' on Debian/Ubuntu) - a completely standard " +
                    "package, no elevated runtime privilege needed.");
            return;
        }
        try {
            atspi.atspi_init();
            available = true;
            scheduler.scheduleAtFixedRate(this::pollOnce, 5, POLL_INTERVAL_SECONDS, TimeUnit.SECONDS);
            log.info("Accessibility-based URL monitor started (polls every {}s)", POLL_INTERVAL_SECONDS);
        } catch (Throwable t) {
            log.warn("AccessibilityUrlMonitorService failed to initialize AT-SPI - disabling: {}", t.getMessage());
        }
    }

    private void pollOnce() {
        if (!available) return;
        try {
            String url = findFocusedAddressBarText();
            if (url == null || url.isBlank()) return;
            recordIfChanged(url);
        } catch (Throwable t) {
            // Never let a native-interop hiccup take down the scheduler.
            log.debug("Accessibility poll failed: {}", t.getMessage());
        }
    }

    private void recordIfChanged(String url) {
        long now = System.currentTimeMillis();
        if (url.equals(lastUrl)) return;

        if (!lastUrl.isEmpty() && lastUrlStartTime > 0) {
            BrowserActivity activity = new BrowserActivity(
                    "Accessibility", lastUrl, null, lastUrlStartTime, now, now - lastUrlStartTime);
            repository.saveBrowserActivity(activity);
            log.debug("Address-bar URL closed out: {} ({}ms)", lastUrl, now - lastUrlStartTime);
        }
        lastUrl = url;
        lastUrlStartTime = now;
    }

    /**
     * Walks: desktop -> applications -> focused window -> descendants,
     * looking for a text-entry accessible whose name suggests it's the
     * address bar ("Address and search bar" on Chromium-based browsers,
     * "Search or enter address" on Firefox - both stable, documented
     * accessible names). Bounded depth/breadth so a large or unusual UI tree
     * can never cause a runaway traversal.
     */
    private static final String[] KNOWN_BROWSER_APP_NAMES = {
            "firefox", "brave", "chrome", "chromium", "edge", "opera", "vivaldi"
    };

    private String findFocusedAddressBarText() {
        AtspiNative atspi = AtspiNative.INSTANCE;
        Pointer desktop = atspi.atspi_get_desktop(0);
        if (desktop == null) return null;

        try {
            int appCount = safeChildCount(desktop);
            for (int i = 0; i < Math.min(appCount, MAX_CHILDREN_PER_NODE); i++) {
                Pointer app = getChild(desktop, i);
                if (app == null) continue;
                try {
                    // FIX: this used to descend into EVERY app's accessibility
                    // tree looking for an address bar - including apps like
                    // Postman, snap-store, and WhatsApp Desktop that could
                    // never have one. For snap-confined apps, that also
                    // triggered a real AppArmor denial for every single poll
                    // (visible as "dbind-WARNING: AT-SPI: Error in GetItems...
                    // AppArmor policy prevents this sender"). Only descend
                    // into apps that are actually browsers.
                    if (!isLikelyBrowserApp(app)) continue;
                    String found = searchForFocusedAddressBar(app, 0);
                    if (found != null) return found;
                } finally {
                    unref(app);
                }
            }
        } finally {
            unref(desktop);
        }
        return null;
    }

    private boolean isLikelyBrowserApp(Pointer app) {
        String name = getName(app);
        if (name == null) return false;
        String lower = name.toLowerCase();
        for (String browser : KNOWN_BROWSER_APP_NAMES) {
            if (lower.contains(browser)) return true;
        }
        return false;
    }

    private String searchForFocusedAddressBar(Pointer node, int depth) {
        if (depth > MAX_TRAVERSAL_DEPTH) return null;

        String name = getName(node);
        String role = getRoleName(node);

        if (role != null && role.equalsIgnoreCase("entry")
                && name != null && name.toLowerCase().contains("address")) {
            return getTextValue(node);
        }

        // Only descend into subtrees that are actually focused/active, so we
        // don't waste time reading the accessibility tree of every background
        // window on the desktop every 2 seconds.
        if (depth == 0 || isFocusedOrActive(node)) {
            int childCount = safeChildCount(node);
            for (int i = 0; i < Math.min(childCount, MAX_CHILDREN_PER_NODE); i++) {
                Pointer child = getChild(node, i);
                if (child == null) continue;
                try {
                    String found = searchForFocusedAddressBar(child, depth + 1);
                    if (found != null) return found;
                } finally {
                    unref(child);
                }
            }
        }
        return null;
    }

    private boolean isFocusedOrActive(Pointer node) {
        try {
            Pointer stateSet = AtspiNative.INSTANCE.atspi_accessible_get_state_set(node);
            if (stateSet == null) return true; // be permissive rather than miss the real window
            boolean focused = AtspiNative.INSTANCE.atspi_state_set_contains(stateSet, AtspiNative.ATSPI_STATE_FOCUSED);
            unref(stateSet);
            return focused;
        } catch (Throwable t) {
            return true;
        }
    }

    private int safeChildCount(Pointer node) {
        try {
            PointerByReference error = new PointerByReference();
            return AtspiNative.INSTANCE.atspi_accessible_get_child_count(node, error);
        } catch (Throwable t) {
            return 0;
        }
    }

    private Pointer getChild(Pointer node, int index) {
        try {
            PointerByReference error = new PointerByReference();
            return AtspiNative.INSTANCE.atspi_accessible_get_child_at_index(node, index, error);
        } catch (Throwable t) {
            return null;
        }
    }

    private String getName(Pointer node) {
        Pointer namePtr = null;
        try {
            PointerByReference error = new PointerByReference();
            namePtr = AtspiNative.INSTANCE.atspi_accessible_get_name(node, error);
            return namePtr == null ? null : namePtr.getString(0);
        } catch (Throwable t) {
            return null;
        } finally {
            freeGString(namePtr);
        }
    }

    private String getRoleName(Pointer node) {
        Pointer rolePtr = null;
        try {
            PointerByReference error = new PointerByReference();
            rolePtr = AtspiNative.INSTANCE.atspi_accessible_get_role_name(node, error);
            return rolePtr == null ? null : rolePtr.getString(0);
        } catch (Throwable t) {
            return null;
        } finally {
            freeGString(rolePtr);
        }
    }

    private String getTextValue(Pointer node) {
        try {
            Pointer textIface = AtspiNative.INSTANCE.atspi_accessible_get_text_iface(node);
            if (textIface == null) return null;
            try {
                PointerByReference error = new PointerByReference();
                int len = AtspiNative.INSTANCE.atspi_text_get_character_count(textIface, error);
                if (len <= 0) return null;
                PointerByReference error2 = new PointerByReference();
                Pointer textPtr = AtspiNative.INSTANCE.atspi_text_get_text(textIface, 0, len, error2);
                try {
                    return textPtr == null ? null : textPtr.getString(0);
                } finally {
                    freeGString(textPtr);
                }
            } finally {
                unref(textIface);
            }
        } catch (Throwable t) {
            return null;
        }
    }

    private void unref(Pointer p) {
        if (p == null) return;
        try {
            AtspiNative.GLib.INSTANCE.g_object_unref(p);
        } catch (Throwable ignored) {
            // If unref itself fails, there's nothing safe left to do but move on -
            // a leaked ref on an occasional error is far better than crashing.
        }
    }

    private void freeGString(Pointer p) {
        if (p == null) return;
        try {
            AtspiNative.GLib.INSTANCE.g_free(p);
        } catch (Throwable ignored) {
        }
    }

    @PreDestroy
    public void stop() {
        scheduler.shutdownNow();
        log.info("Accessibility URL monitor stopped");
    }
}