package com.roze.platform;

public interface ActivityMonitor {
    
    /** Get title of currently focused window */
    String getActiveWindowTitle();
    
    /** Get process name of active application */
    String getActiveProcessName();
    
    /** Get idle time in milliseconds since last user input */
    long getIdleTimeMillis();
    
    /** Initialize platform-specific hooks */
    default void init() {}
    
    /** Cleanup resources */
    default void dispose() {}
}