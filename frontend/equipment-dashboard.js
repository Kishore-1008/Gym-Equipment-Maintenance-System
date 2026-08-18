/* ============================================================
   Gym Equipment Maintenance System — Equipment Dashboard
   equipment-dashboard.js
   ------------------------------------------------------------
   Renders the Admin Equipment Dashboard and drives the
   Add / Edit / Delete / Search / Filter workflows. Depends on:
     - script.js         (session/auth + apiRequest — untouched)
     - equipment-data.js (data access — talks to Spring Boot)

   Nothing here touches localStorage, login, or registration.
   ============================================================ */

const ATTENTION_STATUSES = [
  EQUIPMENT_STATUS.OUT_OF_SERVICE,
  EQUIPMENT_STATUS.MAINTENANCE_DUE,
  EQUIPMENT_STATUS.UNDER_MAINTENANCE,
];

/** Ordering for the Maintenance Attention table: most urgent first. */
const ATTENTION_RANK = {
  [EQUIPMENT_STATUS.OUT_OF_SERVICE]: 0,
  [EQUIPMENT_STATUS.MAINTENANCE_DUE]: 1,
  [EQUIPMENT_STATUS.UNDER_MAINTENANCE]: 2,
};

/** All equipment currently loaded from the API — refreshed after every CRUD action. */
let currentItems = [];

/* ---------- small formatting helpers ---------- */

function escapeHtml(str) {
  return String(str).replace(/[&<>"']/g, (c) => ({
    "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;",
  }[c]));
}

/** Maps a status code to the shared design-system accent (see style.css :root). */
function statusAccent(status) {
  switch (status) {
    case EQUIPMENT_STATUS.OPERATIONAL: return "ok";
    case EQUIPMENT_STATUS.MAINTENANCE_DUE: return "warn";
    case EQUIPMENT_STATUS.UNDER_MAINTENANCE: return "info";
    case EQUIPMENT_STATUS.OUT_OF_SERVICE: return "danger";
    default: return "info";
  }
}

function statusDotEmoji(status) {
  switch (status) {
    case EQUIPMENT_STATUS.OPERATIONAL: return "🟢";
    case EQUIPMENT_STATUS.MAINTENANCE_DUE: return "🟡";
    case EQUIPMENT_STATUS.UNDER_MAINTENANCE: return "🔵";
    case EQUIPMENT_STATUS.OUT_OF_SERVICE: return "🔴";
    default: return "⚪";
  }
}

/* ---------- derived views over the raw equipment list ---------- */

function computeSummary(items) {
  return {
    total: items.length,
    operational: items.filter((i) => i.status === EQUIPMENT_STATUS.OPERATIONAL).length,
    maintenanceDue: items.filter((i) => i.status === EQUIPMENT_STATUS.MAINTENANCE_DUE).length,
    outOfService: items.filter((i) => i.status === EQUIPMENT_STATUS.OUT_OF_SERVICE).length,
  };
}

