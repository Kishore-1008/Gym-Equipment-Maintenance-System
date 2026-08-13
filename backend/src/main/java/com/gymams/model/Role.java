package com.gymams.model;

/**
 * The only three supported roles — stored as this exact code in the
 * database, never as the display label. Matches ROLES in the frontend
 * script.js.
 */
public enum Role {
    ADMIN,
    GYM_MANAGER,
    TECHNICIAN
}
