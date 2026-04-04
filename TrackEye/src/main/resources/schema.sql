-- Activity Sessions Table
CREATE TABLE IF NOT EXISTS activity_sessions (
                                                 id INTEGER PRIMARY KEY AUTOINCREMENT,
                                                 app_name TEXT NOT NULL,
                                                 window_title TEXT,
                                                 process_name TEXT,
                                                 start_time INTEGER NOT NULL,
                                                 end_time INTEGER NOT NULL,
                                                 duration_ms INTEGER NOT NULL
);

-- AFK Sessions Table
CREATE TABLE IF NOT EXISTS afk_sessions (
                                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                                            start_time INTEGER NOT NULL,
                                            end_time INTEGER NOT NULL,
                                            duration_ms INTEGER NOT NULL
);

-- Screenshots Table
CREATE TABLE IF NOT EXISTS screenshots (
                                           id INTEGER PRIMARY KEY AUTOINCREMENT,
                                           timestamp INTEGER NOT NULL,
                                           file_path TEXT NOT NULL,
                                           window_title TEXT,
                                           process_name TEXT
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_activity_start ON activity_sessions(start_time);
CREATE INDEX IF NOT EXISTS idx_afk_start ON afk_sessions(start_time);
CREATE INDEX IF NOT EXISTS idx_screenshots_timestamp ON screenshots(timestamp);

-- Browser Activities Table
CREATE TABLE IF NOT EXISTS browser_activities (
                                                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                                                  browser_name TEXT NOT NULL,
                                                  url TEXT,
                                                  page_title TEXT,
                                                  start_time INTEGER NOT NULL,
                                                  end_time INTEGER NOT NULL,
                                                  duration_ms INTEGER NOT NULL
);

-- Indexes for browser activities
CREATE INDEX IF NOT EXISTS idx_browser_start ON browser_activities(start_time);
CREATE INDEX IF NOT EXISTS idx_browser_url ON browser_activities(url);