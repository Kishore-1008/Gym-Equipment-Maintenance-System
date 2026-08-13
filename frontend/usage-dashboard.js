/* ============================================================
   Gym Equipment Maintenance System — Usage Monitoring
   usage-dashboard.js
   ------------------------------------------------------------
   Renders the Usage Monitoring dashboard and drives its
   workflows. One shared module for both dashboards:
     - Admin:       view summary / table / history / top-used, delete
     - Gym Manager: same view, plus the Batch Usage Logging screen
                    (the "staff" entry point — no clicking into
                    separate equipment pages)

   initUsageDashboard(session) is called from both
   admin-dashboard.html and gym-manager-dashboard.html; it reads
   session.role to decide whether to wire the batch-entry screen.
   Depends on:
     - script.js          (session/auth + apiRequest)
     - equipment-data.js  (fetchEquipment — populates zone/equipment
                            dropdowns and the batch-entry list)
     - usage-data.js       (data access for this module)

   Nothing here touches Equipment Management's own state or DOM.
   ============================================================ */

let usageSession = null;
let usageEquipmentList = [];
let usageZones = [];
let currentUsageDate = todayIso();
let currentUsageHistoryEquipmentId = null;

/* ---------- small formatting helpers (self-contained — this file
   is also loaded on pages that don't load equipment-dashboard.js) ---------- */

function escapeHtml(str) {
  return String(str).replace(/[&<>"']/g, (c) => ({
    "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;",
  }[c]));
}

function formatUsageDate(isoDate) {
  if (!isoDate) return "—";
  const d = new Date(`${isoDate}T00:00:00`);
  return d.toLocaleDateString("en-GB", { day: "2-digit", month: "short", year: "numeric" });
}

function todayIso() {
  return new Date().toISOString().slice(0, 10);
}

