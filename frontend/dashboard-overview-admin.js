/* ============================================================
   Gym Equipment Maintenance System — Dashboard & Reports
   dashboard-overview-admin.js
   ------------------------------------------------------------
   Module 8 — Admin Dashboard overview. Deliberately has NO
   fetch calls of its own beyond the existing data-layer
   functions already used elsewhere in the app (equipment-data.js,
   repair-data.js, maintenance-data.js, repair-history-data.js,
   warranty-data.js, usage-data.js) — every number here is
   computed client-side from data those modules already expose,
   per "prefer existing APIs -> frontend calculates summaries."
   No new backend code was needed for this file at all.

   This sits ABOVE the existing full management sections further
   down the same page — it's a compact "at a glance" summary, not
   a replacement for Equipment Management / Repair Request
   Management / Maintenance Management / Repair History / Warranty
   Management, all of which are untouched.

   Depends on (all already loaded on admin-dashboard.html):
     - script.js, equipment-data.js, repair-data.js,
       maintenance-data.js, repair-history-data.js,
       warranty-data.js, usage-data.js
   ============================================================ */

function dashOverviewEscapeHtml(str) {
  return String(str == null ? "" : str).replace(/[&<>"']/g, (c) => ({
    "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;",
  }[c]));
}

function dashOverviewCard(label, value, accent) {
  return `
    <div class="eq-card eq-card-${accent}">
      <span class="eq-card-value">${dashOverviewEscapeHtml(value)}</span>
      <span class="eq-card-label">${dashOverviewEscapeHtml(label)}</span>
    </div>`;
}

/* ---------- 1. Equipment Statistics (incl. category breakdown) ---------- */

async function renderAdminEquipmentStats() {
  const el = document.getElementById("dashEquipmentStats");
  if (!el) return;

  try {
    const equipment = await fetchEquipment();
    const total = equipment.length;
    const operational = equipment.filter((e) => e.status === "OPERATIONAL").length;
    const underRepair = equipment.filter((e) => e.status === "UNDER_REPAIR").length;
    const outOfService = equipment.filter((e) => e.status === "OUT_OF_SERVICE").length;
    if (total === 0) {
      el.innerHTML = `<p class="eq-empty-state">No equipment available.</p>`;
      return;
    }

    el.innerHTML = [
      dashOverviewCard("Total Equipment", total, "info"),
      dashOverviewCard("Operational", operational, "ok"),
      dashOverviewCard("Under Repair", underRepair, "danger"),
      dashOverviewCard("Out of Service", outOfService, "danger"),
    ].join("");
  } catch (err) {
    el.innerHTML = `<p class="eq-empty-state">Couldn't load equipment statistics.</p>`;
  }
}

/* ---------- 2. Pending Requests ---------- */

async function renderAdminPendingRequests() {
  const countEl = document.getElementById("dashPendingCount");
  const body = document.getElementById("dashPendingBody");
  const empty = document.getElementById("dashPendingEmpty");
  if (!body) return;

  try {
    const all = await fetchAllRepairRequests();
    const pending = all.filter((r) => r.status === "PENDING");
    if (countEl) countEl.textContent = pending.length;

    const recent = pending.slice(0, 5); // already ordered most-recent-first by the API
    if (recent.length === 0) {
      body.innerHTML = "";
      if (empty) empty.hidden = false;
      return;
    }
    if (empty) empty.hidden = true;

    body.innerHTML = recent.map((r) => `
      <tr>
        <td class="eq-mono">#${r.id}</td>
        <td>${dashOverviewEscapeHtml(r.equipmentName)} <span class="eq-mono eq-muted">${dashOverviewEscapeHtml(r.equipmentId)}</span></td>
        <td>${dashOverviewEscapeHtml(r.submittedByFullName)}</td>
        <td>${repairStatusBadge(r.status)}</td>
        <td>${dashOverviewEscapeHtml(r.submittedDate || "—")}</td>
      </tr>`).join("");
  } catch (err) {
    body.innerHTML = "";
    if (empty) { empty.hidden = false; empty.textContent = "Couldn't load pending requests."; }
  }
}

/* ---------- 3. Maintenance Schedule (upcoming / overdue) ---------- */

async function renderAdminMaintenanceSchedule() {
  const body = document.getElementById("dashMaintenanceBody");
  const empty = document.getElementById("dashMaintenanceEmpty");
  if (!body) return;

  try {
    const all = await fetchAllMaintenance();
    const today = new Date().toISOString().slice(0, 10);
    // Upcoming (or overdue-but-still-open) maintenance, soonest first —
    // SCHEDULED/IN_PROGRESS only; COMPLETED/CANCELLED aren't "upcoming".
    const upcoming = all
      .filter((m) => m.status === "SCHEDULED" || m.status === "IN_PROGRESS")
      .sort((a, b) => (a.maintenanceDate < b.maintenanceDate ? -1 : 1))
      .slice(0, 5);

    if (upcoming.length === 0) {
      body.innerHTML = "";
      if (empty) empty.hidden = false;
      return;
    }
    if (empty) empty.hidden = true;

    body.innerHTML = upcoming.map((m) => {
      const overdue = m.status === "SCHEDULED" && m.maintenanceDate < today;
      return `
      <tr>
        <td>${dashOverviewEscapeHtml(m.equipmentName)} <span class="eq-mono eq-muted">${dashOverviewEscapeHtml(m.equipmentId)}</span></td>
        <td>${dashOverviewEscapeHtml(maintenanceTypeLabel(m.maintenanceType))}</td>
        <td>${dashOverviewEscapeHtml(m.maintenanceDate)}${overdue ? ' <span class="eq-badge eq-badge-danger">Overdue</span>' : ""}</td>
        <td>${dashOverviewEscapeHtml(m.assignedTechnicianName)} — ${dashOverviewEscapeHtml(m.assignedTechnicianCode)}</td>
        <td>${maintStatusBadge(m.status)}</td>
      </tr>`;
    }).join("");
  } catch (err) {
    body.innerHTML = "";
    if (empty) { empty.hidden = false; empty.textContent = "Couldn't load the maintenance schedule."; }
  }
}

