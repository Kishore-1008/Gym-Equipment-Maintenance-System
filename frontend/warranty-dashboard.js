/* ============================================================
   Gym Equipment Maintenance System — Warranty Management
   warranty-dashboard.js
   ------------------------------------------------------------
   Admin-only. Renders summary cards, the warranty table (with
   search + status filter), and the Add/Edit/View modals.
   Reuses the shared ops-modal machinery (opsShowModal/
   opsHideModal/showOpsToast/OPS_MODAL_IDS) and eq-detail-row
   helper already established by repair-dashboard.js — same
   pattern repair-history-dashboard.js follows, so this file
   does not duplicate that plumbing.
   Depends on:
     - script.js            (session/auth + apiRequest)
     - equipment-data.js     (equipment list, for the picker)
     - repair-dashboard.js   (shared modal/toast/detail-row helpers — loaded first)
     - warranty-data.js      (data access for this module)
   ============================================================ */

let allWarranties = [];
let warrantyEditingId = null; // null = Add mode, otherwise the id being edited

/* ---------- formatting helpers (self-contained, mirrors repairEscapeHtml) ---------- */

function warrantyEscapeHtml(str) {
  return String(str == null ? "" : str).replace(/[&<>"']/g, (c) => ({
    "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;",
  }[c]));
}

function warrantyShowAlert(container, message, type = "danger") {
  if (!container) return;
  container.innerHTML = `<div class="alert alert-tag alert-tag-${type}" role="alert">${warrantyEscapeHtml(message)}</div>`;
}

function warrantyClearAlert(container) {
  if (container) container.innerHTML = "";
}

function warrantyStatusAccent(status) {
  switch (status) {
    case WARRANTY_STATUS.ACTIVE: return "ok";
    case WARRANTY_STATUS.EXPIRING_SOON: return "warn";
    case WARRANTY_STATUS.EXPIRED: return "danger";
    default: return "info";
  }
}

function warrantyStatusBadge(status) {
  return `<span class="eq-badge eq-badge-${warrantyStatusAccent(status)}">${warrantyEscapeHtml(warrantyStatusLabel(status))}</span>`;
}

/** "142 days remaining" / "Expires today" / "Expired 20 days ago" — no overcomplication, just signed day math already done server-side. */
function warrantyDaysRemainingText(w) {
  const days = w.daysRemaining;
  if (days > 0) return `${days} day${days === 1 ? "" : "s"} remaining`;
  if (days === 0) return "Expires today";
  const daysAgo = Math.abs(days);
  return `Expired ${daysAgo} day${daysAgo === 1 ? "" : "s"} ago`;
}

/* ============================================================
   Summary cards
   ============================================================ */

function renderWarrantySummaryCards() {
  const container = document.getElementById("warrantySummaryCards");
  if (!container) return;

  const total = allWarranties.length;
  const active = allWarranties.filter((w) => w.status === WARRANTY_STATUS.ACTIVE).length;
  const expiringSoon = allWarranties.filter((w) => w.status === WARRANTY_STATUS.EXPIRING_SOON).length;
  const expired = allWarranties.filter((w) => w.status === WARRANTY_STATUS.EXPIRED).length;

  const cards = [
    { label: "Total Warranties", value: total, accent: "info" },
    { label: "Active", value: active, accent: "ok" },
    { label: "Expiring Soon", value: expiringSoon, accent: "warn" },
    { label: "Expired", value: expired, accent: "danger" },
  ];

  container.innerHTML = cards.map((c) => `
    <div class="eq-card eq-card-${c.accent}">
      <span class="eq-card-value">${c.value}</span>
      <span class="eq-card-label">${warrantyEscapeHtml(c.label)}</span>
    </div>`).join("");
}

/* ============================================================
   Table — search + status filter
   ============================================================ */

function warrantyMatchesSearch(w, query) {
  if (!query) return true;
  const haystack = `${w.warrantyCode} ${w.equipmentName} ${w.equipmentId} ${w.provider}`.toLowerCase();
  return haystack.includes(query.toLowerCase());
}

