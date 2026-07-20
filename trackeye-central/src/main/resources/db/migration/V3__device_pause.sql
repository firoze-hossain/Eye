-- V3__device_pause.sql
-- Lets an admin/supervisor pause a device's syncing without revoking it - the
-- api key stays valid, so resuming needs no new registration token. Distinct
-- from is_active=false (a hard revoke, which also clears the api key).

ALTER TABLE devices ADD COLUMN IF NOT EXISTS paused BOOLEAN NOT NULL DEFAULT FALSE;
