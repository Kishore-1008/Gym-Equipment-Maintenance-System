/* ============================================================
   Gym Equipment Maintenance System — Repair Request Management
   repair-dashboard.js
   ------------------------------------------------------------
   Renders the Repair Request Management UI and drives its
   workflows. One shared module for three dashboards:
     - Gym Manager: report a problem, view/track own requests
     - Admin:       view all, approve/reject, assign technician,
                     change equipment status
     - Technician:  view assigned repairs, start/complete work

   initRepairDashboard(session) is called from gym-manager-
   dashboard.html, admin-dashboard.html, and technician-
   dashboard.html; it reads session.role to decide which parts
   of the page to wire up (each dashboard only includes the DOM
   for its own role, so the other branches are no-ops).
   Depends on:
     - script.js        (session/auth + apiRequest)
     - equipment-data.js (equipment list, for the picker + status form)
     - repair-data.js    (data access for this module)
   ============================================================ */

let myRepairRequests = [];
let allRepairRequests = [];
let assignedRepairRequests = [];
let repairAssignTargetId = null;
let repairRejectTargetId = null;
let repairCompleteTargetId = null;

/* ---------- small formatting helpers (self-contained — this file is
   loaded on pages that may not load equipment-dashboard.js) ---------- */

function repairEscapeHtml(str) {
  return String(str == null ? "" : str).replace(/[&<>"']/g, (c) => ({
    "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;",
  }[c]));
}

function repairShowAlert(container, message, type = "danger") {
  if (!container) return;
  container.innerHTML = `<div class="alert alert-tag alert-tag-${type}" role="alert">${repairEscapeHtml(message)}</div>`;
}

function repairClearAlert(container) {
  if (container) container.innerHTML = "";
}

function repairStatusAccent(status) {
  switch (status) {
    case REPAIR_STATUS.PENDING: return "warn";
    case REPAIR_STATUS.APPROVED: return "info";
    case REPAIR_STATUS.REJECTED: return "danger";
    case REPAIR_STATUS.ASSIGNED: return "info";
    case REPAIR_STATUS.IN_PROGRESS: return "warn";
    case REPAIR_STATUS.COMPLETED: return "ok";
    default: return "info";
  }
}

function repairStatusBadge(status) {
  return `<span class="eq-badge eq-badge-${repairStatusAccent(status)}">${repairEscapeHtml(repairStatusLabel(status))}</span>`;
}

/* ---------- shared toast (id="opsToast", present on all three dashboards) ---------- */

function showOpsToast(message) {
  const el = document.getElementById("opsToast");
  if (!el) return;
  el.textContent = message;
  el.classList.add("eq-toast-visible");
  window.clearTimeout(showOpsToast._t);
  showOpsToast._t = window.setTimeout(() => el.classList.remove("eq-toast-visible"), 3200);
}

/* ---------- shared modal show/hide (id="opsModalBackdrop", shared across
   repair + maintenance modals on whichever dashboard includes them) ---------- */

function opsShowModal(modal) {
  document.getElementById("opsModalBackdrop")?.removeAttribute("hidden");
  modal.hidden = false;
}

const OPS_MODAL_IDS = [
  "repairRejectModal", "repairAssignModal", "repairDetailsModal",
  "maintenanceScheduleModal", "maintRescheduleModal", "maintCancelModal",
  "repairCompleteModal", "maintCompleteModal", "repairHistoryDetailsModal",
  "warrantyFormModal", "warrantyDetailsModal",
];

function opsHideModal(modal) {
  modal.hidden = true;
  const stillOpen = OPS_MODAL_IDS.some((id) => {
    const el = document.getElementById(id);
    return el && !el.hasAttribute("hidden");
  });
  if (!stillOpen) document.getElementById("opsModalBackdrop")?.setAttribute("hidden", "");
}

/* ============================================================
   Gym Manager — Report a Problem + My Repair Requests
   ============================================================ */

async function populateRepairEquipmentSelect() {
  const select = document.getElementById("repairEquipmentSelect");
  if (!select) return;
  try {
    const equipment = await fetchEquipment();
    select.innerHTML =
      `<option value="" selected disabled>Select equipment</option>` +
      equipment.map((e) => `<option value="${repairEscapeHtml(e.id)}">${repairEscapeHtml(e.name)} — ${repairEscapeHtml(e.id)}</option>`).join("");
  } catch (err) {
    repairShowAlert(document.getElementById("reportProblemAlert"), err.message);
  }
}

