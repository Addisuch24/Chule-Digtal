
const path = require("path");
require("dotenv").config({ path: path.resolve(__dirname, "../.env") });
const test = require("node:test");
const assert = require("node:assert");
const jwt = require("jsonwebtoken");
const pool = require("../src/config/db");
const financialMovementModel = require("../src/models/financialMovementModel");
const { getJwtSecret } = require("../src/config/env");
const { ensureTestServerRunning } = require("./test_server_helper");

const TEST_PORT = 5055;
const API_BASE = `http://localhost:${TEST_PORT}/api`;

async function apiRequest(path, method = "GET", body = null, token = null) {
  const res = await fetch(`${API_BASE}${path}`, {
    method,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: body ? JSON.stringify(body) : undefined,
  });
  const data = await res.json().catch(() => ({}));
  return { status: res.status, body: data };
}

test("Phase 3: Production Security & Financial Remediation Suite", async (t) => {
  let userAToken = null;
  let userBToken = null;
  let userAId = null;
  let userBId = null;
  let testCustomerId = null;
  let testMovementId = null;

  const timestamp = Date.now();
  const usernameA = `sec_user_a_${timestamp}`;
  const usernameB = `sec_user_b_${timestamp}`;

  t.before(async () => {
    await ensureTestServerRunning(TEST_PORT);

    // Create user A
    const resA = await pool.query(
      "INSERT INTO family_members (full_name, username, password_hash, is_active) VALUES ($1, $2, $3, 1) RETURNING id",
      [`Security User A ${timestamp}`, usernameA, "dummy_hash"]
    );
    userAId = Number(resA.rows[0].id);
    userAToken = jwt.sign({ id: userAId, username: usernameA }, getJwtSecret(), { expiresIn: "1h" });

    // Create user B
    const resB = await pool.query(
      "INSERT INTO family_members (full_name, username, password_hash, is_active) VALUES ($1, $2, $3, 1) RETURNING id",
      [`Security User B ${timestamp}`, usernameB, "dummy_hash"]
    );
    userBId = Number(resB.rows[0].id);
    userBToken = jwt.sign({ id: userBId, username: usernameB }, getJwtSecret(), { expiresIn: "1h" });

    // Create a test customer
    const custRes = await pool.query(
      "INSERT INTO customers (full_name, phone, created_by) VALUES ($1, $2, $3) RETURNING id",
      [`Test Security Customer ${timestamp}`, `+251999${timestamp.toString().slice(-6)}`, userAId]
    );
    testCustomerId = Number(custRes.rows[0].id);
  });

  t.after(async () => {
    delete process.env.REGISTRATION_INVITE_CODE;
    try {
      if (testMovementId) {
        await pool.query("DELETE FROM financial_movements WHERE id = $1", [testMovementId]);
      }
      if (testCustomerId) {
        await pool.query("DELETE FROM financial_movements WHERE customer_id = $1", [testCustomerId]);
        await pool.query("DELETE FROM customers WHERE id = $1", [testCustomerId]);
      }
      if (userAId || userBId) {
        await pool.query("DELETE FROM activities WHERE actor_id IN ($1, $2)", [userAId, userBId]);
        await pool.query("DELETE FROM family_members WHERE id IN ($1, $2)", [userAId, userBId]);
      }
    } catch (_) { }
  });

  // ==========================================
  // Step 2: Device Registration Authorization
  // ==========================================
  await t.test("Step 2: Unauthenticated device registration is rejected with 401", async () => {
    const res = await apiRequest("/device-tokens/register", "POST", {
      deviceToken: "token_unauth_" + timestamp,
      userId: userAId,
    });
    assert.strictEqual(res.status, 401);
    assert.strictEqual(res.body.success, false);
  });

  await t.test("Step 2: Registration for mismatched user ID is rejected with 403", async () => {
    const res = await apiRequest(
      "/device-tokens/register",
      "POST",
      {
        deviceToken: "token_mismatch_" + timestamp,
        userId: userBId, // attempting to register for user B while authenticated as user A
      },
      userAToken
    );
    assert.strictEqual(res.status, 403);
    assert.strictEqual(res.body.success, false);
  });

  await t.test("Step 2: Authenticated registration succeeds strictly for authenticated user", async () => {
    const res = await apiRequest(
      "/device-tokens/register",
      "POST",
      {
        deviceToken: "token_auth_" + timestamp,
        userId: userAId,
        platform: "android",
      },
      userAToken
    );
    assert.strictEqual(res.status, 200, "Device register failed: " + JSON.stringify(res.body));
    assert.strictEqual(res.body.success, true);
    assert.strictEqual(res.body.data.userId, userAId);
    await pool.query("DELETE FROM device_registrations WHERE device_token = $1", ["token_auth_" + timestamp]);
  });

  // ==========================================
  // Step 3: Registration Protection
  // ==========================================
  await t.test("Step 3: Registration with invalid invitation code is rejected with 403", async () => {
    process.env.REGISTRATION_INVITE_CODE = "VALID_FAMILY_SECRET_2026";
    const res = await apiRequest("/auth/register", "POST", {
      fullName: "Invited Member",
      email: `invited_${timestamp}@gmail.com`,
      username: `invited_${timestamp}`,
      password: "password123",
      inviteCode: "WRONG_SECRET",
    });
    assert.strictEqual(res.status, 403, "Register with bad code: " + JSON.stringify(res.body));
    assert.strictEqual(res.body.success, false);
  });

  await t.test("Step 3: Registration without invitation code creates inactive account", async () => {
    process.env.REGISTRATION_INVITE_CODE = "VALID_FAMILY_SECRET_2026";
    const regUsername = `unapproved_${timestamp}`;
    const res = await apiRequest("/auth/register", "POST", {
      fullName: "Unapproved Member",
      email: `${regUsername}@gmail.com`,
      username: regUsername,
      password: "password123",
    });
    assert.strictEqual(res.status, 201, "Register without code: " + JSON.stringify(res.body));
    assert.strictEqual(res.body.data.is_active, 0);

    // Inactive account cannot log in
    const loginRes = await apiRequest("/auth/login", "POST", {
      username: regUsername,
      password: "password123",
    });
    assert.strictEqual(loginRes.status, 401, "Login as inactive: " + JSON.stringify(loginRes.body));

    // Cleanup
    await pool.query("DELETE FROM family_members WHERE username = $1", [regUsername]);
  });

  await t.test("Step 3: Registration with valid invitation code creates active account", async () => {
    process.env.REGISTRATION_INVITE_CODE = "VALID_FAMILY_SECRET_2026";
    const regUsername = `approved_${timestamp}`;
    const res = await apiRequest("/auth/register", "POST", {
      fullName: "Approved Member",
      email: `${regUsername}@gmail.com`,
      username: regUsername,
      password: "password123",
      inviteCode: "VALID_FAMILY_SECRET_2026",
    });
    assert.strictEqual(res.status, 201, "Register with valid code: " + JSON.stringify(res.body));
    assert.strictEqual(res.body.data.is_active, 1);

    // Active account can log in
    const loginRes = await apiRequest("/auth/login", "POST", {
      username: regUsername,
      password: "password123",
    });
    assert.strictEqual(loginRes.status, 200, "Login as active: " + JSON.stringify(loginRes.body));
    assert.ok(loginRes.body.data.token);

    // Cleanup
    delete process.env.REGISTRATION_INVITE_CODE;
    await pool.query("DELETE FROM family_members WHERE username = $1", [regUsername]);
  });

  // ==========================================
  // Step 4: Protect Verified Financial Records
  // ==========================================
  await t.test("Step 4: Unverified financial movement can be edited normally", async () => {
    // User A records a financial movement
    const createRes = await apiRequest(
      "/financial-movements",
      "POST",
      {
        customerId: testCustomerId,
        amount: 250.00,
        movementType: "CREDIT",
        description: "Initial unverified payment",
      },
      userAToken
    );
    assert.strictEqual(createRes.status, 201, "Create unverified movement: " + JSON.stringify(createRes.body));
    testMovementId = Number(createRes.body.data.id);

    // Update unverified movement
    const updateRes = await apiRequest(
      `/financial-movements/${testMovementId}`,
      "PUT",
      {
        customerId: testCustomerId,
        amount: 300.00,
        movementType: "CREDIT",
        description: "Updated unverified payment",
      },
      userAToken
    );
    assert.strictEqual(updateRes.status, 200, "Update unverified movement: " + JSON.stringify(updateRes.body));
    assert.strictEqual(Number(updateRes.body.data.amount), 300);
  });

  // ==========================================
  // Step 5: Segregation of Duties (SoD)
  // ==========================================
  await t.test("Step 5: User cannot verify their own recorded financial movement (SoD violation)", async () => {
    // User A attempts to verify movement that User A recorded
    const verifyRes = await apiRequest(
      `/financial-movements/${testMovementId}/verify`,
      "POST",
      {
        verificationNote: "I am verifying my own transaction",
      },
      userAToken
    );
    assert.strictEqual(verifyRes.status, 403, "Self verify attempt: " + JSON.stringify(verifyRes.body));
    assert.strictEqual(verifyRes.body.success, false);
    assert.ok(verifyRes.body.message.includes("Segregation of duties"));
  });

  await t.test("Step 5: Different authorized family member can verify the movement", async () => {
    // User B verifies the movement recorded by User A
    const verifyRes = await apiRequest(
      `/financial-movements/${testMovementId}/verify`,
      "POST",
      {
        verificationNote: "Approved and verified by User B",
      },
      userBToken
    );
    assert.strictEqual(verifyRes.status, 200, "Verify by user B: " + JSON.stringify(verifyRes.body));
    assert.strictEqual(verifyRes.body.data.verification_status, "VERIFIED");
    assert.strictEqual(Number(verifyRes.body.data.verified_by), userBId);
  });

  await t.test("Step 4: Verified financial movement CANNOT be edited (returns 409 Conflict)", async () => {
    // Attempt to update the now-verified movement
    const updateRes = await apiRequest(
      `/financial-movements/${testMovementId}`,
      "PUT",
      {
        customerId: testCustomerId,
        amount: 999.00,
        movementType: "CREDIT",
        description: "Tamper attempt on verified record",
      },
      userAToken
    );
    assert.strictEqual(updateRes.status, 409, "Update verified movement: " + JSON.stringify(updateRes.body));
    assert.strictEqual(updateRes.body.success, false);
    assert.ok(updateRes.body.message.includes("Verified financial records cannot be modified"));
  });

  // ==========================================
  // Step 6: Financial Precision Arithmetic
  // ==========================================
  await t.test("Step 6: Customer balance calculation handles fractional currency with exact precision (100.10 - 100.00 = 0.10)", async () => {
    // Create new temporary test customer
    const custRes = await pool.query(
      "INSERT INTO customers (full_name, phone, created_by) VALUES ($1, $2, $3) RETURNING id",
      [`Precision Test Customer ${timestamp}`, `+251998${timestamp.toString().slice(-6)}`, userAId]
    );
    const precisionCustId = Number(custRes.rows[0].id);

    try {
      // 1. Credit 100.10
      await pool.query(
        `INSERT INTO financial_movements (customer_id, amount, movement_type, recorded_by, verification_status)
         VALUES ($1, 100.10, 'CREDIT', $2, 'VERIFIED')`,
        [precisionCustId, userAId]
      );

      // 2. Debit 100.00
      await pool.query(
        `INSERT INTO financial_movements (customer_id, amount, movement_type, recorded_by, verification_status)
         VALUES ($1, 100.00, 'DEBIT', $2, 'VERIFIED')`,
        [precisionCustId, userAId]
      );

      // 3. Compute balance via model
      const balanceResult = await financialMovementModel.getCustomerBalance(precisionCustId);

      assert.strictEqual(balanceResult.totalCredit, 100.10);
      assert.strictEqual(balanceResult.totalDebit, 100.00);
      assert.strictEqual(balanceResult.balance, 0.10, "Balance must be exactly 0.10 without IEEE 754 float drift");
    } finally {
      await pool.query("DELETE FROM financial_movements WHERE customer_id = $1", [precisionCustId]);
      await pool.query("DELETE FROM customers WHERE id = $1", [precisionCustId]);
    }
  });
});
