/* ============================================================
   Gym Equipment Maintenance System — Maintenance Management
   maintenance-dashboard.js
   ------------------------------------------------------------
   Renders the Maintenance Management UI and drives its
   workflows. One shared module for two dashboards (there is no
   Gym Manager role here at all):
     - Admin:      schedule maintenance, assign technician, view
                    all, reschedule/cancel
     - Technician: view assigned maintenance, start/complete work

   initMaintenanceDashboard(session) is called from admin-
   dashboard.html and technician-dashboard.html. Depends on:
     - script.js           (session/auth + apiRequest)
     - equipment-data.js    (equipment list, for the picker)
     - repair-data.js       (fetchTechnicians() + shared toast/modal
                              helpers — loaded first on both pages)
     - maintenance-data.js  (data access for this module)
   ============================================================ */

let allMaintenance = [];
let assignedMaintenance = [];
let maintRescheduleTargetId = null;
let maintCancelTargetId = null;
let maintCompleteTargetId = null;

/* ---------- small formatting helpers (self-contained) ---------- */

function maintEscapeHtml(str) {
  return String(str == null ? "" : str).replace(/[&<>"']/g, (c) => ({
    "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;",
  }[c]));
}

function maintShowAlert(container, message, type = "danger") {
  if (!container) return;
  container.innerHTML = `<div class="alert alert-tag alert-tag-${type}" role="alert">${maintEscapeHtml(message)}</div>`;
}

function maintClearAlert(container) {
  if (container) container.innerHTML = "";
}

function maintStatusAccent(status) {
  switch (status) {
    case MAINTENANCE_STATUS.SCHEDULED: return "info";
    case MAINTENANCE_STATUS.IN_PROGRESS: return "warn";
    case MAINTENANCE_STATUS.COMPLETED: return "ok";
    case MAINTENANCE_STATUS.CANCELLED: return "danger";
    default: return "info";
  }
}

function maintStatusBadge(status) {
  return `<span class="eq-badge eq-badge-${maintStatusAccent(status)}">${maintEscapeHtml(maintenanceStatusLabel(status))}</span>`;
}

/* ============================================================
   Admin — Schedule Maintenance + all maintenance records
   ============================================================ */

async function populateMaintenanceFormOptions() {
  const equipmentSelect = document.getElementById("maintEquipmentSelect");
  const typeSelect = document.getElementById("maintTypeSelect");
  const technicianSelect = document.getElementById("maintTechnicianSelect");
  if (!equipmentSelect) return;

  // The <select> markup ships with one placeholder option ("Select type"),
  // so checking `options.length === 0` here was always false and the real
  // options never got injected. Repopulate whenever there's nothing but
  // that placeholder — idempotent, so it's safe to call on every modal open.
  if (typeSelect && typeSelect.options.length <= 1) {
    typeSelect.innerHTML =
      `<option value="" selected disabled>Select type</option>` +
      Object.keys(MAINTENANCE_TYPE).map((code) => `<option value="${code}">${maintEscapeHtml(maintenanceTypeLabel(code))}</option>`).join("");
  }

  try {
    const equipment = await fetchEquipment();
    equipmentSelect.innerHTML =
      `<option value="" selected disabled>Select equipment</option>` +
      equipment.map((e) => `<option value="${maintEscapeHtml(e.id)}">${maintEscapeHtml(e.name)} — ${maintEscapeHtml(e.id)}</option>`).join("");
  } catch (err) {
    maintShowAlert(document.getElementById("maintScheduleAlert"), err.message);
  }

  try {
    const technicians = await fetchTechnicians();
    technicianSelect.innerHTML = technicians.length === 0
      ? `<option value="" selected disabled>No technicians registered</option>`
      : `<option value="" selected disabled>Select technician</option>` +
        technicians.map((t) => `<option value="${maintEscapeHtml(t.username)}">${maintEscapeHtml(t.displayLabel)}</option>`).join("");
  } catch (err) {
    maintShowAlert(document.getElementById("maintScheduleAlert"), err.message);
  }
}

