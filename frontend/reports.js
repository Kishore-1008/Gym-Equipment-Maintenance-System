/* ============================================================
   Gym Equipment Maintenance System — Dashboard & Reports
   reports.js
   ------------------------------------------------------------
   Module 8 — Admin Reports page. Same approach as
   dashboard-overview-admin.js: every number here comes from the
   existing data-layer functions (equipment-data.js, repair-data.js,
   maintenance-data.js, repair-history-data.js, warranty-data.js,
   usage-data.js) — no new backend endpoints, no duplicated data.
   ============================================================ */

function reportsEscapeHtml(str) {
  return String(str == null ? "" : str).replace(/[&<>"']/g, (c) => ({
    "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;",
  }[c]));
}

function reportCard(label, value, accent) {
  return `
    <div class="eq-card eq-card-${accent}">
      <span class="eq-card-value">${reportsEscapeHtml(value)}</span>
      <span class="eq-card-label">${reportsEscapeHtml(label)}</span>
    </div>`;
}

function reportEmpty(message) {
  return `<p class="eq-empty-state">${reportsEscapeHtml(message)}</p>`;
}

/** Name + equipment ID, e.g. "Treadmill EQ001" — same pattern as equipmentLabel() in usage-dashboard.js, redeclared here since that file isn't loaded on reports.html. */
function reportEquipmentLabel(name, id) {
  return `${reportsEscapeHtml(name)} <span class="eq-mono eq-muted">${reportsEscapeHtml(id)}</span>`;
}

/** A text-based card (equipment name, not a number) — smaller value size so a name + ID fits cleanly instead of rendering at the same oversized weight as a plain count. */
function reportTextCard(label, valueHtml, accent) {
  return `
    <div class="eq-card eq-card-${accent}">
      <span class="eq-card-value eq-card-value-sm">${valueHtml}</span>
      <span class="eq-card-label">${reportsEscapeHtml(label)}</span>
    </div>`;
}

/* ---------- 1. Equipment Report ---------- */

async function loadEquipmentReport() {
  const el = document.getElementById("reportEquipment");
  if (!el) return;
  try {
    const equipment = await fetchEquipment();
    if (equipment.length === 0) {
      el.innerHTML = reportEmpty("No equipment available.");
      return;
    }
    el.innerHTML = [
      reportCard("Total", equipment.length, "info"),
      reportCard("Operational", equipment.filter((e) => e.status === "OPERATIONAL").length, "ok"),
      reportCard("Under Repair", equipment.filter((e) => e.status === "UNDER_REPAIR").length, "danger"),
      reportCard("Out of Service", equipment.filter((e) => e.status === "OUT_OF_SERVICE").length, "danger"),
    ].join("");
  } catch (err) {
    el.innerHTML = reportEmpty("Couldn't load the equipment report.");
  }
}

/* ---------- 2. Maintenance Report ---------- */

async function loadMaintenanceReport() {
  const el = document.getElementById("reportMaintenance");
  if (!el) return;
  try {
    const all = await fetchAllMaintenance();
    if (all.length === 0) {
      el.innerHTML = reportEmpty("No maintenance records available.");
      return;
    }
    const today = new Date().toISOString().slice(0, 10);
    const upcoming = all.filter((m) => (m.status === "SCHEDULED" || m.status === "IN_PROGRESS") && m.maintenanceDate >= today).length;
    const overdue = all.filter((m) => m.status === "SCHEDULED" && m.maintenanceDate < today).length;
    const completed = all.filter((m) => m.status === "COMPLETED").length;

    el.innerHTML = [
      reportCard("Upcoming", upcoming, "info"),
      reportCard("Completed", completed, "ok"),
      reportCard("Overdue", overdue, "danger"),
    ].join("");
  } catch (err) {
    el.innerHTML = reportEmpty("Couldn't load the maintenance report.");
  }
}

/* ---------- 3. Repair Report ---------- */

async function loadRepairReport() {
  const el = document.getElementById("reportRepairs");
  if (!el) return;
  try {
    const [allRequests, history] = await Promise.all([fetchAllRepairRequests(), fetchRepairHistory()]);
    if (allRequests.length === 0) {
      el.innerHTML = reportEmpty("No repair history available.");
      return;
    }
    const totalCost = history.reduce((sum, h) => sum + (Number(h.repairCost) || 0), 0);

    el.innerHTML = [
      reportCard("Total Repairs", allRequests.length, "info"),
      reportCard("Completed Repairs", history.length, "ok"),
      reportCard("Total Cost", totalCost.toFixed(2), "warn"),
    ].join("");
  } catch (err) {
    el.innerHTML = reportEmpty("Couldn't load the repair report.");
  }
}

/* ---------- 4. Usage Report ---------- */

async function loadUsageReport() {
  const el = document.getElementById("reportUsage");
  if (!el) return;
  try {
    const dash = await fetchUsageDashboard();
    const equipment = dash.equipment || [];
    if (equipment.length === 0) {
      el.innerHTML = reportEmpty("No usage data available.");
      return;
    }
    const mostUsed = (dash.mostUsedToday && dash.mostUsedToday[0]) || null;
    const leastUsed = (dash.leastUsedToday && dash.leastUsedToday[0]) || null;
    const highUsage = equipment.filter((e) => e.maintenanceStatus === "MAINTENANCE_DUE").length;
    const loggedToday = equipment.filter((e) => e.todayUsageHours > 0).length;

    el.innerHTML = [
      reportCard("Equipment Logged Today", loggedToday, "info"),
      reportTextCard("Most Used", mostUsed ? reportEquipmentLabel(mostUsed.equipmentName, mostUsed.equipmentId) : "—", "ok"),
      reportTextCard("Least Used", leastUsed ? reportEquipmentLabel(leastUsed.equipmentName, leastUsed.equipmentId) : "—", "info"),
      reportCard("High Usage", highUsage, "warn"),
    ].join("");
  } catch (err) {
    el.innerHTML = reportEmpty("Couldn't load the usage report.");
  }
}

/* ---------- 5. Warranty Report ---------- */

async function loadWarrantyReport() {
  const el = document.getElementById("reportWarranty");
  if (!el) return;
  try {
    const all = await fetchWarranties();
    if (all.length === 0) {
      el.innerHTML = reportEmpty("No warranties recorded yet.");
      return;
    }
    el.innerHTML = [
      reportCard("Active", all.filter((w) => w.status === "ACTIVE").length, "ok"),
      reportCard("Expiring Soon", all.filter((w) => w.status === "EXPIRING_SOON").length, "warn"),
      reportCard("Expired", all.filter((w) => w.status === "EXPIRED").length, "danger"),
    ].join("");
  } catch (err) {
    el.innerHTML = reportEmpty("Couldn't load the warranty report.");
  }
}

/* ---------- Init ---------- */

function initReports(session) {
  loadEquipmentReport();
  loadMaintenanceReport();
  loadRepairReport();
  loadUsageReport();
  loadWarrantyReport();
}