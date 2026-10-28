require("dotenv").config();

const KNOWN_PLACEHOLDER_SECRET = "chule_digita_change_this_to_a_long_random_secret";
function isPlaceholderSecret(secret) {
  if (!secret || !secret.trim()) return true;

  const value = secret.trim();

  return (
    value === KNOWN_PLACEHOLDER_SECRET ||

    /replace[_ -]?with|change[_ -]?this|your[_ -]|placeholder|example[_ -]?secret|changeme|todo/i.test(value)
  );
}
/**
 * Validates that all critical environment variables are present and secure.
 * In production (NODE_ENV=production), enforces strict secret length and prohibits known placeholders.
 *
 * @throws {Error} If any production requirement is violated.
 */
function validateEnv() {
  const isProduction = process.env.NODE_ENV === "production";
  const errors = [];

  const jwtSecret = process.env.JWT_SECRET;
  if (isProduction) {
    if (!jwtSecret || jwtSecret.trim() === "") {
      errors.push("JWT_SECRET is missing or empty in production.");
    } else if (jwtSecret.trim() === KNOWN_PLACEHOLDER_SECRET) {
      errors.push("JWT_SECRET must not use the default placeholder secret in production.");
    } else if (jwtSecret.trim().length < 32) {
      errors.push("JWT_SECRET must be at least 32 characters long in production.");
    }

    const hasDbUrl = Boolean(
      (process.env.DATABASE_URL && process.env.DATABASE_URL.trim()) ||
      (process.env.SUPABASE_DB_URL && process.env.SUPABASE_DB_URL.trim())
    );

    if (!process.env.PGHOST && !process.env.DB_HOST && !hasDbUrl) {
      errors.push("PGHOST, DB_HOST, DATABASE_URL, or SUPABASE_DB_URL is required in production.");
    }
    if (!process.env.PGUSER && !process.env.DB_USER && !hasDbUrl) {
      errors.push("PGUSER, DB_USER, DATABASE_URL, or SUPABASE_DB_URL is required in production.");
    }
    if (!process.env.PGDATABASE && !process.env.DB_NAME && !hasDbUrl) {
      errors.push("PGDATABASE, DB_NAME, DATABASE_URL, or SUPABASE_DB_URL is required in production.");
    }

    const corsOrigin = process.env.CORS_ORIGIN;
    if (!corsOrigin || corsOrigin.trim() === "" || corsOrigin.trim() === "*") {
      errors.push("CORS_ORIGIN must be explicitly set to trusted domain(s) in production (wildcard '*' is forbidden).");
    }
  }

  if (errors.length > 0) {
    const errorMsg = `[FATAL CONFIGURATION ERROR] Production environment validation failed:\n  - ${errors.join("\n  - ")}`;
    console.error(errorMsg);
    throw new Error(errorMsg);
  }
}

/**
 * Returns the active JWT secret.
 * Guarantees no placeholder secret is ever used in production.
 */
function getJwtSecret() {
  const isProduction = process.env.NODE_ENV === "production";
  const secret = process.env.JWT_SECRET;

  if (isProduction) {
    if (!secret || secret.trim() === KNOWN_PLACEHOLDER_SECRET || secret.trim().length < 32) {
      throw new Error("Invalid or unconfigured JWT_SECRET in production environment");
    }
    return secret.trim();
  }

  return secret ? secret.trim() : KNOWN_PLACEHOLDER_SECRET;
}

/**
 * Returns allowed CORS origins.
 * In production, extracts trusted domain list. In development/testing, defaults to permissive local origins.
 */
function getCorsOrigin() {
  const isProduction = process.env.NODE_ENV === "production";
  const origin = process.env.CORS_ORIGIN;

  if (isProduction) {
    if (!origin || origin.trim() === "" || origin.trim() === "*") {
      throw new Error("CORS_ORIGIN must be explicitly set to trusted origin(s) in production.");
    }
    if (origin.includes(",")) {
      return origin.split(",").map((o) => o.trim()).filter(Boolean);
    }
    return origin.trim();
  }

  if (origin && origin.trim()) {
    if (origin.includes(",")) {
      return origin.split(",").map((o) => o.trim()).filter(Boolean);
    }
    return origin.trim();
  }

  // Development / Test default: allow all
  return "*";
}

module.exports = {
  validateEnv,
  getJwtSecret,
  getCorsOrigin,
  KNOWN_PLACEHOLDER_SECRET,
};
