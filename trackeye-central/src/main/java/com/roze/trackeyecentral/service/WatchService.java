package com.roze.trackeyecentral.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Backs the "Watch Live" feature. Deliberately kept in-memory (never touches
 * the database or disk): a live-view frame is throwaway by nature, and this
 * avoids ballooning storage with images nobody asked to keep.
 *
 * How it works (near-live via fast polling, not true streaming):
 *  1. Admin opens the viewer -> POST /api/admin/devices/{id}/watch/start
 *     sets an expiry ~20s in the future for that device.
 *  2. The desktop agent, on its own frequent poll (every ~2s while idle),
 *     asks GET /api/sync/watch-status; if active, it captures the screen and
 *     POSTs the JPEG to /api/sync/watch-frame roughly every 1-1.5s.
 *  3. The admin's browser polls GET /api/admin/devices/{id}/watch/frame every
 *     ~1s and swaps the displayed image.
 *  4. The viewer renews the expiry every ~10s while open; closing it (or
 *     forgetting to) lets the session lapse on its own within ~20s, so a
 *     forgotten tab can't watch an employee indefinitely.
 *
 * This trades a little worst-case latency (~1-2s) for zero new infrastructure
 * (no WebSocket server, no new client dependency). A future phase can replace
 * the transport with a persistent WebSocket for sub-second latency without
 * changing this class's public contract.
 */
@Service
public class WatchService {

    private static final long SESSION_TTL_MS = 20_000;

    private record Session(long expiresAt, byte[] lastFrame, long frameAt) {}

    private final ConcurrentHashMap<Long, Session> sessions = new ConcurrentHashMap<>();
    private final AtomicLong frameCounter = new AtomicLong();

    public void start(Long deviceId) {
        sessions.compute(deviceId, (id, existing) -> new Session(
                System.currentTimeMillis() + SESSION_TTL_MS,
                existing != null ? existing.lastFrame() : null,
                existing != null ? existing.frameAt() : 0));
    }

    /** Called by the viewer periodically so the session doesn't expire mid-watch. */
    public void renew(Long deviceId) {
        sessions.computeIfPresent(deviceId, (id, s) ->
                new Session(System.currentTimeMillis() + SESSION_TTL_MS, s.lastFrame(), s.frameAt()));
    }

    public void stop(Long deviceId) {
        sessions.remove(deviceId);
    }

    public boolean isActive(Long deviceId) {
        Session s = sessions.get(deviceId);
        return s != null && s.expiresAt() > System.currentTimeMillis();
    }

    public void pushFrame(Long deviceId, byte[] jpegBytes) {
        sessions.computeIfPresent(deviceId, (id, s) ->
                new Session(s.expiresAt(), jpegBytes, System.currentTimeMillis()));
        frameCounter.incrementAndGet();
    }

    /** Returns the latest frame, or null if there is no active session or no frame yet. */
    public byte[] latestFrame(Long deviceId) {
        Session s = sessions.get(deviceId);
        if (s == null || s.expiresAt() <= System.currentTimeMillis()) return null;
        return s.lastFrame();
    }

    public long lastFrameAge(Long deviceId) {
        Session s = sessions.get(deviceId);
        if (s == null || s.frameAt() == 0) return -1;
        return System.currentTimeMillis() - s.frameAt();
    }
}
