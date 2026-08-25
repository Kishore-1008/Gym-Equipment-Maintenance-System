/* ============================================================
   Gym Equipment Maintenance System — Repair Request Management
   repair-data.js
   ------------------------------------------------------------
   Data-access layer for Module 4 (Repair Request Management).
   Every function returns a Promise and talks to the Spring Boot
   REST API (via apiRequest() from script.js) — same shape as
   equipment-data.js / usage-data.js.
   ============================================================ */

/** Canonical repair status codes — must match RepairStatus.java exactly. */
const REPAIR_STATUS = {
  PENDING: "PENDING",
  APPROVED: "APPROVED",
  REJECTED: "REJECTED",
  ASSIGNED: "ASSIGNED",
  IN_PROGRESS: "IN_PROGRESS",
  COMPLETED: "COMPLETED",
};

const REPAIR_STATUS_LABELS = {
  [REPAIR_STATUS.PENDING]: "Pending",
  [REPAIR_STATUS.APPROVED]: "Approved",
  [REPAIR_STATUS.REJECTED]: "Rejected",
  [REPAIR_STATUS.ASSIGNED]: "Assigned",
  [REPAIR_STATUS.IN_PROGRESS]: "In Progress",
  [REPAIR_STATUS.COMPLETED]: "Completed",
};

function repairStatusLabel(code) {
  return REPAIR_STATUS_LABELS[code] || code;
}

/* ---------- Gym Manager ---------- */

/** POST /api/repair-requests — GYM_MANAGER only. */
async function reportProblem({ equipmentId, problemDescription }) {
  return apiRequest("/repair-requests", {
    method: "POST",
    body: JSON.stringify({ equipmentId, problemDescription }),
  });
}

/** GET /api/repair-requests/my — GYM_MANAGER only. The manager's own submitted requests. */
async function fetchMyRepairRequests() {
  return apiRequest("/repair-requests/my", { method: "GET" });
}

/* ---------- Admin ---------- */

/** GET /api/repair-requests — ADMIN only. Every repair request in the system. */
async function fetchAllRepairRequests() {
  return apiRequest("/repair-requests", { method: "GET" });
}

/** PUT /api/repair-requests/{id}/approve — ADMIN only. */
async function approveRepairRequest(id) {
  return apiRequest(`/repair-requests/${id}/approve`, { method: "PUT" });
}

/** PUT /api/repair-requests/{id}/reject — ADMIN only. rejectionReason is mandatory. */
async function rejectRepairRequest(id, rejectionReason) {
  return apiRequest(`/repair-requests/${id}/reject`, {
    method: "PUT",
    body: JSON.stringify({ rejectionReason }),
  });
}

/** PUT /api/repair-requests/{id}/assign — ADMIN only. Only valid once APPROVED. */
async function assignRepairTechnician(id, technicianUsername) {
  return apiRequest(`/repair-requests/${id}/assign`, {
    method: "PUT",
    body: JSON.stringify({ technicianUsername }),
  });
}

/* ---------- Technician ---------- */

/** GET /api/repair-requests/assigned — TECHNICIAN only. Only this technician's work. */
async function fetchAssignedRepairRequests() {
  return apiRequest("/repair-requests/assigned", { method: "GET" });
}

/** PUT /api/repair-requests/{id}/start — TECHNICIAN only. ASSIGNED -> IN_PROGRESS. */
async function startRepairRequest(id) {
  return apiRequest(`/repair-requests/${id}/start`, { method: "PUT" });
}

/**
 * PUT /api/repair-requests/{id}/complete — TECHNICIAN only. IN_PROGRESS -> COMPLETED.
 * Module 6: also creates a permanent Repair History record server-side in
 * the same transaction, so this now takes the full completion form rather
 * than a single completionDetails string.
 */
async function completeRepairRequest(id, { repairDetails, partsUsed, repairCost, completionNotes }) {
  return apiRequest(`/repair-requests/${id}/complete`, {
    method: "PUT",
    body: JSON.stringify({ repairDetails, partsUsed, repairCost, completionNotes }),
  });
}

/* ---------- Shared: technician directory (ADMIN only) ---------- */

/** GET /api/users/technicians — ADMIN only. Powers both Module 4 and Module 5 dropdowns. */
async function fetchTechnicians() {
  return apiRequest("/users/technicians", { method: "GET" });
}