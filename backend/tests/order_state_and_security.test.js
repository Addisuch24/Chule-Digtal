const test = require("node:test");
const assert = require("node:assert");
const pool = require("../src/config/db");
const orderModel = require("../src/models/orderModel");
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

test("Order State Machine, Security & Health Endpoints", async (t) => {
  let authToken = null;
  let testUserId = null;
  let testOrderId = null;
  const testUsername = "ord_test_" + Date.now();

  t.before(async () => {
    await ensureTestServerRunning(TEST_PORT);
    // Register and login a test user
    const regRes = await apiRequest("/auth/register", "POST", {
      fullName: "Order Test Admin",
      username: testUsername,
      email: `${testUsername}@gmail.com`,
      password: "testPassword123",
    });
    assert.strictEqual(regRes.status, 201, "Registration should succeed");

    const loginRes = await apiRequest("/auth/login", "POST", {
      username: testUsername,
      password: "testPassword123",
    });
    assert.strictEqual(loginRes.status, 200, "Login should succeed");
    authToken = loginRes.body.data.token;
    testUserId = loginRes.body.data.user.id;
  });

  t.after(async () => {
    if (testOrderId) {
      await pool.query("DELETE FROM orders WHERE id = $1", [testOrderId]).catch(() => {});
    }
    if (testUserId) {
      await pool.query("DELETE FROM family_members WHERE id = $1", [testUserId]).catch(() => {});
    }
  });

  await t.test("1. Liveness & Readiness health checks return 200 OK", async () => {
    const liveness = await apiRequest("/health");
    assert.strictEqual(liveness.status, 200);
    assert.strictEqual(liveness.body.success, true);

    const readiness = await apiRequest("/health/ready");
    assert.strictEqual(readiness.status, 200);
    assert.strictEqual(readiness.body.success, true);
  });

  await t.test("2. Unauthenticated request to /orders returns 401 Unauthorized", async () => {
    const res = await apiRequest("/orders", "GET");
    assert.strictEqual(res.status, 401, "Protected route must reject request without token");
  });

  await t.test("3. Order creation with valid fields persists order in PENDING status", async () => {
    const orderNum = "ORD_TST_" + Date.now();
    const createRes = await apiRequest(
      "/orders",
      "POST",
      {
        orderNumber: orderNum,
        title: "Test State Transition Order",
        description: "Testing state transition constraints",
      },
      authToken
    );

    assert.strictEqual(createRes.status, 201);
    assert.strictEqual(createRes.body.success, true);
    testOrderId = createRes.body.data.id;
    assert.strictEqual(createRes.body.data.order_status, "PENDING");
  });

  await t.test("4. Transitioning order to COMPLETED succeeds", async () => {
    const updateRes = await apiRequest(
      `/orders/${testOrderId}/status`,
      "PATCH",
      {
        orderStatus: "COMPLETED",
      },
      authToken
    );

    assert.strictEqual(updateRes.status, 200);
    assert.strictEqual(updateRes.body.success, true);
    assert.strictEqual(updateRes.body.data.order_status, "COMPLETED");
  });

  await t.test("5. Reverting or altering a COMPLETED order status is rejected with 400", async () => {
    const revertRes = await apiRequest(
      `/orders/${testOrderId}/status`,
      "PATCH",
      {
        orderStatus: "PENDING",
      },
      authToken
    );

    assert.strictEqual(revertRes.status, 400, "Should return 400 Bad Request");
    assert.strictEqual(revertRes.body.success, false);
    assert.ok(
      revertRes.body.message.includes("Completed orders cannot be changed"),
      `Expected 'Completed orders cannot be changed', got: ${revertRes.body.message}`
    );
  });
});
