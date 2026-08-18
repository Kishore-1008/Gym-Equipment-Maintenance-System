/* ============================================================
   Gym Equipment Maintenance System — Usage Monitoring
   usage-data.js
   ------------------------------------------------------------
   Data-access layer for Usage Monitoring. Same shape as
   equipment-data.js: every function returns a Promise and talks
   to the Spring Boot API via apiRequest() (from script.js).

   Usage here is a manually-entered TOTAL USAGE HOURS reading per
   equipment per day — no sessions, no IoT/sensors, no start/end
   timers, no QR tracking.

   Shared by both the Admin and Gym Manager dashboards — role
   differences (who can log usage) are handled in
   usage-dashboard.js, not here.
   ============================================================ */

const USAGE_STATUS_LABELS = {
  NORMAL: "Normal",
  NEAR_LIMIT: "Approaching Limit",
  MAINTENANCE_DUE: "Maintenance Recommended",
};

function usageStatusLabel(code) {
  return USAGE_STATUS_LABELS[code] || code || "Normal";
}

/* ------------------------------------------------------------
   API access
   ------------------------------------------------------------ */

/** GET /api/usage — every equipment's reading for one date (0 hrs if not yet logged). Admin + Gym Manager. */
async function fetchUsageTable(date = "") {
  const qs = date ? `?date=${encodeURIComponent(date)}` : "";
  const list = await apiRequest(`/usage${qs}`, { method: "GET" });
  return list.map(fromApiUsageRecord);
}

/**
 * GET /api/usage/dashboard — everything the Usage Monitoring screen needs
 * for one date: today's hours, this month's hours, most/least used
 * (today & month) and each equipment's usage-based maintenance status.
 * Admin + Gym Manager.
 */
async function fetchUsageDashboard(date = "") {
  const qs = date ? `?date=${encodeURIComponent(date)}` : "";
  return apiRequest(`/usage/dashboard${qs}`, { method: "GET" });
}

/** POST /api/usage — single upsert. Gym Manager only (enforced server-side). */
async function saveUsageRecord({ equipmentId, usageDate, usageHours, notes }) {
  const saved = await apiRequest("/usage", {
    method: "POST",
    body: JSON.stringify({ equipmentId, usageDate, usageHours, notes }),
  });
  return fromApiUsageRecord(saved);
}

/** POST /api/usage/batch — save every machine's hours for one date in one call. Gym Manager only. */
async function saveUsageBatch({ usageDate, entries }) {
  const saved = await apiRequest("/usage/batch", {
    method: "POST",
    body: JSON.stringify({ usageDate, entries }),
  });
  return saved.map(fromApiUsageRecord);
}

/** PUT /api/usage/{id} — edit an existing reading's usage hours. Gym Manager only. */
async function updateUsageRecord(id, { usageHours, notes }) {
  const updated = await apiRequest(`/usage/${encodeURIComponent(id)}`, {
    method: "PUT",
    body: JSON.stringify({ usageHours, notes }),
  });
  return fromApiUsageRecord(updated);
}

/** DELETE /api/usage/{id} — Admin only (enforced server-side). */
async function deleteUsageRecordById(id) {
  await apiRequest(`/usage/${encodeURIComponent(id)}`, { method: "DELETE" });
}

/** Normalizes the API's UsageRecordResponse shape — mostly a pass-through. */
function fromApiUsageRecord(item) {
  return {
    id: item.id,
    equipmentId: item.equipmentId,
    equipmentName: item.equipmentName,
    category: item.category,
    usageDate: item.usageDate,
    usageHours: item.usageHours,
    notes: item.notes,
    recordedBy: item.recordedBy,
    recordedAt: item.recordedAt,
    updatedAt: item.updatedAt,
  };
}
