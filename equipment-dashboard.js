/* ============================================================
   Gym Equipment Maintenance System — Equipment Dashboard
   equipment-dashboard.js
   ------------------------------------------------------------
   Renders the Admin Equipment Dashboard. Depends on:
     - script.js        (session/auth — untouched)
     - equipment-data.js (data access — swap-in point for a
                           real backend later)

   Nothing here touches localStorage, login, or registration.
   ============================================================ */

const ATTENTION_STATUSES = [
  EQUIPMENT_STATUS.OUT_OF_SERVICE,
  EQUIPMENT_STATUS.MAINTENANCE_DUE,
  EQUIPMENT_STATUS.UNDER_MAINTENANCE,
];

const PRIORITY_RANK = { High: 0, Medium: 1, Low: 2 };

const DUE_SOON_WINDOW_DAYS = 7;

/* ---------- small formatting helpers ---------- */

function formatDate(isoDate) {
  if (!isoDate) return "—";
  const d = new Date(`${isoDate}T00:00:00`);
  return d.toLocaleDateString("en-GB", { day: "2-digit", month: "short", year: "numeric" });
}

function daysUntil(isoDate) {
  if (!isoDate) return null;
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const due = new Date(`${isoDate}T00:00:00`);
  return Math.round((due - today) / (1000 * 60 * 60 * 24));
}

/** Human label for the Maintenance Attention table, layered on top of the core status. */
function attentionLabel(item) {
  if (item.status === EQUIPMENT_STATUS.OUT_OF_SERVICE) return "Out of Service";
  if (item.status === EQUIPMENT_STATUS.UNDER_MAINTENANCE) return "Under Maintenance";
  if (item.status === EQUIPMENT_STATUS.MAINTENANCE_DUE) {
    const days = daysUntil(item.dueDate);
    if (days !== null && days < 0) return "Overdue";
    if (days !== null && days <= DUE_SOON_WINDOW_DAYS) return "Due Soon";
    return "Maintenance Due";
  }
  return item.status;
}

/** Maps a status to the shared design-system accent (see style.css :root). */
function statusAccent(status) {
  switch (status) {
    case EQUIPMENT_STATUS.ACTIVE: return "ok";
    case EQUIPMENT_STATUS.MAINTENANCE_DUE: return "warn";
    case EQUIPMENT_STATUS.UNDER_MAINTENANCE: return "info";
    case EQUIPMENT_STATUS.OUT_OF_SERVICE: return "danger";
    default: return "info";
  }
}

function statusDotEmoji(status) {
  switch (status) {
    case EQUIPMENT_STATUS.ACTIVE: return "🟢";
    case EQUIPMENT_STATUS.MAINTENANCE_DUE: return "🟡";
    case EQUIPMENT_STATUS.UNDER_MAINTENANCE: return "🔵";
    case EQUIPMENT_STATUS.OUT_OF_SERVICE: return "🔴";
    default: return "⚪";
  }
}

function escapeHtml(str) {
  return String(str).replace(/[&<>"']/g, (c) => ({
    "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;",
  }[c]));
}

/* ---------- derived views over the raw equipment list ---------- */

function computeSummary(items) {
  return {
    total: items.length,
    active: items.filter((i) => i.status === EQUIPMENT_STATUS.ACTIVE).length,
    maintenanceDue: items.filter((i) => i.status === EQUIPMENT_STATUS.MAINTENANCE_DUE).length,
    outOfService: items.filter((i) => i.status === EQUIPMENT_STATUS.OUT_OF_SERVICE).length,
  };
}

function computeHealth(items) {
  const counts = {
    [EQUIPMENT_STATUS.ACTIVE]: 0,
    [EQUIPMENT_STATUS.MAINTENANCE_DUE]: 0,
    [EQUIPMENT_STATUS.UNDER_MAINTENANCE]: 0,
    [EQUIPMENT_STATUS.OUT_OF_SERVICE]: 0,
  };
  items.forEach((i) => { counts[i.status] = (counts[i.status] || 0) + 1; });
  return counts;
}

function getAttentionList(items) {
  return items
    .filter((i) => ATTENTION_STATUSES.includes(i.status))
    .sort((a, b) => {
      const rankDiff = (PRIORITY_RANK[a.priority] ?? 3) - (PRIORITY_RANK[b.priority] ?? 3);
      if (rankDiff !== 0) return rankDiff;
      return (a.dueDate || "").localeCompare(b.dueDate || "");
    });
}

function getOverviewList(items, { search = "", category = "", status = "" } = {}) {
  const needle = search.trim().toLowerCase();
  return items
    .filter((i) => !needle || i.name.toLowerCase().includes(needle) || i.id.toLowerCase().includes(needle))
    .filter((i) => !category || i.category === category)
    .filter((i) => !status || i.status === status)
    .sort((a, b) => b.addedDate.localeCompare(a.addedDate));
}

/* ---------- rendering ---------- */

function renderSummaryCards(summary) {
  const el = document.getElementById("eqSummaryCards");
  if (!el) return;
  const cards = [
    { label: "Total Equipment", value: summary.total, accent: "info" },
    { label: "Active Equipment", value: summary.active, accent: "ok" },
    { label: "Maintenance Due", value: summary.maintenanceDue, accent: "warn" },
    { label: "Out of Service", value: summary.outOfService, accent: "danger" },
  ];
  el.innerHTML = cards.map((c) => `
    <div class="eq-card eq-card-${c.accent}">
      <span class="eq-card-value">${c.value}</span>
      <span class="eq-card-label">${c.label}</span>
    </div>
  `).join("");
}

