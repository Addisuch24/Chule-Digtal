const test = require("node:test");
const assert = require("node:assert/strict");
const { validateEnv, getJwtSecret, getCorsOrigin, KNOWN_PLACEHOLDER_SECRET } = require("../src/config/env");

test("Environment & Production Hardening Suite", async (t) => {
  const originalEnv = { ...process.env };

  t.afterEach(() => {
    // Restore original environment after each test
    process.env = { ...originalEnv };
  });

  await t.test("1. Development mode allows fallback JWT secret without throwing", () => {
    delete process.env.NODE_ENV;
    delete process.env.JWT_SECRET;

    assert.doesNotThrow(() => validateEnv());
    assert.equal(getJwtSecret(), KNOWN_PLACEHOLDER_SECRET);
    assert.equal(getCorsOrigin(), "*");
  });

  await t.test("2. Production mode fails startup when JWT_SECRET is missing", () => {
    process.env.NODE_ENV = "production";
    delete process.env.JWT_SECRET;
    process.env.DB_HOST = "localhost";
    process.env.DB_USER = "root";
    process.env.DB_NAME = "chule_db";
    process.env.CORS_ORIGIN = "https://app.chuledigital.com";

    assert.throws(
      () => validateEnv(),
      /JWT_SECRET is missing or empty in production/
    );
  });

  await t.test("3. Production mode fails startup when JWT_SECRET is the default placeholder", () => {
    process.env.NODE_ENV = "production";
    process.env.JWT_SECRET = KNOWN_PLACEHOLDER_SECRET;
    process.env.DB_HOST = "localhost";
    process.env.DB_USER = "root";
    process.env.DB_NAME = "chule_db";
    process.env.CORS_ORIGIN = "https://app.chuledigital.com";

    assert.throws(
      () => validateEnv(),
      /JWT_SECRET must not use the default placeholder secret in production/
    );
  });

  await t.test("4. Production mode fails startup when JWT_SECRET is shorter than 32 chars", () => {
    process.env.NODE_ENV = "production";
    process.env.JWT_SECRET = "short_secret_under_32_chars";
    process.env.DB_HOST = "localhost";
    process.env.DB_USER = "root";
    process.env.DB_NAME = "chule_db";
    process.env.CORS_ORIGIN = "https://app.chuledigital.com";

    assert.throws(
      () => validateEnv(),
      /JWT_SECRET must be at least 32 characters long in production/
    );
  });

  await t.test("5. Production mode fails startup when CORS_ORIGIN is wildcard or missing", () => {
    process.env.NODE_ENV = "production";
    process.env.JWT_SECRET = "a_very_secure_cryptographic_secret_key_32_bytes!";
    process.env.DB_HOST = "localhost";
    process.env.DB_USER = "root";
    process.env.DB_NAME = "chule_db";
    process.env.CORS_ORIGIN = "*";

    assert.throws(
      () => validateEnv(),
      /CORS_ORIGIN must be explicitly set to trusted domain\(s\) in production/
    );
  });

  await t.test("6. Production mode passes validation with secure configuration", () => {
    process.env.NODE_ENV = "production";
    const secureSecret = "c5a939f88d2f0d927a3c3e80f948f73e0a2948bb8f2910d65a839d09c2a8b9e1";
    process.env.JWT_SECRET = secureSecret;
    process.env.DB_HOST = "10.0.0.5";
    process.env.DB_USER = "chule_prod";
    process.env.DB_NAME = "chule_digita_db";
    process.env.CORS_ORIGIN = "https://app.chuledigital.com, https://admin.chuledigital.com";

    assert.doesNotThrow(() => validateEnv());
    assert.equal(getJwtSecret(), secureSecret);

    const origins = getCorsOrigin();
    assert.deepEqual(origins, ["https://app.chuledigital.com", "https://admin.chuledigital.com"]);
  });
});
