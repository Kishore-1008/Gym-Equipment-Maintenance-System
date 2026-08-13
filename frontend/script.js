/* ============================================================
   Gym Equipment Maintenance System — Authentication Module
   script.js
   ------------------------------------------------------------
   Handles:
     - Spring Boot REST API access (register / login / equipment)
     - registration form validation + submit
     - login form validation + role-based redirect
     - session handling (JWT, stored client-side) + dashboard
       access guard / logout
   ------------------------------------------------------------
   NOTE: there is no localStorage user "database" anymore and no
   seeded default admin account. Every user is created through
   POST /api/auth/register and authenticated through
   POST /api/auth/login — MySQL, via Spring Boot, is the only
   source of truth for accounts.
   ============================================================ */

/* ---------- API configuration ----------
   Point this at wherever the Spring Boot app is running. Update
   it if you run the backend on a different host/port. */
const API_BASE_URL = "http://localhost:8080/api";

/* ---------- Storage keys ---------- */
const SESSION_KEY = "gym_ams_session";

/* ============================================================
   Roles
   ------------------------------------------------------------
   ADMIN, GYM_MANAGER, and TECHNICIAN are the only supported
   roles — this is what gets stored on the user record and in
   the session. ROLE_LABELS is only for display — never store
   or compare against the human-readable label.
   ============================================================ */
const ROLES = {
  ADMIN: "ADMIN",
  GYM_MANAGER: "GYM_MANAGER",
  TECHNICIAN: "TECHNICIAN",
};

const ROLE_LABELS = {
  [ROLES.ADMIN]: "Admin",
  [ROLES.GYM_MANAGER]: "Gym Manager",
  [ROLES.TECHNICIAN]: "Technician",
};

/** Human-readable label for a stored role code (falls back to the raw code). */
function roleLabel(roleCode) {
  return ROLE_LABELS[roleCode] || roleCode;
}

/* Where each role lands after a successful login */
const ROLE_DASHBOARDS = {
  [ROLES.ADMIN]: "admin-dashboard.html",
  [ROLES.GYM_MANAGER]: "gym-manager-dashboard.html",
  [ROLES.TECHNICIAN]: "technician-dashboard.html",
};

/* ============================================================
   Session helpers
   ------------------------------------------------------------
   The session object stored in localStorage is just
   { token, fullName, username, role } — a client-side copy of
   what the backend returned at login, not a user database.
   ============================================================ */
function getSession() {
  try {
    const raw = localStorage.getItem(SESSION_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch (err) {
    console.error("Could not read session:", err);
    return null;
  }
}

function saveSession(session) {
  localStorage.setItem(SESSION_KEY, JSON.stringify(session));
}

function clearSession() {
  localStorage.removeItem(SESSION_KEY);
}

function getAuthToken() {
  const session = getSession();
  return session ? session.token : null;
}

/* ============================================================
   API request helper
   ------------------------------------------------------------
   Shared by this file and equipment-data.js. Attaches the JWT
   (if any) as a Bearer token, JSON-encodes/decodes automatically,
   and throws an Error with the backend's message on failure so
   callers can just try/catch and show it in an alert.
   ============================================================ */
async function apiRequest(path, options = {}) {
  const token = getAuthToken();
  const headers = { "Content-Type": "application/json", ...(options.headers || {}) };
  if (token) headers.Authorization = `Bearer ${token}`;

  let response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, { ...options, headers });
  } catch (err) {
    throw new Error("Couldn't reach the server. Is the Spring Boot backend running?");
  }

  const isJson = response.headers.get("content-type")?.includes("application/json");
  const body = isJson ? await response.json().catch(() => null) : null;

  if (!response.ok) {
    throw new Error((body && body.message) || `Request failed (${response.status}).`);
  }

  return body;
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

    const submitBtn = form.querySelector('button[type="submit"]');
    submitBtn.disabled = true;
    submitBtn.textContent = "Creating account…";

    try {
      // The Spring Boot backend re-validates everything above (including
      // the duplicate-username check) and is the actual source of truth —
      // this is UX only.
      await apiRequest("/auth/register", {
        method: "POST",
        body: JSON.stringify({ fullName, username, password, role }),
      });

      window.location.href = "login.html?registered=1";
    } catch (err) {
      if (/username is already taken/i.test(err.message)) {
        setFieldError(usernameInput, err.message);
      }
      showAlert(alertBox, err.message);
      submitBtn.disabled = false;
      submitBtn.textContent = "Create Account";
    }
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

    const submitBtn = form.querySelector('button[type="submit"]');
    submitBtn.disabled = true;
    submitBtn.textContent = "Logging in…";

    try {
      const auth = await apiRequest("/auth/login", {
        method: "POST",
        body: JSON.stringify({ username: identifier, password }),
      });

      // Authoritative: the backend verified the password against MySQL
      // and signed the role into the token. We just store what it gave us.
      saveSession({
        token: auth.token,
        fullName: auth.fullName,
        username: auth.username,
        role: auth.role,
      });

      showAlert(alertBox, `Welcome, ${auth.fullName}! Redirecting to your dashboard…`, "success");

      const destination = ROLE_DASHBOARDS[auth.role] || "login.html";
      setTimeout(() => {
        window.location.href = destination;
      }, 900);
    } catch (err) {
      showAlert(alertBox, err.message || "Invalid username or password.");
      submitBtn.disabled = false;
      submitBtn.textContent = "Log In";
    }
  });
}

/* ============================================================
   Dashboard guard + welcome header + logout
   Call this at the top of any dashboard page.
   ============================================================ */