/* ---------- 4. Repair Statistics ---------- */

async function renderAdminRepairStats() {
  const el = document.getElementById("dashRepairStats");
  if (!el) return;

  try {
    const [allRequests, history] = await Promise.all([fetchAllRepairRequests(), fetchRepairHistory()]);
    const totalRepairs = allRequests.length;
    const completedRepairs = history.length;
    const totalCost = history.reduce((sum, h) => sum + (Number(h.repairCost) || 0), 0);
    const avgCost = completedRepairs > 0 ? totalCost / completedRepairs : 0;

    if (totalRepairs === 0) {
      el.innerHTML = `<p class="eq-empty-state">No repair history available.</p>`;
      return;
    }

    el.innerHTML = [
      dashOverviewCard("Total Repairs", totalRepairs, "info"),
      dashOverviewCard("Completed Repairs", completedRepairs, "ok"),
      dashOverviewCard("Total Repair Cost", totalCost.toFixed(2), "warn"),
      dashOverviewCard("Average Repair Cost", avgCost.toFixed(2), "warn"),
    ].join("");
  } catch (err) {
    el.innerHTML = `<p class="eq-empty-state">Couldn't load repair statistics.</p>`;
  }
}

/* ---------- 5. Warranty Alerts ---------- */

async function renderAdminWarrantyAlerts() {
  const countsEl = document.getElementById("dashWarrantyCounts");
  const body = document.getElementById("dashWarrantyBody");
  const empty = document.getElementById("dashWarrantyEmpty");
  if (!body) return;

  try {
    const all = await fetchWarranties();
    const active = all.filter((w) => w.status === "ACTIVE").length;
    const expiringSoon = all.filter((w) => w.status === "EXPIRING_SOON");
    const expired = all.filter((w) => w.status === "EXPIRED");

    if (countsEl) {
      countsEl.innerHTML = [
        dashOverviewCard("Active", active, "ok"),
        dashOverviewCard("Expiring Soon", expiringSoon.length, "warn"),
        dashOverviewCard("Expired", expired.length, "danger"),
      ].join("");
    }

    // Highlight the at-risk ones specifically — expiring soon first, then
    // already-expired — rather than repeating the full warranty list.
    const atRisk = [...expiringSoon, ...expired].slice(0, 5);
    if (atRisk.length === 0) {
      body.innerHTML = "";
      if (empty) { empty.hidden = false; empty.textContent = "No warranties expiring soon."; }
      return;
    }
    if (empty) empty.hidden = true;

    body.innerHTML = atRisk.map((w) => `
      <tr>
        <td>${dashOverviewEscapeHtml(w.equipmentName)} <span class="eq-mono eq-muted">${dashOverviewEscapeHtml(w.equipmentId)}</span></td>
        <td>${dashOverviewEscapeHtml(w.provider)}</td>
        <td>${dashOverviewEscapeHtml(w.expiryDate)}</td>
        <td>${dashOverviewEscapeHtml(warrantyDaysRemainingText(w))}</td>
        <td>${warrantyStatusBadge(w.status)}</td>
      </tr>`).join("");
  } catch (err) {
    body.innerHTML = "";
    if (empty) { empty.hidden = false; empty.textContent = "Couldn't load warranty alerts."; }
  }
}

/* ---------- 6. Usage Statistics ---------- */

async function renderAdminUsageStats() {
  const el = document.getElementById("dashUsageStats");
  if (!el) return;

  try {
    const dash = await fetchUsageDashboard();
    const equipment = dash.equipment || [];
    // "Total sessions for the selected/current date": this app logs total
    // daily hours per equipment rather than discrete sessions, so the
    // honest equivalent is how many pieces of equipment have any usage
    // logged for today.
    const loggedToday = equipment.filter((e) => e.todayUsageHours > 0).length;
    const mostUsed = (dash.mostUsedToday && dash.mostUsedToday[0]) || null;
    const highUsageCount = equipment.filter((e) => e.maintenanceStatus === "MAINTENANCE_DUE").length;

    if (equipment.length === 0) {
      el.innerHTML = `<p class="eq-empty-state">No usage data available.</p>`;
      return;
    }

    el.innerHTML = [
      dashOverviewCard("Equipment Logged Today", loggedToday, "info"),
      dashOverviewCard("Most Used Today", mostUsed ? mostUsed.equipmentName : "—", "ok"),
      dashOverviewCard("High-Usage Equipment", highUsageCount, "warn"),
    ].join("");
  } catch (err) {
    el.innerHTML = `<p class="eq-empty-state">Couldn't load usage statistics.</p>`;
  }
}

/* ---------- Init ---------- */

function initDashboardOverviewAdmin(session) {
  if (session.role !== "ADMIN") return;
  renderAdminEquipmentStats();
  renderAdminPendingRequests();
  renderAdminMaintenanceSchedule();
  renderAdminRepairStats();
  renderAdminWarrantyAlerts();
  renderAdminUsageStats();
}