function renderWarrantyTable() {
  const body = document.getElementById("warrantyBody");
  const empty = document.getElementById("warrantyEmpty");
  if (!body) return;

  const query = document.getElementById("warrantySearchInput")?.value.trim() || "";
  const statusFilter = document.getElementById("warrantyStatusFilter")?.value || "";

  const rows = allWarranties.filter((w) =>
    warrantyMatchesSearch(w, query) && (!statusFilter || w.status === statusFilter)
  );

  if (rows.length === 0) {
    body.innerHTML = "";
    if (empty) empty.hidden = false;
    return;
  }
  if (empty) empty.hidden = true;

  body.innerHTML = rows.map((w) => `
    <tr>
      <td class="eq-mono">${warrantyEscapeHtml(w.warrantyCode)}</td>
      <td>${warrantyEscapeHtml(w.equipmentName)} <span class="eq-mono eq-muted">${warrantyEscapeHtml(w.equipmentId)}</span></td>
      <td>${warrantyEscapeHtml(w.provider)}</td>
      <td>${warrantyEscapeHtml(warrantyTypeLabel(w.warrantyType))}</td>
      <td>${warrantyEscapeHtml(w.startDate)}</td>
      <td>${warrantyEscapeHtml(w.expiryDate)}</td>
      <td>${warrantyStatusBadge(w.status)}</td>
      <td class="eq-actions-cell">
        <button type="button" class="eq-row-btn" data-warranty-view="${w.id}">View</button>
        <button type="button" class="eq-row-btn" data-warranty-edit="${w.id}">Edit</button>
      </td>
    </tr>`).join("");

  body.querySelectorAll("[data-warranty-view]").forEach((btn) => {
    btn.addEventListener("click", () => openWarrantyDetailsModal(btn.getAttribute("data-warranty-view")));
  });
  body.querySelectorAll("[data-warranty-edit]").forEach((btn) => {
    btn.addEventListener("click", () => openEditWarrantyModal(btn.getAttribute("data-warranty-edit")));
  });
}

async function loadWarranties() {
  try {
    allWarranties = await fetchWarranties();
    renderWarrantySummaryCards();
    renderWarrantyTable();
  } catch (err) {
    showOpsToast(err.message);
  }
}

function wireWarrantyToolbar() {
  document.getElementById("warrantySearchInput")?.addEventListener("input", renderWarrantyTable);
  document.getElementById("warrantyStatusFilter")?.addEventListener("change", renderWarrantyTable);
}

/* ============================================================
   Add / Edit Warranty modal (shared form, mode-aware)
   ============================================================ */

async function populateWarrantyEquipmentSelect() {
  const select = document.getElementById("warrantyEquipmentSelect");
  if (!select) return;
  try {
    const equipment = await fetchEquipment();
    select.innerHTML =
      `<option value="" selected disabled>Select equipment</option>` +
      equipment.map((e) => `<option value="${warrantyEscapeHtml(e.id)}">${warrantyEscapeHtml(e.name)} — ${warrantyEscapeHtml(e.id)}</option>`).join("");
  } catch (err) {
    warrantyShowAlert(document.getElementById("warrantyFormAlert"), err.message);
  }
}

function populateWarrantyTypeSelect() {
  const select = document.getElementById("warrantyTypeSelect");
  if (!select || select.options.length > 1) return; // static list, populate once
  select.innerHTML =
    `<option value="" selected disabled>Select type</option>` +
    Object.keys(WARRANTY_TYPE).map((code) => `<option value="${code}">${warrantyEscapeHtml(warrantyTypeLabel(code))}</option>`).join("");
}

function openAddWarrantyModal() {
  warrantyEditingId = null;
  const modal = document.getElementById("warrantyFormModal");
  if (!modal) return;

  document.getElementById("warrantyFormTitle").textContent = "Add Warranty";
  document.getElementById("warrantyFormSaveBtn").textContent = "Save Warranty";
  document.getElementById("warrantyForm").reset();
  warrantyClearAlert(document.getElementById("warrantyFormAlert"));

  populateWarrantyTypeSelect();
  populateWarrantyEquipmentSelect();
  opsShowModal(modal);
}

function openEditWarrantyModal(id) {
  const w = allWarranties.find((rec) => String(rec.id) === String(id));
  if (!w) return;

  warrantyEditingId = w.id;
  const modal = document.getElementById("warrantyFormModal");
  if (!modal) return;

  document.getElementById("warrantyFormTitle").textContent = "Edit Warranty";
  document.getElementById("warrantyFormSaveBtn").textContent = "Save Warranty";
  warrantyClearAlert(document.getElementById("warrantyFormAlert"));

  populateWarrantyTypeSelect();

  const equipmentSelect = document.getElementById("warrantyEquipmentSelect");
  fetchEquipment()
    .then((equipment) => {
      equipmentSelect.innerHTML = equipment
        .map((e) => `<option value="${warrantyEscapeHtml(e.id)}">${warrantyEscapeHtml(e.name)} — ${warrantyEscapeHtml(e.id)}</option>`)
        .join("");
      equipmentSelect.value = w.equipmentId;
    })
    .catch((err) => warrantyShowAlert(document.getElementById("warrantyFormAlert"), err.message));

  document.getElementById("warrantyTypeSelect").value = w.warrantyType;
  document.getElementById("warrantyProviderInput").value = w.provider;
  document.getElementById("warrantyStartDateInput").value = w.startDate;
  document.getElementById("warrantyExpiryDateInput").value = w.expiryDate;
  document.getElementById("warrantyCoverageInput").value = w.coverageDetails || "";
  document.getElementById("warrantyContactInput").value = w.contactInformation || "";

  opsShowModal(modal);
}

