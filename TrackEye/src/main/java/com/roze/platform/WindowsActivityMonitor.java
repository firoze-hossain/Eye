package com.roze.platform;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.*;
import com.sun.jna.ptr.IntByReference;

public class WindowsActivityMonitor implements ActivityMonitor {

    @Override
    public String getActiveWindowTitle() {
        try {
            WinDef.HWND hwnd = User32.INSTANCE.GetForegroundWindow();
            if (hwnd == null) return "";
            char[] buffer = new char[512];
            User32.INSTANCE.GetWindowText(hwnd, buffer, 512);
            return Native.toString(buffer).trim();
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public String getActiveProcessName() {
        try {
            WinDef.HWND hwnd = User32.INSTANCE.GetForegroundWindow();
            if (hwnd == null) return "";

            IntByReference pid = new IntByReference();
            User32.INSTANCE.GetWindowThreadProcessId(hwnd, pid);

            WinNT.HANDLE hProcess = Kernel32.INSTANCE.OpenProcess(
                    WinNT.PROCESS_QUERY_INFORMATION | WinNT.PROCESS_VM_READ,
                    false, pid.getValue());

            if (hProcess == null) return "";

            char[] buffer = new char[512];
            Psapi.INSTANCE.GetModuleFileNameExW(hProcess, null, buffer, 512);
            Kernel32.INSTANCE.CloseHandle(hProcess);

            String fullPath = Native.toString(buffer).trim();
            if (fullPath.isEmpty()) return "";
            return fullPath.substring(fullPath.lastIndexOf('\\') + 1)
                    .replace(".exe", "");
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public long getIdleTimeMillis() {
        try {
            WinUser.LASTINPUTINFO info = new WinUser.LASTINPUTINFO();
            User32.INSTANCE.GetLastInputInfo(info);
            return Kernel32.INSTANCE.GetTickCount() - info.dwTime;
        } catch (Exception e) {
            return 0;
        }
    }
}