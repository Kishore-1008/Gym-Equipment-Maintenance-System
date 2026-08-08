/* ============================================================
   Gym Equipment Maintenance System — Equipment Dashboard
   equipment-data.js
   ------------------------------------------------------------
   SAMPLE / MOCK DATA ONLY.
   This file is intentionally the single place that knows where
   equipment data comes from. Every accessor below returns a
   Promise, so when the MySQL/JDBC backend from the SRS is
   ready, each function body can be swapped for a real fetch()
   call (e.g. `return (await fetch("/api/equipment")).json();`)
   without touching equipment-dashboard.js or any HTML.
   ============================================================ */

/** Canonical equipment status values — used everywhere instead of raw strings. */
const EQUIPMENT_STATUS = {
  ACTIVE: "Active",
  MAINTENANCE_DUE: "Maintenance Due",
  UNDER_MAINTENANCE: "Under Maintenance",
  OUT_OF_SERVICE: "Out of Service",
};

/** Priority levels for items needing attention. */
const EQUIPMENT_PRIORITY = {
  HIGH: "High",
  MEDIUM: "Medium",
  LOW: "Low",
};

/* ------------------------------------------------------------
   Sample fleet. Dates are ISO (YYYY-MM-DD) so they sort/parse
   predictably. addedDate drives "Recently Added"; dueDate +
   priority drive "Maintenance Attention".
   ------------------------------------------------------------ */
