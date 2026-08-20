/* ============================================================
   Gym Equipment Maintenance System — Equipment Dashboard
   equipment-data.js
   ------------------------------------------------------------
   Data-access layer. Every function returns a Promise and talks
   to the Spring Boot REST API (via apiRequest() from script.js)
   instead of returning mock data — equipment-dashboard.js was
   written against this same function shape, so it didn't need
   to change how it calls fetchEquipment() etc.

   MOCK_EQUIPMENT is intentionally gone: the database starts
   empty, and every row shown here came from MySQL through
   GET /api/equipment.
   ============================================================ */

/**
 * Canonical equipment status codes — must match the backend's
 * EquipmentStatus enum exactly (com.gymams.model.EquipmentStatus).
 */
const EQUIPMENT_STATUS = {
  OPERATIONAL: "OPERATIONAL",
  MAINTENANCE_DUE: "MAINTENANCE_DUE",
  UNDER_MAINTENANCE: "UNDER_MAINTENANCE",
  OUT_OF_SERVICE: "OUT_OF_SERVICE",
};

/** Display labels for status codes — never store or compare against these. */
const EQUIPMENT_STATUS_LABELS = {
  [EQUIPMENT_STATUS.OPERATIONAL]: "Operational",
  [EQUIPMENT_STATUS.MAINTENANCE_DUE]: "Maintenance Due",
  [EQUIPMENT_STATUS.UNDER_MAINTENANCE]: "Under Maintenance",
  [EQUIPMENT_STATUS.OUT_OF_SERVICE]: "Out of Service",
};

function equipmentStatusLabel(code) {
  return EQUIPMENT_STATUS_LABELS[code] || code;
}

/**
 * Canonical maintenance interval codes — must match the backend's
 * MaintenanceInterval enum exactly (com.gymams.model.MaintenanceInterval).
 */
const MAINTENANCE_INTERVAL = {
  ONE_MONTH: "ONE_MONTH",
  THREE_MONTHS: "THREE_MONTHS",
  SIX_MONTHS: "SIX_MONTHS",
  TWELVE_MONTHS: "TWELVE_MONTHS",
};

const MAINTENANCE_INTERVAL_LABELS = {
  [MAINTENANCE_INTERVAL.ONE_MONTH]: "1 Month",
  [MAINTENANCE_INTERVAL.THREE_MONTHS]: "3 Months",
  [MAINTENANCE_INTERVAL.SIX_MONTHS]: "6 Months",
  [MAINTENANCE_INTERVAL.TWELVE_MONTHS]: "12 Months",
};

function maintenanceIntervalLabel(code) {
  return MAINTENANCE_INTERVAL_LABELS[code] || code;
}

/**
 * The only 8 equipment names the Add/Edit form may submit, and their
 * Name -> Category mapping. Used here purely so the Category field
 * can update instantly as the Admin picks a name — the Spring Boot
 * backend re-derives and enforces the same mapping itself
 * (EquipmentCatalog.java), so this copy is never trusted for
 * anything security-relevant.
 */
const EQUIPMENT_NAME_TO_CATEGORY = {
  "Treadmill": "Cardio",
  "Exercise Bike": "Cardio",
  "Bench Press": "Strength",
  "Elliptical Trainer": "Cardio",
  "Rowing Machine": "Cardio",
  "Squat Rack": "Strength",
  "Lat Pulldown Machine": "Strength",
  "Leg Press Machine": "Strength",
};

const EQUIPMENT_NAME_OPTIONS = Object.keys(EQUIPMENT_NAME_TO_CATEGORY);

/* ------------------------------------------------------------
   API access
   ------------------------------------------------------------ */

/** GET /api/equipment — any authenticated user. */
async function fetchEquipment() {
  const list = await apiRequest("/equipment", { method: "GET" });
  return list.map(fromApiEquipment);
}

/** POST /api/equipment — ADMIN only (enforced server-side). */
async function createEquipment({ equipmentName, maintenanceInterval, status, monthlyUsageLimitHours }) {
  const created = await apiRequest("/equipment", {
    method: "POST",
    body: JSON.stringify({ equipmentName, maintenanceInterval, status, monthlyUsageLimitHours }),
  });
  return fromApiEquipment(created);
}

/** PUT /api/equipment/{id} — ADMIN only (enforced server-side). */
async function updateEquipment(equipmentId, { equipmentName, maintenanceInterval, status, monthlyUsageLimitHours }) {
  const updated = await apiRequest(`/equipment/${encodeURIComponent(equipmentId)}`, {
    method: "PUT",
    body: JSON.stringify({ equipmentName, maintenanceInterval, status, monthlyUsageLimitHours }),
  });
  return fromApiEquipment(updated);
}

/** DELETE /api/equipment/{id} — ADMIN only (enforced server-side). */
async function deleteEquipmentById(equipmentId) {
  await apiRequest(`/equipment/${encodeURIComponent(equipmentId)}`, { method: "DELETE" });
}

/** Normalizes the API's { equipmentId, equipmentName, category, maintenanceInterval, status } shape. */
function fromApiEquipment(item) {
  return {
    id: item.equipmentId,
    name: item.equipmentName,
    category: item.category,
    maintenanceInterval: item.maintenanceInterval,
    status: item.status,
    monthlyUsageLimitHours: item.monthlyUsageLimitHours,
  };
}