function computeHealth(items) {
  const counts = {
    [EQUIPMENT_STATUS.OPERATIONAL]: 0,
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
    .sort((a, b) => (ATTENTION_RANK[a.status] ?? 3) - (ATTENTION_RANK[b.status] ?? 3));
}

function getOverviewList(items, { search = "", category = "", status = "" } = {}) {
  const needle = search.trim().toLowerCase();
  return items
    .filter((i) => !needle || i.name.toLowerCase().includes(needle) || i.id.toLowerCase().includes(needle))
    .filter((i) => !category || i.category === category)
    .filter((i) => !status || i.status === status)
    .sort((a, b) => a.id.localeCompare(b.id));
}

/* ---------- rendering ---------- */

function renderSummaryCards(summary) {
  const el = document.getElementById("eqSummaryCards");
  if (!el) return;
  const cards = [
    { label: "Total Equipment", value: summary.total, accent: "info" },
    { label: "Operational", value: summary.operational, accent: "ok" },
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
    { key: EQUIPMENT_STATUS.OPERATIONAL, label: "Operational", accent: "ok" },
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
      <td>${escapeHtml(i.category)}</td>
      <td>${escapeHtml(maintenanceIntervalLabel(i.maintenanceInterval))}</td>
      <td><span class="eq-badge eq-badge-${statusAccent(i.status)}">${statusDotEmoji(i.status)} ${escapeHtml(equipmentStatusLabel(i.status))}</span></td>
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

  el.innerHTML = list.map((i) => `
    <tr>
      <td class="eq-mono">${escapeHtml(i.id)}</td>
      <td>${escapeHtml(i.name)}</td>
      <td>${escapeHtml(i.category)}</td>
      <td><span class="eq-badge eq-badge-${statusAccent(i.status)}">${statusDotEmoji(i.status)} ${escapeHtml(equipmentStatusLabel(i.status))}</span></td>
      <td class="eq-actions-cell">
        <button type="button" class="eq-row-btn" data-eq-edit="${escapeHtml(i.id)}">Edit</button>
        <button type="button" class="eq-row-btn eq-row-btn-danger" data-eq-delete="${escapeHtml(i.id)}">Delete</button>
      </td>
    </tr>
  `).join("");

  el.querySelectorAll("[data-eq-edit]").forEach((btn) => {
    btn.addEventListener("click", () => openEditModal(btn.getAttribute("data-eq-edit")));
  });
  el.querySelectorAll("[data-eq-delete]").forEach((btn) => {
    btn.addEventListener("click", () => openDeleteConfirm(btn.getAttribute("data-eq-delete")));
  });
}

function renderUserChip(session) {
  const el = document.getElementById("eqUserChip");
  if (!el || !session) return;
  el.textContent = `${session.fullName} · ${roleLabel(session.role)}`;
}

/* ---------- toast ---------- */

function showToast(message) {
  const el = document.getElementById("eqToast");
  if (!el) return;
  el.textContent = message;
  el.classList.add("eq-toast-visible");
  window.clearTimeout(showToast._t);
  showToast._t = window.setTimeout(() => el.classList.remove("eq-toast-visible"), 3200);
}

/* ---------- quick actions ---------- */

function wireQuickActions() {
  document.querySelectorAll("[data-eq-action]").forEach((btn) => {
    btn.addEventListener("click", () => {
      const action = btn.getAttribute("data-eq-action");
      if (action === "add") {
        openAddModal();
      } else if (action === "view") {
        document.getElementById("eqOverviewBody")?.closest("section")?.scrollIntoView({ behavior: "smooth" });
      } else {
        showToast(`${btn.textContent.trim()} isn't built yet — coming in a later module.`);
      }
    });
  });
}

/* ---------- search / filter ---------- */

function populateCategoryFilter(items) {
  const select = document.getElementById("eqCategoryFilter");
  if (!select) return;
  const previousValue = select.value;
  select.querySelectorAll("option:not([value=''])").forEach((opt) => opt.remove());

  const categories = [...new Set(items.map((i) => i.category))].sort();
  categories.forEach((cat) => {
    const opt = document.createElement("option");
    opt.value = cat;
    opt.textContent = cat;
    select.appendChild(opt);
  });

  if (categories.includes(previousValue)) select.value = previousValue;
}

function applyToolbarFilters() {
  const searchInput = document.getElementById("eqSearchInput");
  const categorySelect = document.getElementById("eqCategoryFilter");
  const statusSelect = document.getElementById("eqStatusFilter");
  if (!searchInput || !categorySelect || !statusSelect) return;

  const filtered = getOverviewList(currentItems, {
    search: searchInput.value,
    category: categorySelect.value,
    status: statusSelect.value,
  });
  renderOverviewTable(filtered);
}

function wireToolbar() {
  const searchInput = document.getElementById("eqSearchInput");
  const categorySelect = document.getElementById("eqCategoryFilter");
  const statusSelect = document.getElementById("eqStatusFilter");
  if (!searchInput || !categorySelect || !statusSelect) return;

  searchInput.addEventListener("input", applyToolbarFilters);
  categorySelect.addEventListener("change", applyToolbarFilters);
  statusSelect.addEventListener("change", applyToolbarFilters);
}

/* ============================================================
   Add / Edit Equipment modal
   ============================================================ */

let modalMode = "add"; // "add" | "edit"
let modalEditingId = null;

function populateStaticSelectOptions() {
  const nameSelect = document.getElementById("eqFieldName");
  const intervalSelect = document.getElementById("eqFieldInterval");
  const statusSelect = document.getElementById("eqFieldStatus");
  if (!nameSelect || !intervalSelect || !statusSelect) return;

  if (nameSelect.options.length <= 1) {
    EQUIPMENT_NAME_OPTIONS.forEach((name) => {
      const opt = document.createElement("option");
      opt.value = name;
      opt.textContent = name;
      nameSelect.appendChild(opt);
    });
  }

  if (intervalSelect.options.length <= 1) {
    Object.entries(MAINTENANCE_INTERVAL_LABELS).forEach(([code, label]) => {
      const opt = document.createElement("option");
      opt.value = code;
      opt.textContent = label;
      intervalSelect.appendChild(opt);
    });
  }

  if (statusSelect.options.length === 0) {
    Object.entries(EQUIPMENT_STATUS_LABELS).forEach(([code, label]) => {
      const opt = document.createElement("option");
      opt.value = code;
      opt.textContent = label;
      statusSelect.appendChild(opt);
    });
  }
}

function updateCategoryPreview() {
  const nameSelect = document.getElementById("eqFieldName");
  const categoryField = document.getElementById("eqFieldCategory");
  if (!nameSelect || !categoryField) return;
  categoryField.value = EQUIPMENT_NAME_TO_CATEGORY[nameSelect.value] || "";
}

/**
 * Mirrors the backend's generateNextCode() (EquipmentService.java) purely for
 * display: scan existing EQ### codes, take the highest numeric suffix, add 1,
 * zero-pad to 3 digits. Reads from currentItems — the same list already
 * fetched via GET /api/equipment — so no extra API call is made just to show
 * this preview. The backend remains the sole authority on the code it
 * actually assigns on save; this only has to match it closely enough to be a
 * useful preview.
 */
function computeNextEquipmentId(items) {
  const CODE_PATTERN = /^EQ(\d+)$/;
  const max = items.reduce((highest, item) => {
    const match = CODE_PATTERN.exec(item.id || "");
    if (!match) return highest;
    const n = parseInt(match[1], 10);
    return n > highest ? n : highest;
  }, 0);

  return `EQ${String(max + 1).padStart(3, "0")}`;
}

function showModal(el) {
  document.getElementById("eqModalBackdrop")?.removeAttribute("hidden");
  el.removeAttribute("hidden");
}

function hideModal(el) {
  el.setAttribute("hidden", "");
  const anyOpen = !document.getElementById("eqModal").hasAttribute("hidden") ||
                  !document.getElementById("eqDeleteModal").hasAttribute("hidden");
  if (!anyOpen) document.getElementById("eqModalBackdrop")?.setAttribute("hidden", "");
}

function openAddModal() {
  modalMode = "add";
  modalEditingId = null;

  document.getElementById("eqModalTitle").textContent = "Add Equipment";
  document.getElementById("eqFormAlert").innerHTML = "";
  document.getElementById("eqFieldId").value = computeNextEquipmentId(currentItems);
  document.getElementById("eqFieldName").value = "";
  document.getElementById("eqFieldCategory").value = "";
  document.getElementById("eqFieldInterval").value = "";
  document.getElementById("eqFieldStatus").value = EQUIPMENT_STATUS.OPERATIONAL;
  document.getElementById("eqFieldUsageLimit").value = "";
  document.getElementById("eqModalSaveBtn").textContent = "Save Equipment";

  showModal(document.getElementById("eqModal"));
}

function openEditModal(equipmentId) {
  const item = currentItems.find((i) => i.id === equipmentId);
  if (!item) return;

  modalMode = "edit";
  modalEditingId = equipmentId;

  document.getElementById("eqModalTitle").textContent = `Edit ${item.id}`;
  document.getElementById("eqFormAlert").innerHTML = "";
  document.getElementById("eqFieldId").value = item.id;
  document.getElementById("eqFieldName").value = item.name;
  document.getElementById("eqFieldCategory").value = item.category;
  document.getElementById("eqFieldInterval").value = item.maintenanceInterval;
  document.getElementById("eqFieldStatus").value = item.status;
  document.getElementById("eqFieldUsageLimit").value = item.maintenanceUsageLimitHours ?? "";
  document.getElementById("eqModalSaveBtn").textContent = "Save Changes";

  showModal(document.getElementById("eqModal"));
}

function closeEquipmentModal() {
  hideModal(document.getElementById("eqModal"));
}

function wireEquipmentModal() {
  const modal = document.getElementById("eqModal");
  const form = document.getElementById("eqForm");
  const nameSelect = document.getElementById("eqFieldName");
  const cancelBtn = document.getElementById("eqModalCancelBtn");
  const closeBtn = document.getElementById("eqModalCloseBtn");
  if (!modal || !form) return;

  populateStaticSelectOptions();

  nameSelect.addEventListener("change", updateCategoryPreview);
  cancelBtn?.addEventListener("click", closeEquipmentModal);
  closeBtn?.addEventListener("click", closeEquipmentModal);

  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    const alertBox = document.getElementById("eqFormAlert");
    alertBox.innerHTML = "";

    const equipmentName = document.getElementById("eqFieldName").value;
    const maintenanceInterval = document.getElementById("eqFieldInterval").value;
    const status = document.getElementById("eqFieldStatus").value;
    const usageLimitRaw = document.getElementById("eqFieldUsageLimit").value;
    const maintenanceUsageLimitHours = usageLimitRaw === "" ? null : Number(usageLimitRaw);

    if (!equipmentName || !maintenanceInterval || !status) {
      showAlert(alertBox, "Please fill in every field.");
      return;
    }
    if (maintenanceUsageLimitHours !== null && maintenanceUsageLimitHours < 0) {
      showAlert(alertBox, "Maintenance usage limit can't be negative.");
      return;
    }

    const saveBtn = document.getElementById("eqModalSaveBtn");
    saveBtn.disabled = true;
    const originalLabel = saveBtn.textContent;
    saveBtn.textContent = "Saving…";

    try {
      if (modalMode === "add") {
        const created = await createEquipment({ equipmentName, maintenanceInterval, status, maintenanceUsageLimitHours });
        showToast(`${created.id} added.`);
      } else {
        const updated = await updateEquipment(modalEditingId, { equipmentName, maintenanceInterval, status, maintenanceUsageLimitHours });
        showToast(`${updated.id} updated.`);
      }
      closeEquipmentModal();
      await refreshDashboard();
    } catch (err) {
      showAlert(alertBox, err.message || "Couldn't save this equipment. Please try again.");
    } finally {
      saveBtn.disabled = false;
      saveBtn.textContent = originalLabel;
    }
  });
}