const MOCK_EQUIPMENT = [
  { id: "EQ001", name: "Treadmill",            category: "Cardio",       status: EQUIPMENT_STATUS.ACTIVE,             dueDate: null,         priority: null,                     addedDate: "2026-01-15" },
  { id: "EQ002", name: "Exercise Bike",         category: "Cardio",       status: EQUIPMENT_STATUS.ACTIVE,             dueDate: null,         priority: null,                     addedDate: "2026-01-20" },
  { id: "EQ003", name: "Bench Press",           category: "Strength",    status: EQUIPMENT_STATUS.MAINTENANCE_DUE,    dueDate: "2026-08-10", priority: EQUIPMENT_PRIORITY.HIGH,   addedDate: "2026-02-01" },
  { id: "EQ004", name: "Elliptical Trainer",    category: "Cardio",       status: EQUIPMENT_STATUS.ACTIVE,             dueDate: null,         priority: null,                     addedDate: "2026-02-10" },
  { id: "EQ005", name: "Rowing Machine",        category: "Cardio",       status: EQUIPMENT_STATUS.UNDER_MAINTENANCE,  dueDate: null,         priority: EQUIPMENT_PRIORITY.MEDIUM, addedDate: "2026-02-15" },
  { id: "EQ006", name: "Squat Rack",            category: "Strength",    status: EQUIPMENT_STATUS.ACTIVE,             dueDate: null,         priority: null,                     addedDate: "2026-03-01" },
  { id: "EQ007", name: "Exercise Bike",         category: "Cardio",       status: EQUIPMENT_STATUS.MAINTENANCE_DUE,    dueDate: "2026-08-12", priority: EQUIPMENT_PRIORITY.MEDIUM, addedDate: "2026-03-05" },
  { id: "EQ008", name: "Lat Pulldown Machine",  category: "Strength",    status: EQUIPMENT_STATUS.ACTIVE,             dueDate: null,         priority: null,                     addedDate: "2026-03-10" },
  { id: "EQ009", name: "Leg Press Machine",     category: "Strength",    status: EQUIPMENT_STATUS.OUT_OF_SERVICE,     dueDate: "2026-08-05", priority: EQUIPMENT_PRIORITY.HIGH,   addedDate: "2026-03-15" },
  { id: "EQ010", name: "Cable Crossover",       category: "Strength",    status: EQUIPMENT_STATUS.ACTIVE,             dueDate: null,         priority: null,                     addedDate: "2026-03-20" },
  { id: "EQ011", name: "Kettlebell Set",        category: "Free Weights", status: EQUIPMENT_STATUS.ACTIVE,            dueDate: null,         priority: null,                     addedDate: "2026-04-01" },
  { id: "EQ012", name: "Dumbbell Rack",         category: "Free Weights", status: EQUIPMENT_STATUS.ACTIVE,            dueDate: null,         priority: null,                     addedDate: "2026-04-05" },
  { id: "EQ013", name: "Smith Machine",         category: "Strength",    status: EQUIPMENT_STATUS.UNDER_MAINTENANCE,  dueDate: null,         priority: EQUIPMENT_PRIORITY.MEDIUM, addedDate: "2026-04-10" },
  { id: "EQ014", name: "Stair Climber",         category: "Cardio",       status: EQUIPMENT_STATUS.MAINTENANCE_DUE,    dueDate: "2026-08-09", priority: EQUIPMENT_PRIORITY.HIGH,   addedDate: "2026-04-15" },
  { id: "EQ015", name: "Spin Bike",             category: "Cardio",       status: EQUIPMENT_STATUS.ACTIVE,             dueDate: null,         priority: null,                     addedDate: "2026-04-20" },
  { id: "EQ016", name: "Battle Ropes",          category: "Functional",  status: EQUIPMENT_STATUS.ACTIVE,             dueDate: null,         priority: null,                     addedDate: "2026-05-01" },
  { id: "EQ017", name: "TRX Trainer",           category: "Functional",  status: EQUIPMENT_STATUS.ACTIVE,             dueDate: null,         priority: null,                     addedDate: "2026-05-05" },
  { id: "EQ018", name: "Medicine Ball Set",     category: "Functional",  status: EQUIPMENT_STATUS.ACTIVE,             dueDate: null,         priority: null,                     addedDate: "2026-05-10" },
  { id: "EQ019", name: "Leg Curl Machine",      category: "Strength",    status: EQUIPMENT_STATUS.OUT_OF_SERVICE,     dueDate: "2026-08-03", priority: EQUIPMENT_PRIORITY.HIGH,   addedDate: "2026-05-15" },
  { id: "EQ020", name: "Chest Press Machine",   category: "Strength",    status: EQUIPMENT_STATUS.ACTIVE,             dueDate: null,         priority: null,                     addedDate: "2026-05-20" },
  { id: "EQ021", name: "Foam Roller Set",       category: "Recovery",    status: EQUIPMENT_STATUS.ACTIVE,             dueDate: null,         priority: null,                     addedDate: "2026-06-01" },
  { id: "EQ022", name: "Massage Gun Station",   category: "Recovery",    status: EQUIPMENT_STATUS.MAINTENANCE_DUE,    dueDate: "2026-08-15", priority: EQUIPMENT_PRIORITY.LOW,    addedDate: "2026-06-05" },
  { id: "EQ023", name: "Assault Bike",          category: "Cardio",       status: EQUIPMENT_STATUS.ACTIVE,             dueDate: null,         priority: null,                     addedDate: "2026-06-10" },
  { id: "EQ024", name: "Power Rack",            category: "Strength",    status: EQUIPMENT_STATUS.ACTIVE,             dueDate: null,         priority: null,                     addedDate: "2026-06-15" },
  { id: "EQ025", name: "Vibration Plate",       category: "Functional",  status: EQUIPMENT_STATUS.UNDER_MAINTENANCE,  dueDate: null,         priority: EQUIPMENT_PRIORITY.LOW,    addedDate: "2026-06-20" },
];

/**
 * Fetch the full equipment list.
 * TODO(backend): replace the body with a real API call, e.g.
 *   const res = await fetch("/api/equipment");
 *   return res.json();
 */
async function fetchEquipment() {
  const copy = typeof structuredClone === "function"
    ? structuredClone(MOCK_EQUIPMENT)
    : JSON.parse(JSON.stringify(MOCK_EQUIPMENT));
  return copy;
}
