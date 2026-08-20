/* ============================================================
   Gym Equipment Maintenance System — Usage Monitoring
   usage-dashboard.js
   ------------------------------------------------------------
   Renders the Usage Monitoring dashboard and drives its
   workflows. One shared module for both dashboards:
     - Admin:       read-only view of today/this month's usage,
                     most/least used, and usage-based maintenance
                     status (limits are configured on the
                     Equipment Management form, not here)
     - Gym Manager: same read-only view, plus the Usage Entry
                     screen (batch-enter every machine's hours
                     for one date on a single screen)

   Deliberately excludes any lifetime-usage statistic, trend
   charts, or session counts — usage here is a manually entered
   TOTAL USAGE HOURS reading per equipment per day.

   initUsageDashboard(session) is called from both
   admin-dashboard.html and gym-manager-dashboard.html; it reads
   session.role to decide whether to wire the Usage Entry screen.
   Depends on:
     - script.js       (session/auth + apiRequest)
     - usage-data.js    (data access for this module)

   Nothing here touches Equipment Management's own state or DOM
   (aside from the two usage-limit fields added to its Add/Edit
   Equipment form, which equipment-dashboard.js owns).
   ============================================================ */

let usageSession = null;
let currentUsageDate = todayIso();

/* ---------- small formatting helpers (self-contained — this file
   is also loaded on pages that don't load equipment-dashboard.js) ---------- */

function escapeHtml(str) {
  return String(str).replace(/[&<>"']/g, (c) => ({
    "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;",
  }[c]));
}

function todayIso() {
  return new Date().toISOString().slice(0, 10);
}

function formatHours(value) {
  if (value === null || value === undefined) return "—";
  const n = Number(value);
  return (Number.isInteger(n) ? n : n.toFixed(1)) + " hrs";
}

function usageStatusAccent(status) {
  switch (status) {
    case "MAINTENANCE_DUE": return "danger";
    case "NEAR_LIMIT": return "warn";
    default: return "ok";
  }
}

function equipmentLabel(name, id) {
  return `${escapeHtml(name)} <span class="eq-mono eq-muted">${escapeHtml(id)}</span>`;
}

/* ---------- toast (own element, present on both dashboards) ---------- */

function showUsageToast(message) {
  const el = document.getElementById("usageToast");
  if (!el) return;
  el.textContent = message;
  el.classList.add("eq-toast-visible");
  window.clearTimeout(showUsageToast._t);
  showUsageToast._t = window.setTimeout(() => el.classList.remove("eq-toast-visible"), 3200);
}

function showAlert(container, message, type = "danger") {
  if (!container) return;
  container.innerHTML = `<div class="alert alert-tag alert-tag-${type}" role="alert">${escapeHtml(message)}</div>`;
}

/* ============================================================
   Today's Usage / This Month's Usage lists
   ============================================================ */

function renderUsageHoursList(elementId, rows, hoursKey, emptyMessage) {
  const el = document.getElementById(elementId);
  if (!el) return;

  if (rows.length === 0) {
    el.innerHTML = `<p class="eq-empty-state">${escapeHtml(emptyMessage)}</p>`;
    return;
  }

  el.innerHTML = rows.map((r) => `
    <div class="eq-usage-row">
      <span class="eq-health-label">${equipmentLabel(r.equipmentName, r.equipmentId)}</span>
      <span class="eq-health-count">${formatHours(r[hoursKey])}</span>
    </div>
  `).join("");
}

/* ============================================================
   Most/Least Used highlight cards — each can hold MULTIPLE
   equipment when several are tied for the highest (or lowest)
   usage; every tied equipment is shown, never just the first one.
   ============================================================ */

/** elementId is the .eq-card container itself — this owns both its class and its content. */
function renderHighlightCard(elementId, entries, accent) {
  const el = document.getElementById(elementId);
  if (!el) return;

  el.classList.remove("eq-card-ok", "eq-card-warn", "eq-card-danger", "eq-card-info");
  el.classList.add(`eq-card-${accent}`);

  if (!entries || entries.length === 0) {
    el.innerHTML = `<span class="eq-card-value eq-card-value-sm">—</span><span class="eq-card-label">No usage logged yet</span>`;
    return;
  }

  el.innerHTML = entries.map((entry) => `
    <div class="eq-highlight-entry">
      <span class="eq-card-value eq-card-value-sm">${equipmentLabel(entry.equipmentName, entry.equipmentId)}</span>
      <span class="eq-card-label">${formatHours(entry.hours)}</span>
    </div>
  `).join("");
}

/* ============================================================
   Equipment Usage / Maintenance Status table
   ============================================================ */

function renderMaintenanceStatusTable(rows) {
  const el = document.getElementById("usageStatusBody");
  const empty = document.getElementById("usageStatusEmpty");
  if (!el) return;

  if (rows.length === 0) {
    el.innerHTML = "";
    if (empty) empty.hidden = false;
    return;
  }
  if (empty) empty.hidden = true;

  el.innerHTML = rows.map((r) => {
    const accent = usageStatusAccent(r.maintenanceStatus);
    const limitLabel = r.monthlyUsageLimitHours != null ? formatHours(r.monthlyUsageLimitHours) : "No limit set";

    return `
      <tr>
        <td>${equipmentLabel(r.equipmentName, r.equipmentId)}</td>
        <td>${formatHours(r.todayUsageHours)}</td>
        <td>${formatHours(r.monthUsageHours)}</td>
        <td>${limitLabel}</td>
        <td><span class="eq-badge eq-badge-${accent}">${escapeHtml(usageStatusLabel(r.maintenanceStatus))}</span></td>
      </tr>
    `;
  }).join("");
}

