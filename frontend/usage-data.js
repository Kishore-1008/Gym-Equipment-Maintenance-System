/* ============================================================
   Gym Equipment Maintenance System — Usage Monitoring
   usage-data.js
   ------------------------------------------------------------
   Data-access layer for Usage Monitoring. Same shape as
   equipment-data.js: every function returns a Promise and talks
   to the Spring Boot API via apiRequest() (from script.js).

   Usage here is purely a SESSION COUNT per equipment per day —
   no hours, no start/end time, no meter readings.

   Shared by both the Admin and Gym Manager dashboards — role
   differences (who can log/edit/delete) are handled in
   usage-dashboard.js, not here.
   ============================================================ */

const USAGE_STATUS_LABELS = {
  NOT_USED: "Not Used",
  NORMAL: "Normal",
  HIGH: "High",
};

function usageStatusLabel(code) {
  return USAGE_STATUS_LABELS[code] || code;
}

/* ------------------------------------------------------------
   API access
   ------------------------------------------------------------ */

/** GET /api/usage — the Usage Table for one date, optionally filtered by zone. Admin + Gym Manager. */
async function fetchUsageTable({ zone = "", date = "" } = {}) {
  const params = new URLSearchParams();
  if (zone) params.set("zone", zone);
  if (date) params.set("date", date);
  const qs = params.toString();

  const list = await apiRequest(`/usage${qs ? `?${qs}` : ""}`, { method: "GET" });
  return list.map(fromApiUsageRecord);
}

/** GET /api/usage/summary — summary cards for one date. Admin + Gym Manager. */
async function fetchUsageSummary(date = "") {
  const qs = date ? `?date=${encodeURIComponent(date)}` : "";
  return apiRequest(`/usage/summary${qs}`, { method: "GET" });
}

/** GET /api/usage/history — one equipment's usage over time, optionally grouped by week/month. Admin + Gym Manager. */
async function fetchUsageHistory(equipmentId, { groupBy = "day", days = 30 } = {}) {
  const params = new URLSearchParams({ equipmentId, groupBy, days: String(days) });
  return apiRequest(`/usage/history?${params.toString()}`, { method: "GET" });
}

/** GET /api/usage/top — Top Used Equipment ranking. Admin + Gym Manager. */
async function fetchTopUsedEquipment({ days = 30, limit = 5 } = {}) {
  const params = new URLSearchParams({ days: String(days), limit: String(limit) });
  return apiRequest(`/usage/top?${params.toString()}`, { method: "GET" });
}

/** POST /api/usage — single upsert. Gym Manager only (enforced server-side). */
async function saveUsageRecord({ equipmentId, usageDate, sessionCount }) {
  const saved = await apiRequest("/usage", {
    method: "POST",
    body: JSON.stringify({ equipmentId, usageDate, sessionCount }),
  });
  return fromApiUsageRecord(saved);
}

/** POST /api/usage/batch — save an entire zone's sessions for one date in one call. Gym Manager only. */
async function saveUsageBatch({ usageDate, entries }) {
  const saved = await apiRequest("/usage/batch", {
    method: "POST",
    body: JSON.stringify({ usageDate, entries }),
  });
  return saved.map(fromApiUsageRecord);
}

/** PUT /api/usage/{id} — edit an existing record's session count. Gym Manager only. */
async function updateUsageRecord(id, { sessionCount }) {
  const updated = await apiRequest(`/usage/${encodeURIComponent(id)}`, {
    method: "PUT",
    body: JSON.stringify({ sessionCount }),
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
    zone: item.zone,
    usageDate: item.usageDate,
    sessionCount: item.sessionCount,
    status: item.status,
    recordedBy: item.recordedBy,
    recordedAt: item.recordedAt,
    updatedAt: item.updatedAt,
  };
}