/* ---------- Delete confirmation modal ---------- */

let deleteTargetId = null;

function openDeleteConfirm(equipmentId) {
  deleteTargetId = equipmentId;
  document.getElementById("eqDeleteMessage").textContent =
    `Are you sure you want to delete ${equipmentId}?`;
  showModal(document.getElementById("eqDeleteModal"));
}

function closeDeleteConfirm() {
  deleteTargetId = null;
  hideModal(document.getElementById("eqDeleteModal"));
}

function wireDeleteModal() {
  const cancelBtn = document.getElementById("eqDeleteCancelBtn");
  const confirmBtn = document.getElementById("eqDeleteConfirmBtn");
  if (!cancelBtn || !confirmBtn) return;

  cancelBtn.addEventListener("click", closeDeleteConfirm);

  confirmBtn.addEventListener("click", async () => {
    if (!deleteTargetId) return;
    const id = deleteTargetId;

    confirmBtn.disabled = true;
    confirmBtn.textContent = "Deleting…";

    try {
      await deleteEquipmentById(id);
      showToast(`${id} deleted.`);
      closeDeleteConfirm();
      await refreshDashboard();
    } catch (err) {
      showToast(err.message || "Couldn't delete this equipment.");
    } finally {
      confirmBtn.disabled = false;
      confirmBtn.textContent = "Delete";
    }
  });
}

/* ============================================================
   Load + refresh
   ============================================================ */

async function refreshDashboard() {
  currentItems = await fetchEquipment();

  const summary = computeSummary(currentItems);
  renderSummaryCards(summary);
  renderHealthOverview(computeHealth(currentItems), summary.total);
  renderAttentionTable(getAttentionList(currentItems));
  populateCategoryFilter(currentItems);
  applyToolbarFilters();
}

/* ---------- entry point, called from admin-dashboard.html ---------- */

async function initEquipmentDashboard(session) {
  renderUserChip(session);
  wireQuickActions();
  wireToolbar();
  wireEquipmentModal();
  wireDeleteModal();

  try {
    await refreshDashboard();
  } catch (err) {
    showToast(err.message || "Couldn't load equipment from the server.");
  }
}

window.initEquipmentDashboard = initEquipmentDashboard;
