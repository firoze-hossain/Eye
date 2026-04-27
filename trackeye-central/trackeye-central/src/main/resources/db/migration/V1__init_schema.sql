-- src/main/resources/db/migration/V1__init_schema.sql
-- For development: H2 database, for production: PostgreSQL

-- Organizations table
CREATE TABLE IF NOT EXISTS organizations (
                                             id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                             org_name VARCHAR(255) NOT NULL,
    subdomain VARCHAR(100) UNIQUE NOT NULL,
    plan_type VARCHAR(50) DEFAULT 'basic',
    max_users INT DEFAULT 5,
    data_retention_days INT DEFAULT 30,
    settings JSON,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    is_active BOOLEAN DEFAULT TRUE
    );

-- Users table
CREATE TABLE IF NOT EXISTS users (
                                     id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                     organization_id BIGINT NOT NULL,
                                     email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255),
    full_name VARCHAR(255) NOT NULL,
    role VARCHAR(50) DEFAULT 'employee',
    status VARCHAR(50) DEFAULT 'active',
    created_at BIGINT NOT NULL,
    last_login_at BIGINT,
    invited_by BIGINT,
    FOREIGN KEY (organization_id) REFERENCES organizations(id),
    UNIQUE KEY uk_user_email_org (email, organization_id)
    );

-- Devices table
CREATE TABLE IF NOT EXISTS devices (
                                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                       user_id BIGINT NOT NULL,
                                       device_name VARCHAR(255) NOT NULL,
    device_identifier VARCHAR(255) UNIQUE NOT NULL,
    api_key VARCHAR(512) UNIQUE,
    api_key_created_at BIGINT,
    os_type VARCHAR(100),
    last_seen_at BIGINT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at BIGINT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id)
    );

-- Employee activities
CREATE TABLE IF NOT EXISTS employee_activities (
                                                   id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                   device_id BIGINT NOT NULL,
                                                   app_name VARCHAR(255) NOT NULL,
    window_title TEXT,
    process_name VARCHAR(255),
    start_time BIGINT NOT NULL,
    end_time BIGINT NOT NULL,
    duration_ms BIGINT NOT NULL,
    synced_at BIGINT NOT NULL,
    FOREIGN KEY (device_id) REFERENCES devices(id)
    );

-- Employee screenshots
CREATE TABLE IF NOT EXISTS employee_screenshots (
                                                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                    device_id BIGINT NOT NULL,
                                                    screenshot_url VARCHAR(1000) NOT NULL,
    screenshot_hash VARCHAR(255),
    timestamp BIGINT NOT NULL,
    window_title TEXT,
    process_name VARCHAR(255),
    synced_at BIGINT NOT NULL,
    FOREIGN KEY (device_id) REFERENCES devices(id)
    );

-- Browser activities
CREATE TABLE IF NOT EXISTS employee_browser_activities (
                                                           id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                           device_id BIGINT NOT NULL,
                                                           browser_name VARCHAR(100),
    url VARCHAR(2000),
    page_title TEXT,
    start_time BIGINT NOT NULL,
    end_time BIGINT NOT NULL,
    duration_ms BIGINT NOT NULL,
    synced_at BIGINT NOT NULL,
    FOREIGN KEY (device_id) REFERENCES devices(id)
    );

-- AFK sessions
CREATE TABLE IF NOT EXISTS employee_afk_sessions (
                                                     id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                     device_id BIGINT NOT NULL,
                                                     start_time BIGINT NOT NULL,
                                                     end_time BIGINT NOT NULL,
                                                     duration_ms BIGINT NOT NULL,
                                                     synced_at BIGINT NOT NULL,
                                                     FOREIGN KEY (device_id) REFERENCES devices(id)
    );

-- Audit logs
CREATE TABLE IF NOT EXISTS audit_logs (
                                          id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                          user_id BIGINT,
                                          organization_id BIGINT,
                                          action VARCHAR(255) NOT NULL,
    ip_address VARCHAR(45),
    user_agent TEXT,
    details JSON,
    created_at BIGINT NOT NULL
    );
-- Add to your schema.sql or V1__init_schema.sql
-- Audit logs table (add this if missing)

CREATE TABLE IF NOT EXISTS audit_logs (
                                          id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                          user_id BIGINT,
                                          organization_id BIGINT,
                                          action VARCHAR(255) NOT NULL,
    ip_address VARCHAR(45),
    user_agent VARCHAR(1000),
    details JSON,
    created_at BIGINT NOT NULL
    );

-- Create indexes for audit_logs
CREATE INDEX IF NOT EXISTS idx_audit_org_time ON audit_logs(organization_id, created_at);
CREATE INDEX IF NOT EXISTS idx_audit_user_time ON audit_logs(user_id, created_at);
CREATE INDEX IF NOT EXISTS idx_audit_action ON audit_logs(action);

-- Create indexes
CREATE INDEX idx_activities_device_time ON employee_activities(device_id, start_time);
CREATE INDEX idx_screenshots_device_time ON employee_screenshots(device_id, timestamp);
CREATE INDEX idx_browser_device_time ON employee_browser_activities(device_id, start_time);
CREATE INDEX idx_afk_device_time ON employee_afk_sessions(device_id, start_time);
CREATE INDEX idx_devices_user ON devices(user_id);
CREATE INDEX idx_devices_identifier ON devices(device_identifier);
CREATE INDEX idx_devices_api_key ON devices(api_key);
CREATE INDEX idx_users_organization ON users(organization_id);
CREATE INDEX idx_users_email ON users(email);