/** Status-based Completion Details text, mirroring the Repair module's approach (Issue 13). */
function maintCompletionDetailsDisplay(m) {
  switch (m.status) {
    case MAINTENANCE_STATUS.SCHEDULED: return "Maintenance not started";
    case MAINTENANCE_STATUS.IN_PROGRESS: return "Maintenance in progress";
    case MAINTENANCE_STATUS.COMPLETED: return m.completionDetails ? maintEscapeHtml(m.completionDetails) : "—";
    case MAINTENANCE_STATUS.CANCELLED: return "Maintenance cancelled";
    default: return "—";
  }
}

function maintAdminNoActionText(status) {
  switch (status) {
    case MAINTENANCE_STATUS.COMPLETED: return "No action required — completed";
    case MAINTENANCE_STATUS.CANCELLED: return "No action required — cancelled";
    default: return "No action required";
  }
}

function renderAdminMaintenance() {
  const body = document.getElementById("adminMaintenanceBody");
  const empty = document.getElementById("adminMaintenanceEmpty");
  if (!body) return;

  if (allMaintenance.length === 0) {
    body.innerHTML = "";
    if (empty) empty.hidden = false;
    return;
  }
  if (empty) empty.hidden = true;

  body.innerHTML = allMaintenance.map((m) => {
    const open = m.status === MAINTENANCE_STATUS.SCHEDULED || m.status === MAINTENANCE_STATUS.IN_PROGRESS;
    const actions = [];
    if (open) {
      actions.push(`<button type="button" class="eq-row-btn" data-maint-reschedule="${m.id}">Reschedule</button>`);
      actions.push(`<button type="button" class="eq-row-btn eq-row-btn-danger" data-maint-cancel="${m.id}">Cancel</button>`);
    }
    return `
    <tr>
      <td class="eq-mono">#${m.id}</td>
      <td>${maintEscapeHtml(m.equipmentName)} <span class="eq-mono eq-muted">${maintEscapeHtml(m.equipmentId)}</span></td>
      <td>${maintEscapeHtml(maintenanceTypeLabel(m.maintenanceType))}</td>
      <td>${maintEscapeHtml(m.maintenanceDate)}</td>
      <td>${maintEscapeHtml(m.assignedTechnicianName)} — ${maintEscapeHtml(m.assignedTechnicianCode)}</td>
      <td>${maintStatusBadge(m.status)}</td>
      <td>${maintCompletionDetailsDisplay(m)}</td>
      <td class="eq-actions-cell">${actions.length ? actions.join(" ") : `<span class="eq-no-action">${maintEscapeHtml(maintAdminNoActionText(m.status))}</span>`}</td>
    </tr>`;
  }).join("");

  body.querySelectorAll("[data-maint-reschedule]").forEach((btn) => {
    btn.addEventListener("click", () => openRescheduleModal(btn.getAttribute("data-maint-reschedule")));
  });
  body.querySelectorAll("[data-maint-cancel]").forEach((btn) => {
    btn.addEventListener("click", () => openCancelModal(btn.getAttribute("data-maint-cancel")));
  });
}

async function loadAllMaintenance() {
  try {
    allMaintenance = await fetchAllMaintenance();
    renderAdminMaintenance();
  } catch (err) {
    showOpsToast(err.message);
  }
}

function openScheduleModal() {
  const modal = document.getElementById("maintenanceScheduleModal");
  if (!modal) return;
  document.getElementById("maintScheduleForm")?.reset();
  maintClearAlert(document.getElementById("maintScheduleAlert"));
  opsShowModal(modal);
}

