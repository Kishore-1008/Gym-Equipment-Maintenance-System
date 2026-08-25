/* ============================================================
   Gym Equipment Maintenance System — Technician Dashboard
   technician-dashboard.js
   ------------------------------------------------------------
   Renders the summary counts at the top of the Technician
   dashboard (Assigned Repairs / Assigned Maintenance / In
   Progress / Completed), combining data already loaded by
   repair-dashboard.js (assignedRepairRequests) and
   maintenance-dashboard.js (assignedMaintenance).

   updateTechnicianSummary() is called from both of those files
   after each of their loads — it's a no-op on pages that don't
   include the summary cards element.
   ============================================================ */

function techEscapeHtml(str) {
  return String(str == null ? "" : str).replace(/[&<>"']/g, (c) => ({
    "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;",
  }[c]));
}

function techSummaryCard(label, value, accent) {
  return `
    <div class="eq-card eq-card-${accent}">
      <span class="eq-card-value">${techEscapeHtml(value)}</span>
      <span class="eq-card-label">${techEscapeHtml(label)}</span>
    </div>`;
}

function updateTechnicianSummary() {
  const container = document.getElementById("techSummaryCards");
  if (!container) return;

  const repairs = typeof assignedRepairRequests !== "undefined" ? assignedRepairRequests : [];
  const maintenance = typeof assignedMaintenance !== "undefined" ? assignedMaintenance : [];

  const inProgressCount =
    repairs.filter((r) => r.status === "IN_PROGRESS").length +
    maintenance.filter((m) => m.status === "IN_PROGRESS").length;

  const completedCount =
    repairs.filter((r) => r.status === "COMPLETED").length +
    maintenance.filter((m) => m.status === "COMPLETED").length;

  container.innerHTML = [
    techSummaryCard("Assigned Repairs", repairs.length, "info"),
    techSummaryCard("Assigned Maintenance", maintenance.length, "info"),
    techSummaryCard("In Progress Tasks", inProgressCount, "warn"),
    techSummaryCard("Completed Tasks", completedCount, "ok"),
  ].join("");
}
