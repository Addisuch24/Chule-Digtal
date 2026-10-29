const test = require("node:test");
const assert = require("node:assert");
const bcrypt = require("bcryptjs");
const jwt = require("jsonwebtoken");
const pool = require("../src/config/db");
const authModel = require("../src/models/authModel");
const customerModel = require("../src/models/customerModel");
const customerNoteModel = require("../src/models/customerNoteModel");
const businessNoteModel = require("../src/models/businessNoteModel");
const orderModel = require("../src/models/orderModel");
const financialMovementModel = require("../src/models/financialMovementModel");
const activityModel = require("../src/models/activityModel");
const notificationModel = require("../src/models/notificationModel");
const { ensureTestServerRunning, stopTestServer } = require("./test_server_helper");
const { getJwtSecret } = require("../src/config/env");

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

test("Supabase PostgreSQL Migration Verification Suite", async (t) => {
  await ensureTestServerRunning(TEST_PORT);

  const timestamp = Date.now();
  const testUsername = `migr_test_${timestamp}`;
  const testPassword = "SecureTestPassword123!";
  const passwordHash = await bcrypt.hash(testPassword, 10);

  // Create isolated test family member
  const insertUserRes = await pool.query(
    `INSERT INTO family_members (full_name, phone, email, username, password_hash, is_active)
     VALUES ($1, $2, $3, $4, $5, 1)
     RETURNING id`,
    [`Migration Tester ${timestamp}`, `+251911${timestamp.toString().slice(-6)}`, `${testUsername}@example.com`, testUsername, passwordHash]
  );
  const testUserId = insertUserRes.rows[0].id;
  const token = jwt.sign({ id: testUserId, username: testUsername }, getJwtSecret(), { expiresIn: "1h" });

  // Cleanup all test records after test suite completes
  t.after(async () => {
    try {
      await pool.query("DELETE FROM notifications WHERE recipient_id = $1", [testUserId]);
      await pool.query("DELETE FROM activities WHERE actor_id = $1", [testUserId]);
      await pool.query("DELETE FROM financial_movements WHERE recorded_by = $1", [testUserId]);
      await pool.query("DELETE FROM orders WHERE taken_by = $1", [testUserId]);
      await pool.query("DELETE FROM customer_notes WHERE created_by = $1", [testUserId]);
      await pool.query("DELETE FROM business_notes WHERE created_by = $1", [testUserId]);
      await pool.query("DELETE FROM customers WHERE created_by = $1", [testUserId]);
      await pool.query("DELETE FROM family_members WHERE id = $1", [testUserId]);
    } catch (err) {
      console.warn("Cleanup warning:", err.message);
    }
    await stopTestServer();
  });

  await t.test("1. Database connectivity & version check (SELECT current_database(), version())", async () => {
    const res = await pool.query("SELECT current_database(), version()");
    assert.ok(res.rows.length === 1);
    assert.strictEqual(res.rows[0].current_database, "postgres");
    assert.ok(res.rows[0].version.includes("PostgreSQL 17"), `Expected PostgreSQL 17, got: ${res.rows[0].version}`);
  });

  await t.test("2. Read-only queries across migrated business tables", async () => {
    const tables = [
      "customers",
      "family_members",
      "orders",
      "financial_movements",
      "customer_notes",
      "business_notes",
      "activities",
      "device_registrations",
    ];

    for (const tbl of tables) {
      const countRes = await pool.query(`SELECT COUNT(*) AS total FROM "${tbl}"`);
      assert.strictEqual(typeof countRes.rows[0].total, "number", `COUNT(*) for ${tbl} should be parsed as number`);
      assert.ok(countRes.rows[0].total >= 0, `Table ${tbl} should have non-negative row count`);
    }
  });

  await t.test("3. Login and authentication workflows", async () => {
    // 3a. Successful login with correct credentials
    const loginRes = await apiRequest("/auth/login", "POST", {
      username: testUsername,
      password: testPassword,
    });
    assert.strictEqual(loginRes.status, 200);
    assert.strictEqual(loginRes.body.success, true);
    assert.ok(loginRes.body.data.token, "Token must be returned on login");
    assert.strictEqual(loginRes.body.data.user.id, testUserId);

    // 3b. Failed login with wrong password
    const badLoginRes = await apiRequest("/auth/login", "POST", {
      username: testUsername,
      password: "WrongPassword123!",
    });
    assert.strictEqual(badLoginRes.status, 401);
    assert.strictEqual(badLoginRes.body.success, false);

    // 3c. Duplicate registration returns 409 conflict
    const dupRes = await apiRequest("/auth/register", "POST", {
      fullName: "Duplicate User",
      email: "dup_tester@example.com",
      username: testUsername,
      password: "AnyPassword123!",
    });
    assert.strictEqual(dupRes.status, 409);
    assert.strictEqual(dupRes.body.success, false);

    // 3d. Profile retrieval with valid token
    const profileRes = await apiRequest("/auth/me", "GET", null, token);
    assert.strictEqual(profileRes.status, 200);
    assert.strictEqual(profileRes.body.data.username, testUsername);
  });

  await t.test("4. Customer creation, retrieval, and notes auditing", async () => {
    // 4a. Create customer
    const createCustRes = await apiRequest(
      "/customers",
      "POST",
      {
        fullName: `Test Customer ${timestamp}`,
        phone: `+251922${timestamp.toString().slice(-6)}`,
      },
      token
    );
    assert.strictEqual(createCustRes.status, 201);
    const customerId = createCustRes.body.data.id;
    assert.ok(customerId > 0);

    // 4b. Retrieve customer by ID
    const getCustRes = await apiRequest(`/customers/${customerId}`, "GET", null, token);
    assert.strictEqual(getCustRes.status, 200);
    assert.strictEqual(getCustRes.body.data.id, customerId);

    // 4c. Create customer note
    const noteRes = await apiRequest(
      `/customers/${customerId}/notes`,
      "POST",
      { noteText: "Important customer note for verification test." },
      token
    );
    assert.strictEqual(noteRes.status, 201);
    const noteId = noteRes.body.data.id;

    // 4d. Fetch customer notes list
    const notesListRes = await apiRequest(`/customers/${customerId}/notes`, "GET", null, token);
    assert.strictEqual(notesListRes.status, 200);
    assert.ok(notesListRes.body.data.some((n) => n.id === noteId));

    // 4e. Delete test customer note
    const delNoteRes = await apiRequest(`/customers/${customerId}/notes/${noteId}`, "DELETE", null, token);
    assert.strictEqual(delNoteRes.status, 200);

    // 4f. Delete test customer cleanly
    const delCustRes = await apiRequest(`/customers/${customerId}`, "DELETE", null, token);
    assert.strictEqual(delCustRes.status, 200);
  });

  await t.test("5. Order workflow and financial movement verification", async () => {
    // 5a. Create a customer for order testing
    const custRes = await apiRequest(
      "/customers",
      "POST",
      { fullName: `Order Workflow Customer ${timestamp}`, phone: `0933${timestamp.toString().slice(-6)}` },
      token
    );
    const customerId = custRes.body.data.id;

    // 5b. Create order
    const orderRes = await apiRequest(
      "/orders",
      "POST",
      {
        customerId,
        orderNumber: `ORD-TEST-${timestamp}`,
        title: "Test Migration Order",
        description: "Checking PostgreSQL transition integrity",
      },
      token
    );
    assert.strictEqual(orderRes.status, 201);
    const orderId = orderRes.body.data.id;
    assert.strictEqual(orderRes.body.data.order_status, "PENDING");

    // 5c. Transition order to COMPLETED
    const statusRes = await apiRequest(`/orders/${orderId}/status`, "PATCH", { orderStatus: "COMPLETED" }, token);
    assert.strictEqual(statusRes.status, 200);
    const updatedStatus = statusRes.body.data?.order_status ?? statusRes.body.data?.order?.order_status;
    assert.strictEqual(updatedStatus, "COMPLETED");

    // 5d. Completed order cannot be reverted (rejected with 400)
    const revertRes = await apiRequest(`/orders/${orderId}/status`, "PATCH", { orderStatus: "PENDING" }, token);
    assert.strictEqual(revertRes.status, 400);

    // 5e. Financial movement creation with idempotency key
    const idempotencyKey = `idem-key-${timestamp}`;
    const finRes1 = await apiRequest(
      "/financial-movements",
      "POST",
      {
        customerId,
        amount: 250.75,
        movementType: "CREDIT",
        description: "Test deposit movement",
        idempotencyKey,
      },
      token
    );
    assert.strictEqual(finRes1.status, 201);
    const movementId = finRes1.body.data.id;

    // 5f. Submitting same idempotency key returns existing movement without duplicate insert
    const finRes2 = await apiRequest(
      "/financial-movements",
      "POST",
      {
        customerId,
        amount: 250.75,
        movementType: "CREDIT",
        description: "Duplicate deposit attempt",
        idempotencyKey,
      },
      token
    );
    assert.strictEqual(finRes2.status, 200);
    assert.strictEqual(finRes2.body.data.id, movementId);

    // 5g. Verify financial movement
    const verifyRes = await apiRequest(
      `/financial-movements/${movementId}/verify`,
      "PATCH",
      { verificationNote: "Audited and verified by migration tester" },
      token
    );
    assert.strictEqual(verifyRes.status, 200);
    assert.strictEqual(verifyRes.body.data.verification_status, "VERIFIED");

    // 5h. Verified financial movements cannot be deleted
    const deleteVerifiedRes = await apiRequest(`/financial-movements/${movementId}`, "DELETE", null, token);
    assert.strictEqual(deleteVerifiedRes.status, 400);

    // 5i. Clean up test records in transaction
    await pool.query("DELETE FROM financial_movements WHERE id = $1", [movementId]);
    await pool.query("DELETE FROM orders WHERE id = $1", [orderId]);
    await pool.query("DELETE FROM customers WHERE id = $1", [customerId]);
  });

  await t.test("6. Rejection of unauthorized and invalid requests", async () => {
    // 6a. Unauthenticated request to protected endpoints rejected with 401
    const unauthOrders = await apiRequest("/orders", "GET");
    assert.strictEqual(unauthOrders.status, 401);

    const unauthCustomers = await apiRequest("/customers", "GET");
    assert.strictEqual(unauthCustomers.status, 401);

    const unauthFinancial = await apiRequest("/financial-movements", "GET");
    assert.strictEqual(unauthFinancial.status, 401);

    // 6b. Invalid non-integer ID rejected with 400
    const badIdRes = await apiRequest("/customers/abc", "GET", null, token);
    assert.strictEqual(badIdRes.status, 400);

    // 6c. Non-existent resource returns 404
    const notFoundRes = await apiRequest("/customers/999999999", "GET", null, token);
    assert.strictEqual(notFoundRes.status, 404);
  });

  await t.test("7. Real-time notification creation and unread count", async () => {
    const notif = await notificationModel.createNotification({
      recipientId: testUserId,
      notificationType: "SYSTEM",
      title: "Realtime Migration Check",
      message: "Testing delivery without importing historical notifications",
    });
    assert.ok(typeof notif.id === "number", "Notification ID must be a number");

    // Fetch unread count
    const unreadCount = await notificationModel.getUnreadNotificationCount(testUserId);
    assert.ok(unreadCount >= 1);

    // Fetch notifications list via API
    const listRes = await apiRequest(`/notifications/recipient/${testUserId}`, "GET", null, token);
    assert.strictEqual(listRes.status, 200);
    assert.ok(listRes.body.data.some((item) => item.id === notif.id));

    // Mark as read
    const markRes = await apiRequest(`/notifications/${notif.id}/read`, "PATCH", null, token);
    assert.strictEqual(markRes.status, 200);
    assert.strictEqual(markRes.body.data.is_read, true);

    // Delete single notification
    const delRes = await apiRequest(`/notifications/${notif.id}`, "DELETE", null, token);
    assert.strictEqual(delRes.status, 200);
  });
});