function wireScheduleModal() {
  const modal = document.getElementById("maintenanceScheduleModal");
  const openBtn = document.getElementById("btnScheduleMaintenance");
  if (!modal) return;

  openBtn?.addEventListener("click", openScheduleModal);

  const cancelBtn = document.getElementById("maintScheduleCancelBtn");
  const closeBtn = document.getElementById("maintScheduleCloseBtn");
  const form = document.getElementById("maintScheduleForm");
  const alertBox = document.getElementById("maintScheduleAlert");
  const saveBtn = document.getElementById("maintScheduleSaveBtn");

  const close = () => opsHideModal(modal);
  cancelBtn?.addEventListener("click", close);
  closeBtn?.addEventListener("click", close);

  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    maintClearAlert(alertBox);

    const equipmentId = document.getElementById("maintEquipmentSelect").value;
    const maintenanceDate = document.getElementById("maintDateInput").value;
    const maintenanceType = document.getElementById("maintTypeSelect").value;
    const description = document.getElementById("maintDescriptionInput").value.trim();
    const technicianUsername = document.getElementById("maintTechnicianSelect").value;

    if (!equipmentId || !maintenanceDate || !maintenanceType || !technicianUsername) {
      maintShowAlert(alertBox, "Fill in equipment, date, type, and technician.");
      return;
    }

    saveBtn.disabled = true;
    saveBtn.textContent = "Scheduling…";
    try {
      await scheduleMaintenance({ equipmentId, maintenanceDate, maintenanceType, description, technicianUsername });
      showOpsToast("Maintenance scheduled.");
      close();
      await loadAllMaintenance();
    } catch (err) {
      maintShowAlert(alertBox, err.message);
    } finally {
      saveBtn.disabled = false;
      saveBtn.textContent = "Schedule Maintenance";
    }
  });
}

function openRescheduleModal(id) {
  maintRescheduleTargetId = id;
  const modal = document.getElementById("maintRescheduleModal");
  if (!modal) return;
  document.getElementById("maintRescheduleDate").value = "";
  maintClearAlert(document.getElementById("maintRescheduleAlert"));
  opsShowModal(modal);
}

function wireRescheduleModal() {
  const modal = document.getElementById("maintRescheduleModal");
  if (!modal) return;

  const cancelBtn = document.getElementById("maintRescheduleCancelBtn");
  const closeBtn = document.getElementById("maintRescheduleCloseBtn");
  const saveBtn = document.getElementById("maintRescheduleSaveBtn");
  const dateInput = document.getElementById("maintRescheduleDate");
  const alertBox = document.getElementById("maintRescheduleAlert");

  const close = () => opsHideModal(modal);
  cancelBtn?.addEventListener("click", close);
  closeBtn?.addEventListener("click", close);

  saveBtn.addEventListener("click", async () => {
    maintClearAlert(alertBox);
    if (!dateInput.value) {
      maintShowAlert(alertBox, "Select a new maintenance date.");
      return;
    }
    saveBtn.disabled = true;
    saveBtn.textContent = "Saving…";
    try {
      await rescheduleMaintenance(maintRescheduleTargetId, dateInput.value);
      showOpsToast(`Maintenance #${maintRescheduleTargetId} rescheduled.`);
      close();
      await loadAllMaintenance();
    } catch (err) {
      maintShowAlert(alertBox, err.message);
    } finally {
      saveBtn.disabled = false;
      saveBtn.textContent = "Save New Date";
    }
  });
}

function openCancelModal(id) {
  maintCancelTargetId = id;
  const modal = document.getElementById("maintCancelModal");
  if (!modal) return;
  opsShowModal(modal);
}

function wireCancelModal() {
  const modal = document.getElementById("maintCancelModal");
  if (!modal) return;

  const cancelBtn = document.getElementById("maintCancelCancelBtn");
  const confirmBtn = document.getElementById("maintCancelConfirmBtn");
  const close = () => opsHideModal(modal);

  cancelBtn?.addEventListener("click", close);
  confirmBtn?.addEventListener("click", async () => {
    try {
      await cancelMaintenance(maintCancelTargetId);
      showOpsToast(`Maintenance #${maintCancelTargetId} cancelled.`);
      close();
      await loadAllMaintenance();
    } catch (err) {
      showOpsToast(err.message);
      close();
    }
  });
}

/* ============================================================
   Technician — Assigned Maintenance: start / complete
   ============================================================ */

