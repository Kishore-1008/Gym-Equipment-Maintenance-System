-- ============================================================
-- Gym Equipment Maintenance System
-- reset_repair_request_dev_data.sql
-- ------------------------------------------------------------
-- ⚠️  DEVELOPMENT USE ONLY. THIS SCRIPT DELETES DATA. ⚠️
--
-- Run this ONLY if you want to wipe out old test repair
-- requests and start the id counter over at #1. Do NOT run this
-- against a database that has real repair request history you
-- want to keep — TRUNCATE deletes every row in the table.
--
-- WHY IDs LIKE #5, #6 SHOW UP EVEN THOUGH THEY'RE "THE FIRST"
-- REQUESTS YOU CARE ABOUT
--
-- This is normal, expected MySQL AUTO_INCREMENT behavior, not a
-- bug. Every INSERT — including ones from earlier test rows that
-- were later deleted, or ones that failed partway through while
-- you were debugging the "doesn't have a default value" errors —
-- permanently consumes the next auto-increment value. MySQL never
-- reuses or compacts freed ids, by design (this is true in every
-- production database, not just this project): reusing a freed
-- primary key is what actually causes real bugs (a new row
-- silently inheriting an old row's foreign-key references,
-- audit-log confusion, etc.), so don't try to work around it by
-- renumbering existing rows — that's the "dangerous renumbering"
-- explicitly avoided here.
--
-- Two honest options if you want repair request #1 to be the
-- first real request:
--   (a) Live with the gap — recommended, this is normal.
--   (b) Wipe the table's test data entirely and start clean
--       (this script) — only appropriate in development, before
--       you have real data you'd miss.
--
-- HOW TO USE
--   1. Make sure you actually want to lose every row currently in
--      repair_request. If unsure, back up first:
--        mysqldump -u root -p gym_ams repair_request > repair_request_backup.sql
--   2. Run this script.
--   3. Restart the Spring Boot app (not required, but fine to do).
-- ============================================================

USE gym_ams;

-- See what you're about to delete before you delete it.
SELECT COUNT(*) AS rows_to_be_deleted FROM repair_request;
SELECT * FROM repair_request;

-- TRUNCATE removes every row AND resets AUTO_INCREMENT back to 1
-- in one step (unlike DELETE, which would leave the counter where
-- it was). It also requires no active foreign keys pointing INTO
-- this table from elsewhere — none do in the current schema.
TRUNCATE TABLE repair_request;

-- Confirm: should show 0 rows and AUTO_INCREMENT back at 1.
SELECT COUNT(*) AS remaining_rows FROM repair_request;
SHOW TABLE STATUS LIKE 'repair_request';
