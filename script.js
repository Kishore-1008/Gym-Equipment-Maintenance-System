/* ============================================================
   Gym Equipment Maintenance System — Authentication Module
   script.js
   ------------------------------------------------------------
   Handles:
     - localStorage-backed user "database" (demo persistence)
     - default admin account seeding
     - registration form validation + duplicate checks
     - login form validation + role-based redirect
     - session handling + dashboard access guard / logout
   ============================================================ */

/* ---------- Storage keys ---------- */
const USERS_KEY   = "gym_ams_users";
const SESSION_KEY = "gym_ams_session";

/* Where each role lands after a successful login */
const ROLE_DASHBOARDS = {
  Admin: "admin-dashboard.html",
  Technician: "technician-dashboard.html",
  Staff: "staff-dashboard.html",
};

/* ============================================================
   Storage helpers
   ============================================================ */

/** Read the user list from localStorage (empty array if none saved yet). */
function getUsers() {
  try {
    const raw = localStorage.getItem(USERS_KEY);
    return raw ? JSON.parse(raw) : [];
  } catch (err) {
    console.error("Could not read stored users:", err);
    return [];
  }
}

/** Persist the full user list back to localStorage. */
function saveUsers(users) {
  localStorage.setItem(USERS_KEY, JSON.stringify(users));
}

/** Case-insensitive lookup by username — used for login and for duplicate checks. */
function findUser(username) {
  const needle = username.trim().toLowerCase();
  return getUsers().find((u) => u.username.toLowerCase() === needle);
}

/* ============================================================
   Password hashing
   ------------------------------------------------------------
   This is a demo-grade SHA-256 hash using the browser's built-in
   SubtleCrypto API, so at minimum plaintext passwords are never
   written to localStorage. It is NOT a substitute for a proper
   server-side hash (bcrypt/argon2 + salt) in a real deployment —
   the SRS's "Encrypted passwords" requirement should ultimately
   be enforced by the backend, once one exists.
   ============================================================ */
async function hashPassword(password) {
  const data = new TextEncoder().encode(password);
  const digest = await crypto.subtle.digest("SHA-256", data);
  return Array.from(new Uint8Array(digest))
    .map((b) => b.toString(16).padStart(2, "0"))
    .join("");
}

/* ============================================================
   Seed a default admin account on first run
   ============================================================ */
async function seedDefaultAdmin() {
  const users = getUsers();
  if (users.length > 0) return;

  const passwordHash = await hashPassword("admin123");
  users.push({
    fullName: "System Administrator",
    username: "admin",
    password: passwordHash,
    role: "Admin",
  });
  saveUsers(users);
}

/* ============================================================
   Validation helpers
   ============================================================ */
const USERNAME_PATTERN = /^[a-zA-Z0-9_]{3,20}$/;

function setFieldError(input, message) {
  input.classList.add("is-invalid");
  const feedback = input.closest(".mb-3, .mb-2")?.querySelector(".invalid-feedback");
  if (feedback) feedback.textContent = message;
}

function clearFieldError(input) {
  input.classList.remove("is-invalid");
}

/* ============================================================
   Alert banner helper (shared by login + register pages)
   ============================================================ */
function showAlert(container, message, type = "danger") {
  container.innerHTML = `<div class="alert alert-tag alert-tag-${type}" role="alert">${message}</div>`;
}

function clearAlert(container) {
  container.innerHTML = "";
}

/* ============================================================
   Registration form
   ============================================================ */