function renderHealthOverview(counts, total) {
  const el = document.getElementById("eqHealthOverview");
  if (!el) return;
  const rows = [
    { key: EQUIPMENT_STATUS.ACTIVE, label: "Healthy / Active", accent: "ok" },
    { key: EQUIPMENT_STATUS.MAINTENANCE_DUE, label: "Maintenance Due", accent: "warn" },
    { key: EQUIPMENT_STATUS.UNDER_MAINTENANCE, label: "Under Maintenance", accent: "info" },
    { key: EQUIPMENT_STATUS.OUT_OF_SERVICE, label: "Out of Service", accent: "danger" },
  ];
  el.innerHTML = rows.map((r) => {
    const count = counts[r.key] || 0;
    const pct = total ? Math.round((count / total) * 100) : 0;
    return `
      <div class="eq-health-row">
        <span class="eq-dot eq-dot-${r.accent}" aria-hidden="true"></span>
        <span class="eq-health-label">${r.label}</span>
        <span class="eq-health-bar-track">
          <span class="eq-health-bar-fill eq-dot-${r.accent}" style="width:${pct}%"></span>
        </span>
        <span class="eq-health-count">${count}</span>
      </div>
    `;
  }).join("");
}

function renderAttentionTable(list) {
  const el = document.getElementById("eqAttentionBody");
  const empty = document.getElementById("eqAttentionEmpty");
  if (!el) return;

  if (list.length === 0) {
    el.innerHTML = "";
    if (empty) empty.hidden = false;
    return;
  }
  if (empty) empty.hidden = true;

  el.innerHTML = list.map((i) => `
    <tr>
      <td class="eq-mono">${escapeHtml(i.id)}</td>
      <td>${escapeHtml(i.name)}</td>
      <td><span class="eq-badge eq-badge-${statusAccent(i.status)}">${statusDotEmoji(i.status)} ${escapeHtml(attentionLabel(i))}</span></td>
      <td>${formatDate(i.dueDate)}</td>
      <td>${i.priority ? `<span class="eq-priority eq-priority-${i.priority.toLowerCase()}">${escapeHtml(i.priority)}</span>` : "—"}</td>
    </tr>
  `).join("");
}

function renderOverviewTable(list) {
  const el = document.getElementById("eqOverviewBody");
  const empty = document.getElementById("eqOverviewEmpty");
  if (!el) return;

  if (list.length === 0) {
    el.innerHTML = "";
    if (empty) empty.hidden = false;
    return;
  }
  if (empty) empty.hidden = true;

  el.innerHTML = list.slice(0, 8).map((i) => `
    <tr>
      <td class="eq-mono">${escapeHtml(i.id)}</td>
      <td>${escapeHtml(i.name)}</td>
      <td>${escapeHtml(i.category)}</td>
      <td><span class="eq-badge eq-badge-${statusAccent(i.status)}">${statusDotEmoji(i.status)} ${escapeHtml(i.status)}</span></td>
    </tr>
  `).join("");
}

function renderUserChip(session) {
  const el = document.getElementById("eqUserChip");
  if (!el || !session) return;
  el.textContent = `${session.fullName} · ${session.role}`;
}

/* ---------- quick actions (no other modules exist yet) ---------- */

function showToast(message) {
  const el = document.getElementById("eqToast");
  if (!el) return;
  el.textContent = message;
  el.classList.add("eq-toast-visible");
  window.clearTimeout(showToast._t);
  showToast._t = window.setTimeout(() => el.classList.remove("eq-toast-visible"), 3200);
}

function wireQuickActions() {
  document.querySelectorAll("[data-eq-action]").forEach((btn) => {
    btn.addEventListener("click", () => {
      showToast(`${btn.textContent.trim()} isn't built yet — coming in a later module.`);
    });
  });
}

/* ---------- search / filter ---------- */

function populateCategoryFilter(items) {
  const select = document.getElementById("eqCategoryFilter");
  if (!select) return;
  const categories = [...new Set(items.map((i) => i.category))].sort();
  categories.forEach((cat) => {
    const opt = document.createElement("option");
    opt.value = cat;
    opt.textContent = cat;
    select.appendChild(opt);
  });
}

function wireToolbar(items) {
  const searchInput = document.getElementById("eqSearchInput");
  const categorySelect = document.getElementById("eqCategoryFilter");
  const statusSelect = document.getElementById("eqStatusFilter");
  if (!searchInput || !categorySelect || !statusSelect) return;

  const apply = () => {
    const filtered = getOverviewList(items, {
      search: searchInput.value,
      category: categorySelect.value,
      status: statusSelect.value,
    });
    renderOverviewTable(filtered);
  };

  searchInput.addEventListener("input", apply);
  categorySelect.addEventListener("change", apply);
  statusSelect.addEventListener("change", apply);
}

/* ---------- entry point, called from admin-dashboard.html ---------- */

async function initEquipmentDashboard(session) {
  renderUserChip(session);
  wireQuickActions();

  const items = await fetchEquipment();

  const summary = computeSummary(items);
  renderSummaryCards(summary);
  renderHealthOverview(computeHealth(items), summary.total);
  renderAttentionTable(getAttentionList(items));
  renderOverviewTable(getOverviewList(items));

  populateCategoryFilter(items);
  wireToolbar(items);
}

window.initEquipmentDashboard = initEquipmentDashboard;