/** Status-based Rejection Reason text (Issue 6) — the actual reason only ever shows for REJECTED. */
function repairRejectionReasonDisplay(r) {
  switch (r.status) {
    case REPAIR_STATUS.PENDING: return "Not reviewed yet";
    case REPAIR_STATUS.APPROVED: return "Approved";
    case REPAIR_STATUS.ASSIGNED: return "Not rejected";
    case REPAIR_STATUS.IN_PROGRESS: return "Repair in progress";
    case REPAIR_STATUS.COMPLETED: return "Repair completed";
    case REPAIR_STATUS.REJECTED: return r.rejectionReason ? repairEscapeHtml(r.rejectionReason) : "—";
    default: return "—";
  }
}

/** Status-based Completion Details text (Issue 7) — the actual details only ever show for COMPLETED. */
function repairCompletionDetailsDisplay(r) {
  switch (r.status) {
    case REPAIR_STATUS.PENDING: return "Repair not started";
    case REPAIR_STATUS.APPROVED: return "Awaiting technician assignment";
    case REPAIR_STATUS.ASSIGNED: return "Awaiting repair to start";
    case REPAIR_STATUS.IN_PROGRESS: return "Repair in progress";
    case REPAIR_STATUS.COMPLETED: return r.completionDetails ? repairEscapeHtml(r.completionDetails) : "—";
    case REPAIR_STATUS.REJECTED: return "Repair request rejected";
    default: return "—";
  }
}

function renderMyRepairRequests() {
  const body = document.getElementById("myRepairRequestsBody");
  const empty = document.getElementById("myRepairRequestsEmpty");
  if (!body) return;

  if (myRepairRequests.length === 0) {
    body.innerHTML = "";
    if (empty) empty.hidden = false;
    return;
  }
  if (empty) empty.hidden = true;

  body.innerHTML = myRepairRequests.map((r) => `
    <tr>
      <td class="eq-mono">#${r.id}</td>
      <td>${repairEscapeHtml(r.equipmentName)} <span class="eq-mono eq-muted">${repairEscapeHtml(r.equipmentId)}</span></td>
      <td>${repairEscapeHtml(r.problemDescription)}</td>
      <td>${repairStatusBadge(r.status)}</td>
      <td>${repairEscapeHtml(r.submittedDate || "—")}</td>
      <td>${r.assignedTechnicianName ? `${repairEscapeHtml(r.assignedTechnicianName)} — ${repairEscapeHtml(r.assignedTechnicianCode)}` : "—"}</td>
      <td>${repairRejectionReasonDisplay(r)}</td>
      <td>${repairCompletionDetailsDisplay(r)}</td>
    </tr>
  `).join("");
}

async function loadMyRepairRequests() {
  try {
    myRepairRequests = await fetchMyRepairRequests();
    renderMyRepairRequests();
  } catch (err) {
    showOpsToast(err.message);
  }
}

function wireReportProblemForm() {
  const form = document.getElementById("reportProblemForm");
  if (!form) return;

  const alertBox = document.getElementById("reportProblemAlert");
  const equipmentSelect = document.getElementById("repairEquipmentSelect");
  const descriptionInput = document.getElementById("repairProblemDescription");
  const submitBtn = document.getElementById("repairSubmitBtn");

  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    repairClearAlert(alertBox);

    const equipmentId = equipmentSelect.value;
    const problemDescription = descriptionInput.value.trim();

    if (!equipmentId) {
      repairShowAlert(alertBox, "Select equipment.");
      return;
    }
    if (!problemDescription) {
      repairShowAlert(alertBox, "Enter a problem description.");
      return;
    }

    submitBtn.disabled = true;
    submitBtn.textContent = "Submitting…";

    try {
      await reportProblem({ equipmentId, problemDescription });
      form.reset();
      showOpsToast("Repair request submitted.");
      await loadMyRepairRequests();
    } catch (err) {
      repairShowAlert(alertBox, err.message);
    } finally {
      submitBtn.disabled = false;
      submitBtn.textContent = "Submit Repair Request";
    }
  });
}

/* ============================================================
   Admin — All Repair Requests: approve / reject / assign
   ============================================================ */

