/* ============================================================
   Gym Equipment Maintenance System — Warranty Management
   warranty-data.js
   ------------------------------------------------------------
   Data-access layer for Module 7. Admin-only. Every function
   returns a Promise and talks to the Spring Boot REST API via
   apiRequest() from script.js — same shape as equipment-data.js.
   ============================================================ */

/** Canonical warranty type codes — must match the backend's WarrantyType enum exactly. */
const WARRANTY_TYPE = {
  MANUFACTURER: "MANUFACTURER",
  EXTENDED: "EXTENDED",
  DEALER: "DEALER",
  OTHER: "OTHER",
};

const WARRANTY_TYPE_LABELS = {
  [WARRANTY_TYPE.MANUFACTURER]: "Manufacturer",
  [WARRANTY_TYPE.EXTENDED]: "Extended",
  [WARRANTY_TYPE.DEALER]: "Dealer",
  [WARRANTY_TYPE.OTHER]: "Other",
};

function warrantyTypeLabel(code) {
  return WARRANTY_TYPE_LABELS[code] || code;
}

/** Canonical warranty status codes — computed server-side, never stored. Must match WarrantyStatus enum exactly. */
const WARRANTY_STATUS = {
  ACTIVE: "ACTIVE",
  EXPIRING_SOON: "EXPIRING_SOON",
  EXPIRED: "EXPIRED",
};

const WARRANTY_STATUS_LABELS = {
  [WARRANTY_STATUS.ACTIVE]: "Active",
  [WARRANTY_STATUS.EXPIRING_SOON]: "Expiring Soon",
  [WARRANTY_STATUS.EXPIRED]: "Expired",
};

function warrantyStatusLabel(code) {
  return WARRANTY_STATUS_LABELS[code] || code;
}

/** POST /api/warranties — ADMIN only. */
async function createWarranty({ equipmentId, provider, warrantyType, startDate, expiryDate, coverageDetails, contactInformation }) {
  return apiRequest("/warranties", {
    method: "POST",
    body: JSON.stringify({ equipmentId, provider, warrantyType, startDate, expiryDate, coverageDetails, contactInformation }),
  });
}

/** GET /api/warranties — ADMIN only. Full detail included, so View Details needs no extra request. */
async function fetchWarranties() {
  return apiRequest("/warranties", { method: "GET" });
}

/** PUT /api/warranties/{id} — ADMIN only. */
async function updateWarranty(id, { equipmentId, provider, warrantyType, startDate, expiryDate, coverageDetails, contactInformation }) {
  return apiRequest(`/warranties/${id}`, {
    method: "PUT",
    body: JSON.stringify({ equipmentId, provider, warrantyType, startDate, expiryDate, coverageDetails, contactInformation }),
  });
}