/* ============================================================
   Gym Equipment Maintenance System — Repair History
   repair-history-data.js
   ------------------------------------------------------------
   Data-access layer for Module 6 (Repair History). Admin-only
   and strictly read-only — records are created internally by the
   backend the moment a Technician completes a repair; there is no
   create/update/delete call here.
   ============================================================ */

/** GET /api/repair-history — ADMIN only. Full detail included, most recently completed first. */
async function fetchRepairHistory() {
  return apiRequest("/repair-history", { method: "GET" });
}