function renderAdminRepairRequests() {
  const body = document.getElementById("adminRepairBody");
  const empty = document.getElementById("adminRepairEmpty");
  if (!body) return;

  const filter = document.getElementById("repairStatusFilter")?.value || "";
  const rows = filter ? allRepairRequests.filter((r) => r.status === filter) : allRepairRequests;

  if (rows.length === 0) {
    body.innerHTML = "";
    if (empty) empty.hidden = false;
    return;
  }
  if (empty) empty.hidden = true;

  body.innerHTML = rows.map((r) => {
    const actions = [];
    if (r.status === REPAIR_STATUS.PENDING) {
      actions.push(`<button type="button" class="eq-row-btn" data-repair-approve="${r.id}">Approve</button>`);
      actions.push(`<button type="button" class="eq-row-btn eq-row-btn-danger" data-repair-reject="${r.id}">Reject</button>`);
    } else if (r.status === REPAIR_STATUS.APPROVED) {
      actions.push(`<button type="button" class="eq-row-btn" data-repair-assign="${r.id}">Assign Technician</button>`);
    } else {
      // ASSIGNED / IN_PROGRESS / COMPLETED / REJECTED (and any future
      // status) always get a real action — the Actions column must never
      // be empty or show a bare "no action" placeholder.
      actions.push(`<button type="button" class="eq-row-btn" data-repair-details="${r.id}">View Details</button>`);
    }
    return `
    <tr>
      <td class="eq-mono">#${r.id}</td>
      <td>${repairEscapeHtml(r.equipmentName)} <span class="eq-mono eq-muted">${repairEscapeHtml(r.equipmentId)}</span></td>
      <td>${repairEscapeHtml(r.problemDescription)}</td>
      <td>${repairStatusBadge(r.status)}</td>
      <td>${repairEscapeHtml(r.submittedByFullName)}</td>
      <td>${repairEscapeHtml(r.submittedDate || "—")}</td>
      <td>${r.assignedTechnicianName ? `${repairEscapeHtml(r.assignedTechnicianName)} — ${repairEscapeHtml(r.assignedTechnicianCode)}` : "—"}</td>
      <td class="eq-actions-cell">${actions.join(" ")}</td>
    </tr>`;
  }).join("");

  body.querySelectorAll("[data-repair-approve]").forEach((btn) => {
    btn.addEventListener("click", () => handleApprove(btn.getAttribute("data-repair-approve")));
  });
  body.querySelectorAll("[data-repair-reject]").forEach((btn) => {
    btn.addEventListener("click", () => openRejectModal(btn.getAttribute("data-repair-reject")));
  });
  body.querySelectorAll("[data-repair-assign]").forEach((btn) => {
    btn.addEventListener("click", () => openAssignModal(btn.getAttribute("data-repair-assign")));
  });
  body.querySelectorAll("[data-repair-details]").forEach((btn) => {
    btn.addEventListener("click", () => openRepairDetailsModal(btn.getAttribute("data-repair-details")));
  });
}

async function loadAllRepairRequests() {
  try {
    allRepairRequests = await fetchAllRepairRequests();
    renderAdminRepairRequests();
  } catch (err) {
    showOpsToast(err.message);
  }
}

async function handleApprove(id) {
  try {
    await approveRepairRequest(id);
    showOpsToast(`Request #${id} approved.`);
    await loadAllRepairRequests();
  } catch (err) {
    showOpsToast(err.message);
  }
}

function openRejectModal(id) {
  repairRejectTargetId = id;
  const modal = document.getElementById("repairRejectModal");
  if (!modal) return;
  document.getElementById("repairRejectReason").value = "";
  repairClearAlert(document.getElementById("repairRejectAlert"));
  opsShowModal(modal);
}

