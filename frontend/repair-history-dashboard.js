/* ============================================================
   Gym Equipment Maintenance System — Repair History
   repair-history-dashboard.js
   ------------------------------------------------------------
   Admin-only, read-only view of Module 6. Loads once and renders
   the Repair History table plus a read-only View Details modal —
   same "load list with full detail, look up client-side for
   details" pattern already used for the Repair Requests table.
   Depends on:
     - script.js               (session/auth + apiRequest)
     - repair-dashboard.js      (shared showOpsToast/opsShowModal/
                                  opsHideModal/repairEscapeHtml
                                  helpers — loaded first)
     - repair-history-data.js   (data access for this module)
   ============================================================ */

let repairHistoryRecords = [];

function repairHistoryCostDisplay(cost) {
  const n = Number(cost);
  return Number.isFinite(n) ? n.toFixed(2) : "0.00";
}

function renderRepairHistoryTable() {
  const body = document.getElementById("repairHistoryBody");
  const empty = document.getElementById("repairHistoryEmpty");
  if (!body) return;

  if (repairHistoryRecords.length === 0) {
    body.innerHTML = "";
    if (empty) empty.hidden = false;
    return;
  }
  if (empty) empty.hidden = true;

  body.innerHTML = repairHistoryRecords.map((h) => `
    <tr>
      <td class="eq-mono">#${h.id}</td>
      <td class="eq-mono">#${h.repairRequestId}</td>
      <td>${repairEscapeHtml(h.equipmentName)} <span class="eq-mono eq-muted">${repairEscapeHtml(h.equipmentId)}</span></td>
      <td>${repairEscapeHtml(h.technicianName)} — ${repairEscapeHtml(h.technicianCode)}</td>
      <td>${repairEscapeHtml(h.completedDate || "—")}</td>
      <td>${repairHistoryCostDisplay(h.repairCost)}</td>
      <td class="eq-actions-cell"><button type="button" class="eq-row-btn" data-history-details="${h.id}">View Details</button></td>
    </tr>
  `).join("");

  body.querySelectorAll("[data-history-details]").forEach((btn) => {
    btn.addEventListener("click", () => openRepairHistoryDetailsModal(btn.getAttribute("data-history-details")));
  });
}

async function loadRepairHistory() {
  try {
    repairHistoryRecords = await fetchRepairHistory();
    renderRepairHistoryTable();
  } catch (err) {
    showOpsToast(err.message);
  }
}

function openRepairHistoryDetailsModal(id) {
  const modal = document.getElementById("repairHistoryDetailsModal");
  const body = document.getElementById("repairHistoryDetailsBody");
  if (!modal || !body) return;

  const h = repairHistoryRecords.find((rec) => String(rec.id) === String(id));
  if (!h) return;

  const rows = [
    repairDetailRow("Repair Request ID", `#${h.repairRequestId}`),
    repairDetailRow("Equipment", `${repairEscapeHtml(h.equipmentName)} — ${repairEscapeHtml(h.equipmentId)}`),
    repairDetailRow("Technician", `${repairEscapeHtml(h.technicianName)} — ${repairEscapeHtml(h.technicianCode)}`),
    repairDetailRow("Original Problem", repairEscapeHtml(h.problemDescription)),
    repairDetailRow("Repair Details", repairEscapeHtml(h.repairDetails)),
    repairDetailRow("Parts Used", repairEscapeHtml(h.partsUsed)),
    repairDetailRow("Repair Cost", repairHistoryCostDisplay(h.repairCost)),
    repairDetailRow("Completion Notes", h.completionNotes ? repairEscapeHtml(h.completionNotes) : "—"),
    repairDetailRow("Completed", repairEscapeHtml(h.completedDate || "—")),
  ];

  body.innerHTML = rows.join("");
  opsShowModal(modal);
}

function wireRepairHistoryDetailsModal() {
  const modal = document.getElementById("repairHistoryDetailsModal");
  if (!modal) return;

  const close = () => opsHideModal(modal);
  document.getElementById("repairHistoryDetailsCloseIconBtn")?.addEventListener("click", close);
  document.getElementById("repairHistoryDetailsCloseBtn")?.addEventListener("click", close);
}

function initRepairHistoryDashboard(session) {
  if (session.role !== "ADMIN") return;
  wireRepairHistoryDetailsModal();
  loadRepairHistory();
}