function wireWarrantyFormModal() {
  const modal = document.getElementById("warrantyFormModal");
  const openBtn = document.getElementById("btnAddWarranty");
  if (!modal) return;

  openBtn?.addEventListener("click", openAddWarrantyModal);

  const cancelBtn = document.getElementById("warrantyFormCancelBtn");
  const closeBtn = document.getElementById("warrantyFormCloseBtn");
  const form = document.getElementById("warrantyForm");
  const alertBox = document.getElementById("warrantyFormAlert");
  const saveBtn = document.getElementById("warrantyFormSaveBtn");

  const close = () => opsHideModal(modal);
  cancelBtn?.addEventListener("click", close);
  closeBtn?.addEventListener("click", close);

  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    warrantyClearAlert(alertBox);

    const equipmentId = document.getElementById("warrantyEquipmentSelect").value;
    const provider = document.getElementById("warrantyProviderInput").value.trim();
    const warrantyType = document.getElementById("warrantyTypeSelect").value;
    const startDate = document.getElementById("warrantyStartDateInput").value;
    const expiryDate = document.getElementById("warrantyExpiryDateInput").value;
    const coverageDetails = document.getElementById("warrantyCoverageInput").value.trim();
    const contactInformation = document.getElementById("warrantyContactInput").value.trim();

    if (!equipmentId) {
      warrantyShowAlert(alertBox, "Select equipment.");
      return;
    }
    if (!provider) {
      warrantyShowAlert(alertBox, "Enter the warranty provider.");
      return;
    }
    if (!warrantyType) {
      warrantyShowAlert(alertBox, "Select a warranty type.");
      return;
    }
    if (!startDate || !expiryDate) {
      warrantyShowAlert(alertBox, "Select a start date and an expiry date.");
      return;
    }
    if (!(new Date(expiryDate) > new Date(startDate))) {
      warrantyShowAlert(alertBox, "Expiry date must be after the start date.");
      return;
    }

    const payload = { equipmentId, provider, warrantyType, startDate, expiryDate, coverageDetails, contactInformation };

    saveBtn.disabled = true;
    saveBtn.textContent = "Saving…";
    try {
      if (warrantyEditingId) {
        await updateWarranty(warrantyEditingId, payload);
        showOpsToast("Warranty updated.");
      } else {
        await createWarranty(payload);
        showOpsToast("Warranty added.");
      }
      close();
      await loadWarranties();
    } catch (err) {
      warrantyShowAlert(alertBox, err.message);
    } finally {
      saveBtn.disabled = false;
      saveBtn.textContent = "Save Warranty";
    }
  });
}

/* ============================================================
   View Warranty Details modal (read-only)
   ============================================================ */

function openWarrantyDetailsModal(id) {
  const modal = document.getElementById("warrantyDetailsModal");
  const body = document.getElementById("warrantyDetailsBody");
  if (!modal || !body) return;

  const w = allWarranties.find((rec) => String(rec.id) === String(id));
  if (!w) return;

  const rows = [
    repairDetailRow("Warranty ID", warrantyEscapeHtml(w.warrantyCode)),
    repairDetailRow("Equipment", `${warrantyEscapeHtml(w.equipmentName)} — ${warrantyEscapeHtml(w.equipmentId)}`),
    repairDetailRow("Provider", warrantyEscapeHtml(w.provider)),
    repairDetailRow("Warranty Type", warrantyEscapeHtml(warrantyTypeLabel(w.warrantyType))),
    repairDetailRow("Start Date", warrantyEscapeHtml(w.startDate)),
    repairDetailRow("Expiry Date", warrantyEscapeHtml(w.expiryDate)),
    repairDetailRow("Coverage Details", w.coverageDetails ? warrantyEscapeHtml(w.coverageDetails) : "—"),
    repairDetailRow("Contact Information", w.contactInformation ? warrantyEscapeHtml(w.contactInformation) : "—"),
    repairDetailRow("Status", `${warrantyStatusBadge(w.status)} <span class="eq-mono eq-muted">${warrantyEscapeHtml(warrantyDaysRemainingText(w))}</span>`),
  ];

  body.innerHTML = rows.join("");
  opsShowModal(modal);
}

function wireWarrantyDetailsModal() {
  const modal = document.getElementById("warrantyDetailsModal");
  if (!modal) return;

  const close = () => opsHideModal(modal);
  document.getElementById("warrantyDetailsCloseIconBtn")?.addEventListener("click", close);
  document.getElementById("warrantyDetailsCloseBtn")?.addEventListener("click", close);
}

/* ============================================================
   Init
   ============================================================ */

function initWarrantyDashboard(session) {
  if (session.role !== "ADMIN") return;
  wireWarrantyToolbar();
  wireWarrantyFormModal();
  wireWarrantyDetailsModal();
  loadWarranties();
}