function wireRejectModal() {
  const modal = document.getElementById("repairRejectModal");
  if (!modal) return;

  const cancelBtn = document.getElementById("repairRejectCancelBtn");
  const closeBtn = document.getElementById("repairRejectCloseBtn");
  const saveBtn = document.getElementById("repairRejectSaveBtn");
  const reasonInput = document.getElementById("repairRejectReason");
  const alertBox = document.getElementById("repairRejectAlert");

  const close = () => opsHideModal(modal);
  cancelBtn?.addEventListener("click", close);
  closeBtn?.addEventListener("click", close);

  saveBtn.addEventListener("click", async () => {
    repairClearAlert(alertBox);
    const reason = reasonInput.value.trim();
    if (!reason) {
      repairShowAlert(alertBox, "A rejection reason is required.");
      return;
    }
    saveBtn.disabled = true;
    saveBtn.textContent = "Rejecting…";
    try {
      await rejectRepairRequest(repairRejectTargetId, reason);
      showOpsToast(`Request #${repairRejectTargetId} rejected.`);
      close();
      await loadAllRepairRequests();
    } catch (err) {
      repairShowAlert(alertBox, err.message);
    } finally {
      saveBtn.disabled = false;
      saveBtn.textContent = "Reject Request";
    }
  });
}

async function openAssignModal(id) {
  repairAssignTargetId = id;
  const modal = document.getElementById("repairAssignModal");
  if (!modal) return;

  const select = document.getElementById("repairAssignTechnician");
  repairClearAlert(document.getElementById("repairAssignAlert"));
  select.innerHTML = `<option value="" selected disabled>Loading technicians…</option>`;
  opsShowModal(modal);

  try {
    const technicians = await fetchTechnicians();
    if (technicians.length === 0) {
      select.innerHTML = `<option value="" selected disabled>No technicians registered</option>`;
    } else {
      select.innerHTML =
        `<option value="" selected disabled>Select technician</option>` +
        technicians.map((t) => `<option value="${repairEscapeHtml(t.username)}">${repairEscapeHtml(t.displayLabel)}</option>`).join("");
    }
  } catch (err) {
    repairShowAlert(document.getElementById("repairAssignAlert"), err.message);
  }
}

function wireAssignModal() {
  const modal = document.getElementById("repairAssignModal");
  if (!modal) return;

  const cancelBtn = document.getElementById("repairAssignCancelBtn");
  const closeBtn = document.getElementById("repairAssignCloseBtn");
  const saveBtn = document.getElementById("repairAssignSaveBtn");
  const select = document.getElementById("repairAssignTechnician");
  const alertBox = document.getElementById("repairAssignAlert");

  const close = () => opsHideModal(modal);
  cancelBtn?.addEventListener("click", close);
  closeBtn?.addEventListener("click", close);

  saveBtn.addEventListener("click", async () => {
    repairClearAlert(alertBox);
    if (!select.value) {
      repairShowAlert(alertBox, "Select a technician.");
      return;
    }
    saveBtn.disabled = true;
    saveBtn.textContent = "Assigning…";
    try {
      await assignRepairTechnician(repairAssignTargetId, select.value);
      showOpsToast(`Technician assigned to request #${repairAssignTargetId}.`);
      close();
      await loadAllRepairRequests();
    } catch (err) {
      repairShowAlert(alertBox, err.message);
    } finally {
      saveBtn.disabled = false;
      saveBtn.textContent = "Assign Technician";
    }
  });
}

/* ---------- Admin — View Details (read-only, for any status the row's
   Actions column doesn't have a workflow action for) ---------- */

function repairDetailRow(label, value) {
  return `
    <div class="eq-detail-row">
      <span class="eq-detail-label">${repairEscapeHtml(label)}</span>
      <span class="eq-detail-value">${value}</span>
    </div>`;
}

function openRepairDetailsModal(id) {
  const modal = document.getElementById("repairDetailsModal");
  const body = document.getElementById("repairDetailsBody");
  if (!modal || !body) return;

  const r = allRepairRequests.find((req) => String(req.id) === String(id));
  if (!r) return;

  const rows = [
    repairDetailRow("Request ID", `#${r.id}`),
    repairDetailRow("Equipment", `${repairEscapeHtml(r.equipmentName)} — ${repairEscapeHtml(r.equipmentId)}`),
    repairDetailRow("Problem Description", repairEscapeHtml(r.problemDescription)),
    repairDetailRow("Status", repairStatusBadge(r.status)),
    repairDetailRow("Reported By", repairEscapeHtml(r.submittedByFullName)),
    repairDetailRow("Submitted", repairEscapeHtml(r.submittedDate || "—")),
  ];

  if (r.assignedTechnicianName) {
    rows.push(repairDetailRow("Assigned Technician", `${repairEscapeHtml(r.assignedTechnicianName)} — ${repairEscapeHtml(r.assignedTechnicianCode)}`));
  }
  if (r.assignedDate) {
    rows.push(repairDetailRow("Assigned Date", repairEscapeHtml(r.assignedDate)));
  }
  if (r.startedDate) {
    rows.push(repairDetailRow("Started", repairEscapeHtml(r.startedDate)));
  }
  if (r.status === REPAIR_STATUS.REJECTED) {
    rows.push(repairDetailRow("Rejection Reason", repairEscapeHtml(r.rejectionReason || "—")));
  }
  if (r.status === REPAIR_STATUS.COMPLETED) {
    rows.push(repairDetailRow("Completion Details", repairEscapeHtml(r.completionDetails || "—")));
    rows.push(repairDetailRow("Completed", repairEscapeHtml(r.completedDate || "—")));
  }

  body.innerHTML = rows.join("");
  opsShowModal(modal);
}

