-- ============================================================
-- Gym Equipment Maintenance System
-- cleanup_repair_request_legacy_columns.sql
-- ------------------------------------------------------------
-- WHY THIS SCRIPT EXISTS
--
-- spring.jpa.hibernate.ddl-auto=update (see application.properties)
-- only ever ADDS tables/columns to match the current entities — it
-- never DROPS or RENAMES a column that used to exist on an earlier
-- version of an entity. If repair_request's Java entity went
-- through a few designs before landing on the current one
-- (RepairRequest.java: equipment_id, problem_description, status,
-- reported_by_username, reported_by_full_name, submitted_at,
-- rejection_reason, reviewed_at, assigned_technician_username,
-- assigned_technician_full_name, assigned_technician_code,
-- assigned_at, started_at, completed_at, completion_details),
-- older columns like created_at, request_date, reported_by_id,
-- reported_by, assigned_technician_id, or technician_id can be
-- left behind in MySQL from a previous run. If any of those
-- leftover columns are NOT NULL with no default, every INSERT
-- through the current entity (which never sets them, because they
-- don't exist in the entity anymore) fails with exactly the errors
-- you saw:
--   Field 'created_at' doesn't have a default value
--   Field 'request_date' doesn't have a default value
--   Field 'reported_by_id' doesn't have a default value
--
-- This script only inspects/drops columns that are NOT part of the
-- current RepairRequest entity. It never touches equipment_id,
-- problem_description, status, submitted_at, or any other column
-- the current entity actually uses.
--
-- HOW TO USE
--   1. Run the SELECT/SHOW statements in Step 1 first and read the
--      output. Only columns that actually show up there are
--      candidates for removal.
--   2. Back up the table before dropping anything:
--        mysqldump -u root -p gym_ams repair_request > repair_request_backup.sql
--   3. Uncomment and run only the DROP statements that match
--      columns you actually found in Step 1. Don't run drops for
--      columns that don't exist — MySQL will just error, but no
--      need to run statements you don't need.
--   4. Restart the Spring Boot app afterwards so Hibernate
--      validates against the now-clean schema.
-- ============================================================

USE gym_ams;

-- ---------- Step 1: inspect what's actually there ----------
-- Read this output before touching anything below.
SHOW CREATE TABLE repair_request;

SELECT COLUMN_NAME, IS_NULLABLE, COLUMN_DEFAULT, DATA_TYPE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'gym_ams' AND TABLE_NAME = 'repair_request'
ORDER BY ORDINAL_POSITION;

-- Foreign keys currently on the table (you'll need the CONSTRAINT_NAME
-- values here if any of the legacy columns below turn out to be FKs —
-- MySQL requires dropping the FK before dropping the column it's on).
SELECT CONSTRAINT_NAME, COLUMN_NAME, REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME
FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = 'gym_ams' AND TABLE_NAME = 'repair_request'
  AND REFERENCED_TABLE_NAME IS NOT NULL;

-- ---------- Step 2: drop legacy foreign keys, if present ----------
-- Only run a line here if Step 1's FK query actually listed that
-- CONSTRAINT_NAME. Substitute the real name from your output —
-- MySQL auto-generates names like "repair_request_ibfk_2", so don't
-- guess; copy it from Step 1.
--
-- ALTER TABLE repair_request DROP FOREIGN KEY <constraint_name_for_reported_by_id>;
-- ALTER TABLE repair_request DROP FOREIGN KEY <constraint_name_for_assigned_technician_id>;
-- ALTER TABLE repair_request DROP FOREIGN KEY <constraint_name_for_technician_id>;

-- ---------- Step 3: drop legacy columns, if present ----------
-- Uncomment only the lines for columns Step 1 actually showed you.
--
-- ALTER TABLE repair_request DROP COLUMN created_at;
-- ALTER TABLE repair_request DROP COLUMN request_date;
-- ALTER TABLE repair_request DROP COLUMN reported_by_id;
-- ALTER TABLE repair_request DROP COLUMN reported_by;
-- ALTER TABLE repair_request DROP COLUMN assigned_technician_id;
-- ALTER TABLE repair_request DROP COLUMN technician_id;

-- ---------- Step 4: re-verify ----------
-- Run this again after the drops — it should now match the current
-- RepairRequest.java entity field-for-field (allowing for the usual
-- Java-camelCase -> sql_snake_case mapping).
SHOW CREATE TABLE repair_request;

-- ============================================================
-- NOTE ON REPAIR REQUEST IDs (#5, #6, ...)
-- ------------------------------------------------------------
-- This is normal, expected MySQL AUTO_INCREMENT behavior, not a
-- bug: every INSERT consumes the next auto-increment value whether
-- or not that row is later deleted or the insert is rolled back by
-- an error (like the ones above). Test rows created and deleted
-- while you were debugging the "doesn't have a default value"
-- errors permanently advanced the counter — MySQL does not reuse
-- or compact freed IDs, by design, so two rows currently existing
-- can legitimately be #5 and #6.
--
-- Do NOT run anything like:
--   ALTER TABLE repair_request AUTO_INCREMENT = 1;
-- on a table that already has real rows — it does not renumber the
-- existing rows, it only changes where the *next* insert starts
-- counting from, and if that next value collides with an existing
-- id you get a primary-key violation. There is no safe, automatic
-- way to "compact" existing IDs without manually reassigning every
-- row's primary key and fixing up every foreign key that points at
-- it — not worth doing, and not something this script attempts.
-- If you want a clean counter going forward, the only real options
-- are either living with the gap (recommended — this is normal in
-- every production database) or truncating the table entirely
-- (TRUNCATE TABLE repair_request; — only if you're fine losing all
-- current repair request data, since it resets AUTO_INCREMENT to 1
-- as a side effect).
-- ============================================================