function renderAssignedMaintenance() {
  const body = document.getElementById("techMaintenanceBody");
  const empty = document.getElementById("techMaintenanceEmpty");
  if (!body) return;

  if (assignedMaintenance.length === 0) {
    body.innerHTML = "";
    if (empty) empty.hidden = false;
  } else {
    if (empty) empty.hidden = true;
    body.innerHTML = assignedMaintenance.map((m) => {
      const actions = [];
      if (m.status === MAINTENANCE_STATUS.SCHEDULED) {
        actions.push(`<button type="button" class="eq-row-btn" data-maint-start="${m.id}">Start Maintenance</button>`);
      }
      if (m.status === MAINTENANCE_STATUS.IN_PROGRESS) {
        actions.push(`<button type="button" class="eq-row-btn" data-maint-complete="${m.id}">Mark Completed</button>`);
      }
      return `
      <tr>
        <td>${maintEscapeHtml(m.equipmentName)} <span class="eq-mono eq-muted">${maintEscapeHtml(m.equipmentId)}</span></td>
        <td>${maintEscapeHtml(maintenanceTypeLabel(m.maintenanceType))}</td>
        <td>${maintEscapeHtml(m.maintenanceDate)}</td>
        <td>${m.description ? maintEscapeHtml(m.description) : "—"}</td>
        <td>${maintStatusBadge(m.status)}</td>
        <td class="eq-actions-cell">${actions.length ? actions.join(" ") : `<span class="eq-no-action">${maintEscapeHtml(m.status === MAINTENANCE_STATUS.COMPLETED ? "No action required — completed" : "No action required")}</span>`}</td>
      </tr>`;
    }).join("");

    body.querySelectorAll("[data-maint-start]").forEach((btn) => {
      btn.addEventListener("click", () => handleStartMaintenance(btn.getAttribute("data-maint-start")));
    });
    body.querySelectorAll("[data-maint-complete]").forEach((btn) => {
      btn.addEventListener("click", () => openMaintCompleteModal(btn.getAttribute("data-maint-complete")));
    });
  }

  updateTechnicianSummary();
}

async function loadAssignedMaintenance() {
  try {
    assignedMaintenance = await fetchAssignedMaintenance();
    renderAssignedMaintenance();
  } catch (err) {
    showOpsToast(err.message);
  }
}

async function handleStartMaintenance(id) {
  try {
    await startMaintenance(id);
    showOpsToast(`Maintenance #${id} started.`);
    await loadAssignedMaintenance();
  } catch (err) {
    showOpsToast(err.message);
  }
}

function openMaintCompleteModal(id) {
  maintCompleteTargetId = id;
  const modal = document.getElementById("maintCompleteModal");
  if (!modal) return;
  document.getElementById("maintCompletionDetails").value = "";
  maintClearAlert(document.getElementById("maintCompleteAlert"));
  opsShowModal(modal);
}

function wireMaintCompleteModal() {
  const modal = document.getElementById("maintCompleteModal");
  if (!modal) return;

  const cancelBtn = document.getElementById("maintCompleteCancelBtn");
  const closeBtn = document.getElementById("maintCompleteCloseBtn");
  const saveBtn = document.getElementById("maintCompleteSaveBtn");
  const detailsInput = document.getElementById("maintCompletionDetails");
  const alertBox = document.getElementById("maintCompleteAlert");

  const close = () => opsHideModal(modal);
  cancelBtn?.addEventListener("click", close);
  closeBtn?.addEventListener("click", close);

  saveBtn.addEventListener("click", async () => {
    maintClearAlert(alertBox);
    const details = detailsInput.value.trim();
    if (!details) {
      maintShowAlert(alertBox, "Enter completion details.");
      return;
    }
    saveBtn.disabled = true;
    saveBtn.textContent = "Saving…";
    try {
      await completeMaintenance(maintCompleteTargetId, details);
      showOpsToast(`Maintenance #${maintCompleteTargetId} marked completed.`);
      close();
      await loadAssignedMaintenance();
    } catch (err) {
      maintShowAlert(alertBox, err.message);
    } finally {
      saveBtn.disabled = false;
      saveBtn.textContent = "Mark Completed";
    }
  });
}

/* ============================================================
   Init
   ============================================================ */

function initMaintenanceDashboard(session) {
  if (session.role === "ADMIN") {
    wireScheduleModal();
    wireRescheduleModal();
    wireCancelModal();
    populateMaintenanceFormOptions();
    loadAllMaintenance();
  }

  if (session.role === "TECHNICIAN") {
    wireMaintCompleteModal();
    loadAssignedMaintenance();
  }
}