function wireRepairDetailsModal() {
  const modal = document.getElementById("repairDetailsModal");
  if (!modal) return;

  const close = () => opsHideModal(modal);
  document.getElementById("repairDetailsCloseIconBtn")?.addEventListener("click", close);
  document.getElementById("repairDetailsCloseBtn")?.addEventListener("click", close);
}

/* ---------- Admin — Change Equipment Status (Module 4, repair workflow) ---------- */

async function populateStatusChangeEquipmentSelect() {
  const select = document.getElementById("statusChangeEquipment");
  if (!select) return;
  try {
    const equipment = await fetchEquipment();
    select.innerHTML =
      `<option value="" selected disabled>Select equipment</option>` +
      equipment.map((e) => `<option value="${repairEscapeHtml(e.id)}">${repairEscapeHtml(e.name)} — ${repairEscapeHtml(e.id)}</option>`).join("");
  } catch (err) {
    repairShowAlert(document.getElementById("statusChangeAlert"), err.message);
  }
}

function wireEquipmentStatusForm() {
  const form = document.getElementById("equipmentStatusForm");
  if (!form) return;

  const equipmentSelect = document.getElementById("statusChangeEquipment");
  const statusSelect = document.getElementById("statusChangeStatus");
  const alertBox = document.getElementById("statusChangeAlert");
  const btn = document.getElementById("statusChangeBtn");

  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    repairClearAlert(alertBox);

    if (!equipmentSelect.value || !statusSelect.value) {
      repairShowAlert(alertBox, "Select equipment and a status.");
      return;
    }

    btn.disabled = true;
    btn.textContent = "Updating…";
    try {
      await apiRequest(`/equipment/${encodeURIComponent(equipmentSelect.value)}/status`, {
        method: "PATCH",
        body: JSON.stringify({ status: statusSelect.value }),
      });
      showOpsToast(`${equipmentSelect.value} status updated.`);
      form.reset();
    } catch (err) {
      repairShowAlert(alertBox, err.message);
    } finally {
      btn.disabled = false;
      btn.textContent = "Update Status";
    }
  });
}

/* ============================================================
   Technician — Assigned Repairs: start / complete
   ============================================================ */

function renderAssignedRepairRequests() {
  const body = document.getElementById("techRepairBody");
  const empty = document.getElementById("techRepairEmpty");
  if (!body) return;

  if (assignedRepairRequests.length === 0) {
    body.innerHTML = "";
    if (empty) empty.hidden = false;
  } else {
    if (empty) empty.hidden = true;
    body.innerHTML = assignedRepairRequests.map((r) => {
      const actions = [];
      if (r.status === REPAIR_STATUS.ASSIGNED) {
        actions.push(`<button type="button" class="eq-row-btn" data-repair-start="${r.id}">Start Repair</button>`);
      }
      if (r.status === REPAIR_STATUS.IN_PROGRESS) {
        actions.push(`<button type="button" class="eq-row-btn" data-repair-complete="${r.id}">Mark Completed</button>`);
      }
      return `
      <tr>
        <td>${repairEscapeHtml(r.equipmentName)} <span class="eq-mono eq-muted">${repairEscapeHtml(r.equipmentId)}</span></td>
        <td>${repairEscapeHtml(r.problemDescription)}</td>
        <td>${repairStatusBadge(r.status)}</td>
        <td>${repairEscapeHtml(r.assignedDate || "—")}</td>
        <td class="eq-actions-cell">${actions.length ? actions.join(" ") : `<span class="eq-no-action">${repairEscapeHtml("No action required")}</span>`}</td>
      </tr>`;
    }).join("");

    body.querySelectorAll("[data-repair-start]").forEach((btn) => {
      btn.addEventListener("click", () => handleStartRepair(btn.getAttribute("data-repair-start")));
    });
    body.querySelectorAll("[data-repair-complete]").forEach((btn) => {
      btn.addEventListener("click", () => openRepairCompleteModal(btn.getAttribute("data-repair-complete")));
    });
  }

  updateTechnicianSummary();
}

