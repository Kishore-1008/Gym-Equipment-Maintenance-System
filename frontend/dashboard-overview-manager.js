/* ============================================================
   Gym Equipment Maintenance System — Dashboard & Reports
   dashboard-overview-manager.js
   ------------------------------------------------------------
   Module 8 — Gym Manager Dashboard overview: Equipment Status
   and a summary of the Manager's OWN requests. No new backend
   endpoints — fetchEquipment() was already callable by any
   authenticated role, and fetchMyRepairRequests() already scopes
   results server-side to the logged-in Manager via the existing
   JWT identity (GET /api/repair-requests/my) — this file never
   touches another Manager's data, it just summarizes the same
   rows the "My Repair Requests" table below already shows.
   Depends on (already loaded on gym-manager-dashboard.html):
     - script.js, equipment-data.js, repair-data.js, repair-dashboard.js
   Self-contained: equipment-dashboard.js (admin-only) is NOT
   loaded on this page, so status label/accent helpers are
   re-declared locally rather than reused from it.
   ============================================================ */

function dashMgrEscapeHtml(str) {
  return String(str == null ? "" : str).replace(/[&<>"']/g, (c) => ({
    "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;",
  }[c]));
}

function dashMgrCard(label, value, accent) {
  return `
    <div class="eq-card eq-card-${accent}">
      <span class="eq-card-value">${dashMgrEscapeHtml(value)}</span>
      <span class="eq-card-label">${dashMgrEscapeHtml(label)}</span>
    </div>`;
}

function dashMgrEquipmentStatusAccent(status) {
  switch (status) {
    case "OPERATIONAL": return "ok";
    case "MAINTENANCE_DUE": return "warn";
    case "UNDER_MAINTENANCE": return "info";
    case "UNDER_REPAIR": return "danger";
    case "OUT_OF_SERVICE": return "danger";
    default: return "info";
  }
}

/* ---------- 1. Equipment Status ---------- */

async function renderManagerEquipmentStatus() {
  const cardsEl = document.getElementById("dashMgrEquipmentCards");
  const body = document.getElementById("dashMgrEquipmentBody");
  const empty = document.getElementById("dashMgrEquipmentEmpty");
  if (!cardsEl) return;

  try {
    const equipment = await fetchEquipment();
    const total = equipment.length;
    const operational = equipment.filter((e) => e.status === "OPERATIONAL").length;
    const underRepair = equipment.filter((e) => e.status === "UNDER_REPAIR").length;
    const outOfService = equipment.filter((e) => e.status === "OUT_OF_SERVICE").length;

    if (total === 0) {
      cardsEl.innerHTML = `<p class="eq-empty-state">No equipment available.</p>`;
      if (body) body.innerHTML = "";
      if (empty) empty.hidden = false;
      return;
    }

    cardsEl.innerHTML = [
      dashMgrCard("Total Equipment", total, "info"),
      dashMgrCard("Operational", operational, "ok"),
      dashMgrCard("Under Repair", underRepair, "danger"),
      dashMgrCard("Out of Service", outOfService, "danger"),
    ].join("");

    if (!body) return;
    // Only equipment needing attention, so the table stays short and useful.
    const attention = equipment.filter((e) => e.status !== "OPERATIONAL");
    if (attention.length === 0) {
      body.innerHTML = "";
      if (empty) { empty.hidden = false; empty.textContent = "All equipment is operational."; }
      return;
    }
    if (empty) empty.hidden = true;
    body.innerHTML = attention.map((e) => `
      <tr>
        <td class="eq-mono">${dashMgrEscapeHtml(e.id)}</td>
        <td>${dashMgrEscapeHtml(e.name)}</td>
        <td>${dashMgrEscapeHtml(e.category)}</td>
        <td><span class="eq-badge eq-badge-${dashMgrEquipmentStatusAccent(e.status)}">${dashMgrEscapeHtml(equipmentStatusLabel(e.status))}</span></td>
      </tr>`).join("");
  } catch (err) {
    cardsEl.innerHTML = `<p class="eq-empty-state">Couldn't load equipment status.</p>`;
  }
}

/* ---------- 2. Their Requests (already scoped server-side to this Manager) ---------- */

async function renderManagerRequestSummary() {
  const el = document.getElementById("dashMgrRequestCounts");
  if (!el) return;

  try {
    const mine = await fetchMyRepairRequests();
    const counts = {
      PENDING: 0, APPROVED: 0, REJECTED: 0, ASSIGNED: 0, IN_PROGRESS: 0, COMPLETED: 0,
    };
    mine.forEach((r) => { if (counts[r.status] !== undefined) counts[r.status]++; });

    if (mine.length === 0) {
      el.innerHTML = `<p class="eq-empty-state">You haven't submitted any repair requests yet.</p>`;
      return;
    }

    el.innerHTML = [
      dashMgrCard("Pending", counts.PENDING, "warn"),
      dashMgrCard("Approved", counts.APPROVED, "info"),
      dashMgrCard("Assigned / In Progress", counts.ASSIGNED + counts.IN_PROGRESS, "info"),
      dashMgrCard("Completed", counts.COMPLETED, "ok"),
      dashMgrCard("Rejected", counts.REJECTED, "danger"),
    ].join("");
  } catch (err) {
    el.innerHTML = `<p class="eq-empty-state">Couldn't load your request summary.</p>`;
  }
}

/* ---------- Init ---------- */

function initDashboardOverviewManager(session) {
  if (session.role !== "GYM_MANAGER") return;
  renderManagerEquipmentStatus();
  renderManagerRequestSummary();
}