function initRegisterForm() {
  const form = document.getElementById("registerForm");
  if (!form) return;

  const alertBox = document.getElementById("registerAlert");

  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    clearAlert(alertBox);

    const fullNameInput = document.getElementById("fullName");
    const usernameInput = document.getElementById("username");
    const passwordInput = document.getElementById("password");
    const roleInput = document.getElementById("role");

    [fullNameInput, usernameInput, passwordInput, roleInput].forEach(clearFieldError);

    const fullName = fullNameInput.value.trim();
    const username = usernameInput.value.trim();
    const password = passwordInput.value;
    const role = roleInput.value;

    let isValid = true;

    if (fullName.length < 2) {
      setFieldError(fullNameInput, "Enter your full name.");
      isValid = false;
    }

    if (!USERNAME_PATTERN.test(username)) {
      setFieldError(
        usernameInput,
        "Username must be 3–20 characters: letters, numbers, or underscores."
      );
      isValid = false;
    }

    if (password.length < 8) {
      setFieldError(passwordInput, "Password must be at least 8 characters.");
      isValid = false;
    }

    if (!role) {
      setFieldError(roleInput, "Select a role.");
      isValid = false;
    }

    if (!isValid) {
      showAlert(alertBox, "Please fix the highlighted fields and try again.");
      return;
    }

    // Duplicate check (case-insensitive) against existing usernames
    const users = getUsers();
    const usernameTaken = users.some(
      (u) => u.username.toLowerCase() === username.toLowerCase()
    );

    if (usernameTaken) {
      setFieldError(usernameInput, "That username is already taken.");
      showAlert(alertBox, "That username is already taken. Try logging in instead.");
      return;
    }

    // Save the new user (password stored as a hash, not plaintext)
    const submitBtn = form.querySelector('button[type="submit"]');
    submitBtn.disabled = true;
    submitBtn.textContent = "Creating account…";

    const passwordHash = await hashPassword(password);
    users.push({ fullName, username, password: passwordHash, role });
    saveUsers(users);

    // Redirect back to login so the user can sign in with their new credentials
    window.location.href = "login.html?registered=1";
  });
}

/* ============================================================
   Login form
   ============================================================ */
function initLoginForm() {
  const form = document.getElementById("loginForm");
  if (!form) return;

  const alertBox = document.getElementById("loginAlert");

  // If we just arrived from a successful registration, say so.
  const params = new URLSearchParams(window.location.search);
  if (params.get("registered") === "1") {
    showAlert(alertBox, "Account created. Log in with your new credentials.", "success");
  }

  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    clearAlert(alertBox);

    const identifierInput = document.getElementById("loginIdentifier");
    const passwordInput = document.getElementById("loginPassword");
    [identifierInput, passwordInput].forEach(clearFieldError);

    const identifier = identifierInput.value.trim();
    const password = passwordInput.value;

    if (!identifier || !password) {
      showAlert(alertBox, "Enter your username and password.");
      return;
    }

    const user = findUser(identifier);
    const enteredHash = await hashPassword(password);

    if (!user || user.password !== enteredHash) {
      showAlert(alertBox, "Invalid username or password.");
      return;
    }

    // Successful login: start a session and greet the user before redirecting.
    const session = { fullName: user.fullName, username: user.username, role: user.role };
    localStorage.setItem(SESSION_KEY, JSON.stringify(session));

    showAlert(alertBox, `Welcome, ${user.fullName}! Redirecting to your dashboard…`, "success");

    const destination = ROLE_DASHBOARDS[user.role] || "login.html";
    setTimeout(() => {
      window.location.href = destination;
    }, 900);
  });
}

/* ============================================================
   Dashboard guard + welcome header + logout
   Call this at the top of any dashboard page.
   ============================================================ */
function requireSession(expectedRole) {
  const raw = localStorage.getItem(SESSION_KEY);
  if (!raw) {
    window.location.href = "login.html";
    return null;
  }

  const session = JSON.parse(raw);
  if (expectedRole && session.role !== expectedRole) {
    // Logged in, but under a different role — send them to their own dashboard.
    window.location.href = ROLE_DASHBOARDS[session.role] || "login.html";
    return null;
  }

  // Simple greeting line above the card
  const greetingEl = document.getElementById("dashGreeting");
  if (greetingEl) {
    greetingEl.textContent = `Welcome, ${session.fullName}!`;
  }

  // User-info card — full name, username, role, read straight from the
  // session (itself built from the matched user record at login time,
  // never hardcoded per dashboard).
  const detailFullName = document.getElementById("detailFullName");
  if (detailFullName) detailFullName.textContent = session.fullName;

  const detailUsername = document.getElementById("detailUsername");
  if (detailUsername) detailUsername.textContent = session.username;

  const detailRole = document.getElementById("detailRole");
  if (detailRole) detailRole.textContent = session.role;

  const logoutBtn = document.getElementById("logoutBtn");
  if (logoutBtn) {
    logoutBtn.addEventListener("click", () => {
      localStorage.removeItem(SESSION_KEY);
      window.location.href = "login.html";
    });
  }

  return session;
}

/* ============================================================
   Init
   ============================================================ */
document.addEventListener("DOMContentLoaded", () => {
  seedDefaultAdmin();
  initRegisterForm();
  initLoginForm();
});