async function loadAssignedRepairRequests() {
  try {
    assignedRepairRequests = await fetchAssignedRepairRequests();
    renderAssignedRepairRequests();
  } catch (err) {
    showOpsToast(err.message);
  }
}

async function handleStartRepair(id) {
  try {
    await startRepairRequest(id);
    showOpsToast(`Repair #${id} started.`);
    await loadAssignedRepairRequests();
  } catch (err) {
    showOpsToast(err.message);
  }
}

function openRepairCompleteModal(id) {
  repairCompleteTargetId = id;
  const modal = document.getElementById("repairCompleteModal");
  if (!modal) return;
  document.getElementById("repairCompletionDetails").value = "";
  document.getElementById("repairPartsUsed").value = "";
  document.getElementById("repairCost").value = "";
  document.getElementById("repairCompletionNotes").value = "";
  repairClearAlert(document.getElementById("repairCompleteAlert"));
  opsShowModal(modal);
}

function wireRepairCompleteModal() {
  const modal = document.getElementById("repairCompleteModal");
  if (!modal) return;

  const cancelBtn = document.getElementById("repairCompleteCancelBtn");
  const closeBtn = document.getElementById("repairCompleteCloseBtn");
  const saveBtn = document.getElementById("repairCompleteSaveBtn");
  const detailsInput = document.getElementById("repairCompletionDetails");
  const partsInput = document.getElementById("repairPartsUsed");
  const costInput = document.getElementById("repairCost");
  const notesInput = document.getElementById("repairCompletionNotes");
  const alertBox = document.getElementById("repairCompleteAlert");

  const close = () => opsHideModal(modal);
  cancelBtn?.addEventListener("click", close);
  closeBtn?.addEventListener("click", close);

  saveBtn.addEventListener("click", async () => {
    repairClearAlert(alertBox);

    const repairDetails = detailsInput.value.trim();
    const partsUsed = partsInput.value.trim();
    const costRaw = costInput.value.trim();
    const completionNotes = notesInput.value.trim();

    if (!repairDetails) {
      repairShowAlert(alertBox, "Enter repair details / work performed.");
      return;
    }
    if (!partsUsed) {
      repairShowAlert(alertBox, 'Enter parts used, or "No parts used" if none.');
      return;
    }
    if (costRaw === "" || Number.isNaN(Number(costRaw)) || Number(costRaw) < 0) {
      repairShowAlert(alertBox, "Enter a repair cost of 0 or more.");
      return;
    }

    saveBtn.disabled = true;
    saveBtn.textContent = "Saving…";
    try {
      await completeRepairRequest(repairCompleteTargetId, {
        repairDetails,
        partsUsed,
        repairCost: Number(costRaw),
        completionNotes: completionNotes || null,
      });
      showOpsToast(`Repair #${repairCompleteTargetId} marked completed.`);
      close();
      await loadAssignedRepairRequests();
    } catch (err) {
      repairShowAlert(alertBox, err.message);
    } finally {
      saveBtn.disabled = false;
      saveBtn.textContent = "Mark Completed";
    }
  });
}

/* ============================================================
   Init
   ============================================================ */

function initRepairDashboard(session) {
  if (session.role === "GYM_MANAGER") {
    populateRepairEquipmentSelect();
    wireReportProblemForm();
    loadMyRepairRequests();
  }

  if (session.role === "ADMIN") {
    const filter = document.getElementById("repairStatusFilter");
    filter?.addEventListener("change", renderAdminRepairRequests);
    wireRejectModal();
    wireAssignModal();
    wireRepairDetailsModal();
    wireEquipmentStatusForm();
    populateStatusChangeEquipmentSelect();
    loadAllRepairRequests();
  }

  if (session.role === "TECHNICIAN") {
    wireRepairCompleteModal();
    loadAssignedRepairRequests();
  }
}