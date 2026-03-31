package com.roze.repository;

import com.roze.config.AppConfig;
import com.roze.model.ActivitySession;
import com.roze.model.AfkSession;
import com.roze.model.ScreenshotRecord;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.sql.*;
import java.util.*;

@Slf4j
@Repository
@RequiredArgsConstructor
public class SessionRepository {

    private final AppConfig config;
    private Connection connection;

    @PostConstruct
    public void init() {
        try {
            File dbDir = new File(config.getStoragePath());
            if (!dbDir.exists()) {
                dbDir.mkdirs();
            }
            
            String dbUrl = "jdbc:sqlite:" + config.getStoragePath() + "/trackeye.db";
            connection = DriverManager.getConnection(dbUrl);
            connection.setAutoCommit(true);
            
            createTables();
            log.info("Database initialized at: {}", dbUrl);
        } catch (SQLException e) {
            log.error("Failed to initialize database", e);
        }
    }

    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // Activity sessions
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS activity_sessions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    app_name TEXT NOT NULL,
                    window_title TEXT,
                    process_name TEXT,
                    start_time INTEGER NOT NULL,
                    end_time INTEGER NOT NULL,
                    duration_ms INTEGER NOT NULL
                )
            """);
            
            // AFK sessions
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS afk_sessions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    start_time INTEGER NOT NULL,
                    end_time INTEGER NOT NULL,
                    duration_ms INTEGER NOT NULL
                )
            """);
            
            // Screenshots
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS screenshots (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    timestamp INTEGER NOT NULL,
                    file_path TEXT NOT NULL,
                    window_title TEXT,
                    process_name TEXT
                )
            """);
            
            // Create indexes
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_activity_start ON activity_sessions(start_time)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_afk_start ON afk_sessions(start_time)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_screenshots_time ON screenshots(timestamp)");
        }
    }

    public void saveActivity(ActivitySession session) {
        String sql = "INSERT INTO activity_sessions (app_name, window_title, process_name, start_time, end_time, duration_ms) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, session.getAppName());
            ps.setString(2, session.getWindowTitle());
            ps.setString(3, session.getProcessName());
            ps.setLong(4, session.getStartTime());
            ps.setLong(5, session.getEndTime());
            ps.setLong(6, session.getDurationMs());
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to save activity", e);
        }
    }

    public void saveAfk(AfkSession session) {
        String sql = "INSERT INTO afk_sessions (start_time, end_time, duration_ms) VALUES (?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, session.getStartTime());
            ps.setLong(2, session.getEndTime());
            ps.setLong(3, session.getDurationMs());
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to save AFK session", e);
        }
    }

    public void saveScreenshot(ScreenshotRecord screenshot) {
        String sql = "INSERT INTO screenshots (timestamp, file_path, window_title, process_name) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, screenshot.getTimestamp());
            ps.setString(2, screenshot.getFilePath());
            ps.setString(3, screenshot.getWindowTitle());
            ps.setString(4, screenshot.getProcessName());
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to save screenshot", e);
        }
    }

    public List<ActivitySession> getActivitySessions(long fromTime, long toTime) {
        List<ActivitySession> sessions = new ArrayList<>();
        String sql = "SELECT * FROM activity_sessions WHERE start_time >= ? AND start_time <= ? ORDER BY start_time DESC";
        
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, fromTime);
            ps.setLong(2, toTime);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                sessions.add(new ActivitySession(
                    rs.getLong("id"),
                    rs.getString("app_name"),
                    rs.getString("window_title"),
                    rs.getString("process_name"),
                    rs.getLong("start_time"),
                    rs.getLong("end_time"),
                    rs.getLong("duration_ms")
                ));
            }
        } catch (SQLException e) {
            log.error("Failed to get activity sessions", e);
        }
        return sessions;
    }

    public List<Map<String, Object>> getTopApps(long fromTime, long toTime, int limit) {
        List<Map<String, Object>> topApps = new ArrayList<>();
        String sql = """
            SELECT app_name, SUM(duration_ms) as total_ms, COUNT(*) as session_count
            FROM activity_sessions
            WHERE start_time >= ? AND start_time <= ?
            GROUP BY app_name
            ORDER BY total_ms DESC
            LIMIT ?
        """;
        
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, fromTime);
            ps.setLong(2, toTime);
            ps.setInt(3, limit);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                Map<String, Object> app = new LinkedHashMap<>();
                app.put("appName", rs.getString("app_name"));
                app.put("totalMs", rs.getLong("total_ms"));
                app.put("sessions", rs.getInt("session_count"));
                topApps.add(app);
            }
        } catch (SQLException e) {
            log.error("Failed to get top apps", e);
        }
        return topApps;
    }

    public long getTotalTime(long fromTime, long toTime) {
        String sql = "SELECT COALESCE(SUM(duration_ms), 0) as total FROM activity_sessions WHERE start_time >= ? AND start_time <= ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, fromTime);
            ps.setLong(2, toTime);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getLong("total");
            }
        } catch (SQLException e) {
            log.error("Failed to get total time", e);
        }
        return 0;
    }

    public List<ScreenshotRecord> getScreenshots(long fromTime, long toTime) {
        List<ScreenshotRecord> screenshots = new ArrayList<>();
        String sql = "SELECT * FROM screenshots WHERE timestamp >= ? AND timestamp <= ? ORDER BY timestamp DESC";
        
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, fromTime);
            ps.setLong(2, toTime);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                screenshots.add(new ScreenshotRecord(
                    rs.getLong("id"),
                    rs.getLong("timestamp"),
                    rs.getString("file_path"),
                    rs.getString("window_title"),
                    rs.getString("process_name")
                ));
            }
        } catch (SQLException e) {
            log.error("Failed to get screenshots", e);
        }
        return screenshots;
    }

    @PreDestroy
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                log.info("Database connection closed");
            }
        } catch (SQLException e) {
            log.error("Failed to close database connection", e);
        }
    }
}