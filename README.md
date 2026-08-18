# Module 3 (Usage Monitoring) — Session Count → Usage Hours

This package contains **only the files that changed**, in the same folder
structure as your project (`backend/src/main/java/com/gymams/...`,
`frontend/...`). Copy each file over its counterpart in your existing repo.
Modules 1, 2, 4, auth/JWT, DB config, and role permissions are untouched.

## 1. Files to DELETE from your project

These three DTOs backed the old session-count trend/top-used features,
which are replaced by the simpler Today/Month/Most-Used/Least-Used view
you asked for (no graphs, no lifetime stats):

```
backend/src/main/java/com/gymams/dto/UsageSummaryResponse.java
backend/src/main/java/com/gymams/dto/UsageHistoryPointResponse.java
backend/src/main/java/com/gymams/dto/TopUsageResponse.java
```

## 2. Files to REPLACE (included in this package)

**Backend**
```
model/Equipment.java              — + dailyUsageLimitHours / monthlyUsageLimitHours
model/UsageRecord.java            — sessionCount → usageHours (double) + notes
model/UsageStatus.java            — repurposed: NORMAL / NEAR_LIMIT / MAINTENANCE_DUE
dto/EquipmentRequest.java         — + the two usage-limit fields
dto/EquipmentResponse.java        — + the two usage-limit fields
dto/UsageRecordRequest.java       — sessionCount → usageHours, + notes
dto/UsageRecordResponse.java      — sessionCount → usageHours, + notes, dropped per-record status
dto/BatchUsageRequest.java        — sessionCount → usageHours per entry
dto/EquipmentUsageResponse.java   — NEW: one equipment's today/month hours + maintenance status
dto/UsageDashboardResponse.java   — NEW: the whole dashboard payload in one response
repository/UsageRecordRepository.java — trimmed to the two finder methods actually used
service/EquipmentService.java     — passes the two new limit fields through create/update
service/UsageService.java         — rewritten: tableForDate(), dashboard(), upsert(), batchUpsert(), updateById(), delete()
controller/UsageController.java   — new endpoints (see below)
```

**Frontend**
```
usage-data.js            — rewritten data-access layer for the new API
usage-dashboard.js        — rewritten rendering + Usage Entry workflow
equipment-data.js         — passes the two new limit fields through create/update
equipment-dashboard.js    — Add/Edit Equipment modal now reads/writes the two limit fields
admin-dashboard.html      — (a) two new inputs in the Equipment modal, (b) Usage Monitoring section rebuilt (read-only)
gym-manager-dashboard.html — Usage Monitoring section rebuilt (Usage Entry + read-only views)
style.css                 — a few small additions: `.eq-usage-row`, `.eq-usage-view-row`, `.eq-usage-highlight-note`
```

## 3. Database changes

With `spring.jpa.hibernate.ddl-auto=update` (your current setting), Hibernate
will do this automatically on next boot:
- `equipment` table: adds nullable columns `daily_usage_limit_hours`, `monthly_usage_limit_hours` (DOUBLE)
- `usage_record` table: adds column `usage_hours` (DOUBLE), adds column `notes` (VARCHAR 255)

**It will NOT drop the old `session_count` column** — Hibernate `update` mode
never removes columns. It's harmless to leave it, but if you want a clean
schema, drop it manually once you've confirmed everything works:
```sql
ALTER TABLE usage_record DROP COLUMN session_count;
```
Any existing usage rows will have `usage_hours = 0` after migration, since
there's no way to convert a historical session count into hours.

## 4. API endpoints (Module 3)

| Method | Path                  | Role                  | Purpose |
|--------|-----------------------|------------------------|---------|
| GET    | `/api/usage?date=`    | ADMIN, GYM_MANAGER     | Every equipment's reading for one date (0 hrs if not yet logged) — used to prefill Usage Entry |
| GET    | `/api/usage/dashboard?date=` | ADMIN, GYM_MANAGER | Everything the screen needs in one call: today's hours, this month's hours, most/least used (today & month), and each equipment's maintenance status |
| POST   | `/api/usage`           | GYM_MANAGER            | Log/edit one equipment's hours for one date |
| POST   | `/api/usage/batch`     | GYM_MANAGER            | Log every equipment's hours for one date in one call (Usage Entry screen) |
| PUT    | `/api/usage/{id}`      | GYM_MANAGER            | Edit an existing reading's hours |
| DELETE | `/api/usage/{id}`      | ADMIN                  | Delete a reading (kept for API completeness; not wired to any UI button, per "keep it simple") |

Equipment endpoints (`/api/equipment`, ADMIN-only writes) are unchanged
except the request/response body now optionally carries
`dailyUsageLimitHours` / `monthlyUsageLimitHours`.

## 5. How the calculations work

- **Today's usage**: sum of `usage_hours` where `usage_date = selected date`, per equipment (0 if no reading yet).
- **This month's usage**: sum of `usage_hours` for all readings where `usage_date` falls between the 1st and last day of the selected date's month, per equipment. Never stored — recomputed on every request from the daily readings.
- **Most/Least used today**: max/min of today's per-equipment totals.
- **Most/Least used this month**: max/min of this month's per-equipment totals.
- **Maintenance status** (`UsageStatus.fromUsage`): compares today's hours to `dailyUsageLimitHours` and this month's hours to `monthlyUsageLimitHours` independently, then reports the worse of the two:
  - No limit configured → always `NORMAL`
  - `usage >= limit` → `MAINTENANCE_DUE`
  - `usage >= 80% of limit` → `NEAR_LIMIT` ("Maintenance Due Soon")
  - otherwise → `NORMAL`

  This is entirely separate from `EquipmentStatus` (OPERATIONAL / MAINTENANCE_DUE / UNDER_MAINTENANCE / OUT_OF_SERVICE), which still reflects real damage/repair state driven by Module 4. A `MAINTENANCE_DUE` usage status never touches `EquipmentStatus` and never auto-creates a repair request.

## 6. Where limits are configured

The Admin sets `Daily Usage Limit (hours)` and `Monthly Usage Limit (hours)`
on the existing **Add/Edit Equipment** form (Module 1) — both optional,
leave blank for "no limit configured." This was the smallest change that
satisfies "the Admin can configure usage limits from the appropriate
equipment/maintenance configuration UI" without introducing a new page.

## 7. How to test

1. Restart the backend — Hibernate adds the new columns automatically.
2. As Admin: edit an equipment (e.g. Treadmill), set Daily Limit = 1,
   Monthly Limit = 5, save.
3. Log in as Gym Manager → Usage Monitoring → Usage Entry: enter e.g. 1.5
   hours for that equipment today, Save Usage.
4. Confirm: Today's Usage shows 1.5 hrs; Most Used Today highlights it;
   Equipment Usage & Maintenance Status shows `1.5 / 1 hrs` and a
   **Maintenance Due** badge (since 1.5 ≥ 1).
5. Log in as Admin → Usage Monitoring: same data, read-only (no Usage
   Entry section).
6. Change the viewing date to a day with no readings — every equipment
   should show 0 hrs rather than being omitted.
