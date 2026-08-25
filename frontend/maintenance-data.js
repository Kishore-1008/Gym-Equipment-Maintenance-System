/* ============================================================
   Gym Equipment Maintenance System — Maintenance Management
   maintenance-data.js
   ------------------------------------------------------------
   Data-access layer for Module 5 (Maintenance Management).
   Entirely Admin + Technician — there is no Gym Manager function
   here at all. Same shape as repair-data.js / equipment-data.js.
   ============================================================ */

/** Canonical maintenance status codes — must match MaintenanceStatus.java exactly. */
const MAINTENANCE_STATUS = {
  SCHEDULED: "SCHEDULED",
  IN_PROGRESS: "IN_PROGRESS",
  COMPLETED: "COMPLETED",
  CANCELLED: "CANCELLED",
};

const MAINTENANCE_STATUS_LABELS = {
  [MAINTENANCE_STATUS.SCHEDULED]: "Scheduled",
  [MAINTENANCE_STATUS.IN_PROGRESS]: "In Progress",
  [MAINTENANCE_STATUS.COMPLETED]: "Completed",
  [MAINTENANCE_STATUS.CANCELLED]: "Cancelled",
};

function maintenanceStatusLabel(code) {
  return MAINTENANCE_STATUS_LABELS[code] || code;
}

/** Canonical maintenance type codes — must match MaintenanceType.java exactly. */
const MAINTENANCE_TYPE = {
  ROUTINE_MAINTENANCE: "ROUTINE_MAINTENANCE",
  PREVENTIVE_MAINTENANCE: "PREVENTIVE_MAINTENANCE",
  INSPECTION: "INSPECTION",
  CLEANING: "CLEANING",
};

const MAINTENANCE_TYPE_LABELS = {
  [MAINTENANCE_TYPE.ROUTINE_MAINTENANCE]: "Routine Maintenance",
  [MAINTENANCE_TYPE.PREVENTIVE_MAINTENANCE]: "Preventive Maintenance",
  [MAINTENANCE_TYPE.INSPECTION]: "Inspection",
  [MAINTENANCE_TYPE.CLEANING]: "Cleaning",
};

function maintenanceTypeLabel(code) {
  return MAINTENANCE_TYPE_LABELS[code] || code;
}

/* ---------- Admin ---------- */

/** POST /api/maintenance — ADMIN only. */
async function scheduleMaintenance({ equipmentId, maintenanceDate, maintenanceType, description, technicianUsername }) {
  return apiRequest("/maintenance", {
    method: "POST",
    body: JSON.stringify({ equipmentId, maintenanceDate, maintenanceType, description, technicianUsername }),
  });
}

/** GET /api/maintenance — ADMIN only. Every maintenance record in the system. */
async function fetchAllMaintenance() {
  return apiRequest("/maintenance", { method: "GET" });
}

/** PUT /api/maintenance/{id}/reschedule — ADMIN only. Not allowed once COMPLETED/CANCELLED. */
async function rescheduleMaintenance(id, maintenanceDate) {
  return apiRequest(`/maintenance/${id}/reschedule`, {
    method: "PUT",
    body: JSON.stringify({ maintenanceDate }),
  });
}

/** PUT /api/maintenance/{id}/cancel — ADMIN only. Not allowed once COMPLETED. */
async function cancelMaintenance(id) {
  return apiRequest(`/maintenance/${id}/cancel`, { method: "PUT" });
}

/* ---------- Technician ---------- */

/** GET /api/maintenance/assigned — TECHNICIAN only. Only this technician's tasks. */
async function fetchAssignedMaintenance() {
  return apiRequest("/maintenance/assigned", { method: "GET" });
}

/** PUT /api/maintenance/{id}/start — TECHNICIAN only. SCHEDULED -> IN_PROGRESS. */
async function startMaintenance(id) {
  return apiRequest(`/maintenance/${id}/start`, { method: "PUT" });
}

/** PUT /api/maintenance/{id}/complete — TECHNICIAN only. IN_PROGRESS -> COMPLETED. */
async function completeMaintenance(id, completionDetails) {
  return apiRequest(`/maintenance/${id}/complete`, {
    method: "PUT",
    body: JSON.stringify({ completionDetails }),
  });
}
