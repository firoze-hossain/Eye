package com.roze.platform;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;

/**
 * Minimal JNA binding to libatspi (AT-SPI2, Linux's accessibility API - the
 * same infrastructure GNOME's Orca screen reader is built on, so it ships
 * with any normal GNOME desktop for accessibility purposes; no packet
 * capture, no root, no setcap).
 *
 * Deliberately narrow: only the synchronous read-only accessor calls needed
 * to walk the tree and read text. No signal/event subscriptions, no GMainLoop
 * integration - those are the parts of AT-SPI's C API most prone to native-
 * interop bugs (threading, ref-counting across callbacks), and none of them
 * are needed just to poll "what does the address bar say right now."
 *
 * Every AtspiAccessible* this returns is a GObject reference the CALLER must
 * release with GLib.INSTANCE.g_object_unref(ptr) once done with it - see
 * AccessibilityUrlMonitorService for the disciplined try/finally pattern used
 * around every call here.
 */
public interface AtspiNative extends Library {
    AtspiNative INSTANCE = loadOrNull();

    static AtspiNative loadOrNull() {
        try {
            return Native.load("atspi", AtspiNative.class);
        } catch (UnsatisfiedLinkError e) {
            return null; // libatspi not present - feature disables itself, see caller
        }
    }

    // int atspi_init(void) - must be called once before anything else.
    int atspi_init();

    // AtspiAccessible* atspi_get_desktop(gint i)
    Pointer atspi_get_desktop(int i);

    // gint atspi_accessible_get_child_count(AtspiAccessible*, GError**)
    int atspi_accessible_get_child_count(Pointer accessible, PointerByReference error);

    // AtspiAccessible* atspi_accessible_get_child_at_index(AtspiAccessible*, gint, GError**)
    Pointer atspi_accessible_get_child_at_index(Pointer accessible, int index, PointerByReference error);

    // gchar* atspi_accessible_get_name(AtspiAccessible*, GError**) - caller must g_free()
    Pointer atspi_accessible_get_name(Pointer accessible, PointerByReference error);

    // gchar* atspi_accessible_get_role_name(AtspiAccessible*, GError**) - caller must g_free()
    Pointer atspi_accessible_get_role_name(Pointer accessible, PointerByReference error);

    // AtspiStateSet* atspi_accessible_get_state_set(AtspiAccessible*)
    Pointer atspi_accessible_get_state_set(Pointer accessible);

    // gboolean atspi_state_set_contains(AtspiStateSet*, AtspiStateType)
    boolean atspi_state_set_contains(Pointer stateSet, int state);

    // AtspiText* atspi_accessible_get_text_iface(AtspiAccessible*) - may return NULL
    Pointer atspi_accessible_get_text_iface(Pointer accessible);

    // gchar* atspi_text_get_text(AtspiText*, gint start, gint end, GError**) - caller must g_free()
    Pointer atspi_text_get_text(Pointer textIface, int start, int end, PointerByReference error);

    // gint atspi_text_get_character_count(AtspiText*, GError**)
    int atspi_text_get_character_count(Pointer textIface, PointerByReference error);

    // ATSPI_STATE_FOCUSED - index into AT-SPI's state enum. Stable across
    // AT-SPI2 versions (part of the public, versioned ABI).
    int ATSPI_STATE_FOCUSED = 26;

    /** Separately-loaded GObject core functions for ref release / string free. */
    class GLib {
        public static final GObjectLib INSTANCE = loadOrNull();

        private static GObjectLib loadOrNull() {
            try {
                return Native.load("gobject-2.0", GObjectLib.class);
            } catch (UnsatisfiedLinkError e) {
                return null;
            }
        }

        public interface GObjectLib extends Library {
            void g_object_unref(Pointer object);
            void g_free(Pointer mem);
        }
    }
}
