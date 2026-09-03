/* ============================================================
   Gym Equipment Maintenance System — Dashboard & Reports
   dashboard-overview-technician.js
   ------------------------------------------------------------
   Module 8 — Technician Dashboard overview: a "Completed Jobs"
   summary pulling together finished work from both Repair
   Requests (Module 4) and Maintenance (Module 5) — each already
   scoped server-side to the logged-in technician
   (GET /api/repair-requests/assigned, GET /api/maintenance/assigned).

   Deliberately calls those data-layer functions directly rather
   than reading repair-dashboard.js's/maintenance-dashboard.js's
   assignedRepairRequests/assignedMaintenance globals — those are
   populated by async loads triggered in the same init pass this
   file's init runs in, so reading them synchronously would be a
   load-order race. An extra fetch of already-small, already-
   scoped lists is a small, safe price for correctness.
   Depends on (already loaded on technician-dashboard.html):
     - script.js, repair-data.js, maintenance-data.js, repair-dashboard.js
   ============================================================ */

function dashTechEscapeHtml(str) {
  return String(str == null ? "" : str).replace(/[&<>"']/g, (c) => ({
    "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;",
  }[c]));
}

function dashTechCard(label, value, accent) {
  return `
    <div class="eq-card eq-card-${accent}">
      <span class="eq-card-value">${dashTechEscapeHtml(value)}</span>
      <span class="eq-card-label">${dashTechEscapeHtml(label)}</span>
    </div>`;
}

async function renderTechnicianCompletedJobs() {
  const countsEl = document.getElementById("dashTechCompletedCounts");
  const body = document.getElementById("dashTechCompletedBody");
  const empty = document.getElementById("dashTechCompletedEmpty");
  if (!countsEl) return;

  try {
    const [repairs, maintenance] = await Promise.all([
      fetchAssignedRepairRequests(),
      fetchAssignedMaintenance(),
    ]);

    const completedRepairs = repairs
      .filter((r) => r.status === "COMPLETED")
      .map((r) => ({
        equipmentName: r.equipmentName,
        equipmentId: r.equipmentId,
        jobType: "Repair",
        completedDate: r.completedDate,
      }));

    const completedMaintenance = maintenance
      .filter((m) => m.status === "COMPLETED")
      .map((m) => ({
        equipmentName: m.equipmentName,
        equipmentId: m.equipmentId,
        jobType: "Maintenance",
        completedDate: m.completedDate,
      }));

    const allCompleted = [...completedRepairs, ...completedMaintenance]
      .sort((a, b) => (a.completedDate < b.completedDate ? 1 : -1)); // most recent first

    countsEl.innerHTML = [
      dashTechCard("Total Completed", allCompleted.length, "ok"),
      dashTechCard("Completed Repairs", completedRepairs.length, "info"),
      dashTechCard("Completed Maintenance", completedMaintenance.length, "info"),
    ].join("");

    if (!body) return;
    const recent = allCompleted.slice(0, 5);
    if (recent.length === 0) {
      body.innerHTML = "";
      if (empty) empty.hidden = false;
      return;
    }
    if (empty) empty.hidden = true;

    body.innerHTML = recent.map((job) => `
      <tr>
        <td>${dashTechEscapeHtml(job.equipmentName)} <span class="eq-mono eq-muted">${dashTechEscapeHtml(job.equipmentId)}</span></td>
        <td>${dashTechEscapeHtml(job.jobType)}</td>
        <td>${dashTechEscapeHtml(job.completedDate || "—")}</td>
      </tr>`).join("");
  } catch (err) {
    countsEl.innerHTML = `<p class="eq-empty-state">Couldn't load completed jobs.</p>`;
  }
}

function initDashboardOverviewTechnician(session) {
  if (session.role !== "TECHNICIAN") return;
  renderTechnicianCompletedJobs();
}