/* ============================================================
   Load + render the whole dashboard for the selected date
   ============================================================ */

async function refreshUsageDashboard() {
  try {
    const dashboard = await fetchUsageDashboard(currentUsageDate);
    const rows = dashboard.equipment || [];

    renderUsageHoursList("usageTodayList", rows, "todayUsageHours", "No usage logged for today yet.");
    renderUsageHoursList("usageMonthList", rows, "monthUsageHours", "No usage logged this month yet.");

    renderHighlightCard("usageMostToday", dashboard.mostUsedToday, "ok");
    renderHighlightCard("usageLeastToday", dashboard.leastUsedToday, "warn");
    renderHighlightCard("usageMostMonth", dashboard.mostUsedMonth, "ok");
    renderHighlightCard("usageLeastMonth", dashboard.leastUsedMonth, "warn");

    renderMaintenanceStatusTable(rows);

    const monthLabelEl = document.getElementById("usageMonthLabel");
    if (monthLabelEl) monthLabelEl.textContent = dashboard.month || "";
  } catch (err) {
    showUsageToast(err.message || "Couldn't load usage data.");
  }
}

function wireDateControl() {
  const dateInput = document.getElementById("usageViewDate");
  if (!dateInput) return;
  if (!dateInput.value) dateInput.value = currentUsageDate;
  dateInput.addEventListener("change", () => {
    currentUsageDate = dateInput.value || todayIso();
    refreshUsageDashboard();
  });
}

/* ============================================================
   Usage Entry (Gym Manager only) — batch-enter every machine's
   hours for one date on a single screen.
   ============================================================ */

function renderUsageEntryRows(rows) {
  const tbody = document.getElementById("usageEntryBody");
  if (!tbody) return;

  tbody.innerHTML = rows.map((r) => `
    <tr>
      <td>${equipmentLabel(r.equipmentName, r.equipmentId)}</td>
      <td>
        <input type="number" min="0" step="0.1" class="form-control eq-batch-input"
               data-usage-equipment="${escapeHtml(r.equipmentId)}"
               value="${r.usageHours ?? 0}"
               aria-label="Usage hours for ${escapeHtml(r.equipmentName)}" />
      </td>
    </tr>
  `).join("");
}

async function loadUsageEntryForDate() {
  const date = document.getElementById("usageEntryDate")?.value || todayIso();
  try {
    const rows = await fetchUsageTable(date);
    renderUsageEntryRows(rows);
  } catch (err) {
    showUsageToast(err.message || "Couldn't load equipment for usage entry.");
  }
}

function wireUsageEntry() {
  const dateInput = document.getElementById("usageEntryDate");
  const saveBtn = document.getElementById("usageEntrySaveBtn");
  const alertBox = document.getElementById("usageEntryAlert");
  if (!dateInput || !saveBtn) return;

  if (!dateInput.value) dateInput.value = todayIso();

  dateInput.addEventListener("change", loadUsageEntryForDate);

  saveBtn.addEventListener("click", async () => {
    if (alertBox) alertBox.innerHTML = "";
    const usageDate = dateInput.value || todayIso();

    const inputs = document.querySelectorAll("[data-usage-equipment]");
    const entries = Array.from(inputs).map((input) => ({
      equipmentId: input.getAttribute("data-usage-equipment"),
      usageHours: Number(input.value) || 0,
    }));

    if (entries.length === 0) {
      showAlert(alertBox, "No equipment to log usage for.");
      return;
    }
    if (entries.some((e) => e.usageHours < 0)) {
      showAlert(alertBox, "Usage hours can't be negative.");
      return;
    }

    saveBtn.disabled = true;
    const originalLabel = saveBtn.textContent;
    saveBtn.textContent = "Saving…";

    try {
      await saveUsageBatch({ usageDate, entries });
      showUsageToast(`Saved usage hours for ${entries.length} machine(s) on ${usageDate}.`);
      currentUsageDate = usageDate;
      const viewDate = document.getElementById("usageViewDate");
      if (viewDate) viewDate.value = usageDate;
      await refreshUsageDashboard();
    } catch (err) {
      showAlert(alertBox, err.message || "Couldn't save these usage entries.");
    } finally {
      saveBtn.disabled = false;
      saveBtn.textContent = originalLabel;
    }
  });
}

/* ============================================================
   Entry point — called from admin-dashboard.html and
   gym-manager-dashboard.html
   ============================================================ */

async function initUsageDashboard(session) {
  usageSession = session;
  currentUsageDate = todayIso();

  wireDateControl();

  if (session.role === ROLES.GYM_MANAGER) {
    wireUsageEntry();
    await loadUsageEntryForDate();
  }

  await refreshUsageDashboard();
}

window.initUsageDashboard = initUsageDashboard;