function requireSession(expectedRole) {
  const session = getSession();
  if (!session || !session.token) {
    window.location.href = "login.html";
    return null;
  }

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
  // session (itself built from the backend's login response, never
  // hardcoded per dashboard).
  const detailFullName = document.getElementById("detailFullName");
  if (detailFullName) detailFullName.textContent = session.fullName;

  const detailUsername = document.getElementById("detailUsername");
  if (detailUsername) detailUsername.textContent = session.username;

  const detailRole = document.getElementById("detailRole");
  if (detailRole) detailRole.textContent = roleLabel(session.role);

  const logoutBtn = document.getElementById("logoutBtn");
  if (logoutBtn) {
    logoutBtn.addEventListener("click", () => {
      clearSession();
      window.location.href = "login.html";
    });
  }

  // Shared across all three dashboards — no-ops if a dashboard doesn't
  // include the modal markup.
  initChangePasswordModal();

  return session;
}

/* ============================================================
   Change Password modal
   ------------------------------------------------------------
   Shared by the Admin, Gym Manager, and Technician dashboards —
   one implementation instead of three. Wired up automatically
   from requireSession(), so each dashboard only needs to include
   the trigger button + modal markup (see admin/gym-manager/
   technician dashboard HTML for the shared block).

   The current session's JWT is attached automatically by
   apiRequest(); the backend identifies who's changing their
   password from that token, so nothing here ever sends a
   username for this request.
   ============================================================ */
function initChangePasswordModal() {
  const openBtn = document.getElementById("changePasswordBtn");
  const modal = document.getElementById("changePasswordModal");
  if (!openBtn || !modal) return; // this page doesn't include the modal

  const backdrop = document.getElementById("changePasswordBackdrop");
  const closeBtn = document.getElementById("changePasswordCloseBtn");
  const cancelBtn = document.getElementById("changePasswordCancelBtn");
  const form = document.getElementById("changePasswordForm");
  const alertBox = document.getElementById("changePasswordAlert");

  const currentInput = document.getElementById("currentPassword");
  const newInput = document.getElementById("newPassword");
  const confirmInput = document.getElementById("confirmNewPassword");
  const saveBtn = document.getElementById("changePasswordSaveBtn");
  const fields = [currentInput, newInput, confirmInput];

  function resetForm() {
    form.reset();
    fields.forEach(clearFieldError);
    clearAlert(alertBox);
  }

  function openModal() {
    resetForm();
    modal.hidden = false;
    if (backdrop) backdrop.hidden = false;
    currentInput.focus();
  }

  function closeModal() {
    modal.hidden = true;
    if (backdrop) backdrop.hidden = true;
    resetForm();
  }

  openBtn.addEventListener("click", openModal);
  closeBtn?.addEventListener("click", closeModal);
  cancelBtn?.addEventListener("click", closeModal);
  backdrop?.addEventListener("click", closeModal);
  document.addEventListener("keydown", (event) => {
    if (event.key === "Escape" && !modal.hidden) closeModal();
  });

  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    clearAlert(alertBox);
    fields.forEach(clearFieldError);

    const currentPassword = currentInput.value;
    const newPassword = newInput.value;
    const confirmPassword = confirmInput.value;

    let isValid = true;

    if (!currentPassword) {
      setFieldError(currentInput, "Enter your current password.");
      isValid = false;
    }

    if (newPassword.length < 8) {
      setFieldError(newInput, "New password must be at least 8 characters.");
      isValid = false;
    }

    if (isValid && newPassword === currentPassword) {
      setFieldError(newInput, "New password must be different from your current password.");
      isValid = false;
    }

    if (confirmPassword !== newPassword) {
      setFieldError(confirmInput, "Passwords do not match.");
      isValid = false;
    }

    if (!isValid) {
      showAlert(alertBox, "Please fix the highlighted fields and try again.");
      return;
    }

    saveBtn.disabled = true;
    saveBtn.textContent = "Updating…";

    try {
      // The backend re-verifies the current password against MySQL and
      // re-checks the new password's rules — this is UX only.
      await apiRequest("/auth/change-password", {
        method: "PUT",
        body: JSON.stringify({ currentPassword, newPassword, confirmPassword }),
      });

      showAlert(alertBox, "Password updated. Use your new password next time you log in.", "success");
      form.reset();
      saveBtn.disabled = false;
      saveBtn.textContent = "Update Password";

      setTimeout(closeModal, 1400);
    } catch (err) {
      showAlert(alertBox, err.message || "Could not change your password. Please try again.");
      saveBtn.disabled = false;
      saveBtn.textContent = "Update Password";
    }
  });
}

/* ============================================================
   Password visibility toggle
   ------------------------------------------------------------
   Works off data-target on any .password-toggle-btn, so it
   applies to the registration password field now and to any
   other password field (e.g. Confirm Password, login) later
   without further JS changes. Only flips the input's type
   attribute — the value itself is never touched, so the field
   still submits normally.
   ============================================================ */
function initPasswordToggles() {
  document.querySelectorAll(".password-toggle-btn").forEach((btn) => {
    btn.addEventListener("click", () => {
      const input = document.getElementById(btn.getAttribute("data-target"));
      if (!input) return;

      const willShow = input.type === "password";
      input.type = willShow ? "text" : "password";

      btn.classList.toggle("is-visible", willShow);
      btn.setAttribute("aria-pressed", String(willShow));
      btn.setAttribute("aria-label", willShow ? "Hide password" : "Show password");
    });
  });
}

/* ============================================================
   Init
   ============================================================ */
document.addEventListener("DOMContentLoaded", () => {
  initRegisterForm();
  initLoginForm();
  initPasswordToggles();
});