function usageStatusAccent(status) {
  switch (status) {
    case "HIGH": return "danger";
    case "NORMAL": return "ok";
    case "NOT_USED": return "warn";
    default: return "info";
  }
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
   A. Summary cards
   ============================================================ */

function renderUsageSummaryCards(summary) {
  const el = document.getElementById("usageSummaryCards");
  if (!el) return;

  const cards = [
    { label: "Total Sessions", value: summary.totalSessions, accent: "info" },
    {
      label: "Most Used Equipment",
      value: summary.mostUsedEquipmentName ? `${summary.mostUsedEquipmentName} (${summary.mostUsedSessions})` : "—",
      accent: "ok", small: true,
    },
    {
      label: "Least Used Equipment",
      value: summary.leastUsedEquipmentName ? `${summary.leastUsedEquipmentName} (${summary.leastUsedSessions})` : "—",
      accent: "warn", small: true,
    },
    { label: "Total Equipment", value: summary.totalEquipment, accent: "danger" },
  ];

  el.innerHTML = cards.map((c) => `
    <div class="eq-card eq-card-${c.accent}">
      <span class="eq-card-value${c.small ? " eq-card-value-sm" : ""}">${escapeHtml(String(c.value))}</span>
      <span class="eq-card-label">${c.label}</span>
    </div>
  `).join("");
}

/* ============================================================
   B. Usage Table
   ============================================================ */

function renderUsageTable(list) {
  const el = document.getElementById("usageTableBody");
  const empty = document.getElementById("usageTableEmpty");
  if (!el) return;

  if (list.length === 0) {
    el.innerHTML = "";
    if (empty) empty.hidden = false;
    return;
  }
  if (empty) empty.hidden = true;

  const isAdmin = usageSession.role === ROLES.ADMIN;

  el.innerHTML = list.map((r) => {
    const accent = usageStatusAccent(r.status);
    const canDelete = isAdmin && r.id != null;
    const actions = canDelete
      ? `<button type="button" class="eq-row-btn eq-row-btn-danger" data-usage-delete="${r.id}">Delete</button>`
      : "—";

    return `
      <tr>
        <td>${escapeHtml(r.equipmentName)} <span class="eq-mono eq-muted">${escapeHtml(r.equipmentId)}</span></td>
        <td>${escapeHtml(r.zone)}</td>
        <td>${r.sessionCount}</td>
        <td><span class="eq-badge eq-badge-${accent}">${escapeHtml(usageStatusLabel(r.status))}</span></td>
        <td class="eq-actions-cell">${actions}</td>
      </tr>
    `;
  }).join("");

  el.querySelectorAll("[data-usage-delete]").forEach((btn) => {
    btn.addEventListener("click", () => openUsageDeleteConfirm(Number(btn.getAttribute("data-usage-delete"))));
  });
}

function currentTableFilters() {
  return {
    zone: document.getElementById("usageZoneFilter")?.value || "",
    date: document.getElementById("usageDateFilter")?.value || currentUsageDate,
  };
}

async function applyUsageTableFilters() {
  try {
    const filters = currentTableFilters();
    currentUsageDate = filters.date || todayIso();
    const [table, summary] = await Promise.all([
      fetchUsageTable(filters),
      fetchUsageSummary(currentUsageDate),
    ]);
    renderUsageTable(table);
    renderUsageSummaryCards(summary);
  } catch (err) {
    showUsageToast(err.message || "Couldn't load usage data.");
  }
}

function wireUsageTableToolbar() {
  const zoneFilter = document.getElementById("usageZoneFilter");
  const dateFilter = document.getElementById("usageDateFilter");
  if (dateFilter && !dateFilter.value) dateFilter.value = currentUsageDate;

  [zoneFilter, dateFilter].forEach((el) => {
    el?.addEventListener("change", applyUsageTableFilters);
  });
}

/* ---------- Delete confirmation ---------- */

let usageDeleteTargetId = null;

function openUsageDeleteConfirm(id) {
  usageDeleteTargetId = id;
  document.getElementById("usageDeleteMessage").textContent =
    "Are you sure you want to delete this usage record?";
  document.getElementById("usageModalBackdrop")?.removeAttribute("hidden");
  document.getElementById("usageDeleteModal")?.removeAttribute("hidden");
}

function closeUsageDeleteConfirm() {
  usageDeleteTargetId = null;
  document.getElementById("usageDeleteModal")?.setAttribute("hidden", "");
  document.getElementById("usageModalBackdrop")?.setAttribute("hidden", "");
}

function wireUsageDeleteModal() {
  const cancelBtn = document.getElementById("usageDeleteCancelBtn");
  const confirmBtn = document.getElementById("usageDeleteConfirmBtn");
  if (!cancelBtn || !confirmBtn) return;

  cancelBtn.addEventListener("click", closeUsageDeleteConfirm);

  confirmBtn.addEventListener("click", async () => {
    if (!usageDeleteTargetId) return;
    const id = usageDeleteTargetId;

    confirmBtn.disabled = true;
    confirmBtn.textContent = "Deleting…";

    try {
      await deleteUsageRecordById(id);
      showUsageToast("Usage record deleted.");
      closeUsageDeleteConfirm();
      await Promise.all([applyUsageTableFilters(), refreshTopUsed()]);
    } catch (err) {
      showUsageToast(err.message || "Couldn't delete this usage record.");
    } finally {
      confirmBtn.disabled = false;
      confirmBtn.textContent = "Delete";
    }
  });
}

/* ============================================================
   C. Usage History / Trend
   ============================================================ */

function trendDirection(points) {
  // points are oldest -> newest for day view already reversed for week/month;
  // normalize to chronological order first.
  if (points.length < 2) return null;
  const chronological = [...points].reverse();
  const half = Math.floor(chronological.length / 2) || 1;
  const firstHalf = chronological.slice(0, half);
  const secondHalf = chronological.slice(chronological.length - half);
  const avg = (arr) => arr.reduce((s, p) => s + p.sessionCount, 0) / arr.length;
  const a = avg(firstHalf);
  const b = avg(secondHalf);
  if (b - a > 0.5) return "increasing";
  if (a - b > 0.5) return "decreasing";
  return "stable";
}

function renderUsageHistory(points) {
  const chartEl = document.getElementById("usageHistoryChart");
  const bodyEl = document.getElementById("usageHistoryBody");
  const emptyEl = document.getElementById("usageHistoryEmpty");
  const trendEl = document.getElementById("usageHistoryTrendLabel");
  if (!bodyEl) return;

  if (points.length === 0) {
    if (chartEl) chartEl.innerHTML = "";
    bodyEl.innerHTML = "";
    if (emptyEl) emptyEl.hidden = false;
    if (trendEl) trendEl.textContent = "";
    return;
  }
  if (emptyEl) emptyEl.hidden = true;

  const max = Math.max(...points.map((p) => p.sessionCount), 1);

  if (chartEl) {
    // points come back newest-first; render oldest-first left to right
    const chronological = [...points].reverse();
    chartEl.innerHTML = chronological.map((p) => {
      const pct = Math.round((p.sessionCount / max) * 100);
      return `
        <div class="eq-trend-col" title="${escapeHtml(p.label)}: ${p.sessionCount} sessions">
          <span class="eq-trend-value">${p.sessionCount}</span>
          <span class="eq-trend-bar" style="height:${Math.max(pct, 3)}%"></span>
          <span class="eq-trend-label">${escapeHtml(p.label)}</span>
        </div>
      `;
    }).join("");
  }

  bodyEl.innerHTML = points.map((p) => `
    <tr>
      <td>${escapeHtml(p.label)}</td>
      <td>${p.sessionCount}</td>
    </tr>
  `).join("");

  if (trendEl) {
    const direction = trendDirection(points);
    const labels = { increasing: "Trending up ▲", decreasing: "Trending down ▼", stable: "Stable —" };
    trendEl.textContent = direction ? labels[direction] : "";
  }
}

function populateUsageHistoryEquipmentSelect() {
  const select = document.getElementById("usageHistoryEquipmentSelect");
  if (!select) return;
  select.innerHTML = "";

  usageEquipmentList.forEach((eq) => {
    const opt = document.createElement("option");
    opt.value = eq.id;
    opt.textContent = `${eq.name} (${eq.id})`;
    select.appendChild(opt);
  });

  if (usageEquipmentList.length) {
    currentUsageHistoryEquipmentId = usageEquipmentList[0].id;
    select.value = currentUsageHistoryEquipmentId;
  }
}

async function refreshUsageHistory() {
  if (!currentUsageHistoryEquipmentId) return;
  const groupBy = document.getElementById("usageHistoryGroupBySelect")?.value || "day";
  const daysByGroup = { day: 7, week: 30, month: 180 };
  try {
    const points = await fetchUsageHistory(currentUsageHistoryEquipmentId, {
      groupBy,
      days: daysByGroup[groupBy] || 30,
    });
    renderUsageHistory(points);
  } catch (err) {
    showUsageToast(err.message || "Couldn't load usage history.");
  }
}

function wireUsageHistoryControls() {
  const equipmentSelect = document.getElementById("usageHistoryEquipmentSelect");
  const groupBySelect = document.getElementById("usageHistoryGroupBySelect");

  equipmentSelect?.addEventListener("change", () => {
    currentUsageHistoryEquipmentId = equipmentSelect.value;
    refreshUsageHistory();
  });
  groupBySelect?.addEventListener("change", refreshUsageHistory);
}

/* ============================================================
   D. Top Used Equipment
   ============================================================ */

function renderTopUsed(list) {
  const el = document.getElementById("usageTopList");
  const empty = document.getElementById("usageTopEmpty");
  if (!el) return;

  if (list.length === 0) {
    el.innerHTML = "";
    if (empty) empty.hidden = false;
    return;
  }
  if (empty) empty.hidden = true;

  el.innerHTML = list.map((item, index) => `
    <div class="eq-top-row">
      <span class="eq-top-rank">${index + 1}</span>
      <span class="eq-health-label">${escapeHtml(item.equipmentName)} <span class="eq-mono eq-muted">${escapeHtml(item.equipmentId)}</span></span>
      <span class="eq-health-count">${item.totalSessions} sessions</span>
    </div>
  `).join("");
}

async function refreshTopUsed() {
  try {
    const list = await fetchTopUsedEquipment({ days: 30, limit: 5 });
    renderTopUsed(list);
  } catch (err) {
    showUsageToast(err.message || "Couldn't load top used equipment.");
  }
}

/* ============================================================
   Zone dropdown population (shared by table filter + batch entry)
   ============================================================ */

function populateZoneOptions(equipmentList) {
  usageZones = [...new Set(equipmentList.map((eq) => eq.category))].sort();

  const zoneFilter = document.getElementById("usageZoneFilter");
  const batchZoneSelect = document.getElementById("usageBatchZoneSelect");

  [zoneFilter, batchZoneSelect].forEach((select) => {
    if (!select) return;
    const keepFirst = select.options.length ? select.options[0].outerHTML : "";
    select.innerHTML = keepFirst;
    usageZones.forEach((zone) => {
      const opt = document.createElement("option");
      opt.value = zone;
      opt.textContent = zone;
      select.appendChild(opt);
    });
  });
}

/* ============================================================
   Batch Usage Logging (Gym Manager only)
   ============================================================ */

function renderBatchEntryRows() {
  const tbody = document.getElementById("usageBatchBody");
  const empty = document.getElementById("usageBatchEmpty");
  if (!tbody) return;

  const zone = document.getElementById("usageBatchZoneSelect")?.value || "";
  const rows = zone ? usageEquipmentList.filter((eq) => eq.category === zone) : [];

  if (rows.length === 0) {
    tbody.innerHTML = "";
    if (empty) empty.hidden = false;
    return;
  }
  if (empty) empty.hidden = true;

  tbody.innerHTML = rows.map((eq) => `
    <tr>
      <td>${escapeHtml(eq.name)} <span class="eq-mono eq-muted">${escapeHtml(eq.id)}</span></td>
      <td>
        <input type="number" min="0" class="form-control eq-batch-input"
               data-batch-equipment="${escapeHtml(eq.id)}" value="0" aria-label="Sessions for ${escapeHtml(eq.name)}" />
      </td>
    </tr>
  `).join("");
}

async function prefillBatchEntryFromExisting() {
  const zone = document.getElementById("usageBatchZoneSelect")?.value || "";
  const date = document.getElementById("usageBatchDateInput")?.value || todayIso();
  if (!zone) return;

  try {
    const existing = await fetchUsageTable({ zone, date });
    existing.forEach((record) => {
      const input = document.querySelector(`[data-batch-equipment="${CSS.escape(record.equipmentId)}"]`);
      if (input) input.value = record.sessionCount;
    });
  } catch (err) {
    // Non-fatal — the form still works starting from zero.
  }
}

function wireBatchEntry() {
  const zoneSelect = document.getElementById("usageBatchZoneSelect");
  const dateInput = document.getElementById("usageBatchDateInput");
  const saveBtn = document.getElementById("usageBatchSaveBtn");
  const alertBox = document.getElementById("usageBatchAlert");
  if (!zoneSelect || !saveBtn) return;

  if (dateInput && !dateInput.value) dateInput.value = todayIso();

  const refreshBatchRows = async () => {
    renderBatchEntryRows();
    await prefillBatchEntryFromExisting();
  };

  zoneSelect.addEventListener("change", refreshBatchRows);
  dateInput?.addEventListener("change", refreshBatchRows);

  saveBtn.addEventListener("click", async () => {
    if (alertBox) alertBox.innerHTML = "";
    const zone = zoneSelect.value;
    const usageDate = dateInput?.value || todayIso();

    if (!zone) {
      showAlert(alertBox, "Select a zone first.");
      return;
    }

    const inputs = document.querySelectorAll("[data-batch-equipment]");
    const entries = Array.from(inputs).map((input) => ({
      equipmentId: input.getAttribute("data-batch-equipment"),
      sessionCount: Number(input.value) || 0,
    }));

    if (entries.length === 0) {
      showAlert(alertBox, "No equipment in this zone.");
      return;
    }

    saveBtn.disabled = true;
    const originalLabel = saveBtn.textContent;
    saveBtn.textContent = "Saving…";

    try {
      await saveUsageBatch({ usageDate, entries });
      showUsageToast(`Saved sessions for ${entries.length} machine(s) in ${zone} on ${usageDate}.`);
      await Promise.all([applyUsageTableFilters(), refreshTopUsed(), refreshUsageHistory()]);
    } catch (err) {
      showAlert(alertBox, err.message || "Couldn't save these usage entries.");
    } finally {
      saveBtn.disabled = false;
      saveBtn.textContent = originalLabel;
    }
  });
}

/* ============================================================
   Load + refresh
   ============================================================ */

/* ---------- entry point, called from admin-dashboard.html and gym-manager-dashboard.html ---------- */

async function initUsageDashboard(session) {
  usageSession = session;
  currentUsageDate = todayIso();

  wireUsageTableToolbar();
  wireUsageDeleteModal();
  wireUsageHistoryControls();

  if (session.role === ROLES.GYM_MANAGER) {
    wireBatchEntry();
  }

  try {
    usageEquipmentList = await fetchEquipment();
    populateZoneOptions(usageEquipmentList);
    populateUsageHistoryEquipmentSelect();
    if (session.role === ROLES.GYM_MANAGER) {
      renderBatchEntryRows();
    }
  } catch (err) {
    showUsageToast(err.message || "Couldn't load the equipment list.");
  }

  try {
    await Promise.all([
      applyUsageTableFilters(),
      refreshTopUsed(),
      refreshUsageHistory(),
    ]);
  } catch (err) {
    showUsageToast(err.message || "Couldn't load usage data.");
  }
}

window.initUsageDashboard = initUsageDashboard;
