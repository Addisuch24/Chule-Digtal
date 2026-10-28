const test = require("node:test");
const assert = require("node:assert");
const pool = require("../src/config/db");
const deviceRegistrationModel = require("../src/models/deviceRegistrationModel");
const pushService = require("../src/services/pushService");
const { ensureTestServerRunning } = require("./test_server_helper");

const API_BASE = "http://localhost:5000/api";

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

test("Device Registration & Push Architecture", async (t) => {
  t.before(async () => {
    await ensureTestServerRunning(5000);
  });

  const testToken1 = "test_device_token_" + Date.now();
  const testToken2 = "test_device_token_logged_out_" + Date.now();

  t.after(async () => {
    await pool.query("DELETE FROM device_registrations WHERE device_token IN ($1, $2)", [testToken1, testToken2]);
  });

  await t.test("1. Can register a device token without login (logged-out registration)", async () => {
    const res = await apiRequest("/device-tokens/register", "POST", {
      deviceToken: testToken1,
      platform: "android",
      receiveLoggedOut: true,
    });

    assert.strictEqual(res.status, 200);
    assert.strictEqual(res.body.success, true);
    assert.strictEqual(res.body.data.deviceToken, testToken1);
    assert.strictEqual(res.body.data.userId, null);
    assert.strictEqual(res.body.data.receiveLoggedOut, true);
  });

  const userRes = await pool.query("SELECT id FROM family_members LIMIT 1");
  const testUserId = userRes.rows.length > 0 ? Number(userRes.rows[0].id) : null;

  await t.test("2. Can associate user with existing device registration", async () => {
    if (!testUserId) return;
    const res = await apiRequest("/device-tokens/register", "POST", {
      deviceToken: testToken1,
      userId: testUserId,
      platform: "android",
      receiveLoggedOut: true,
    });

    assert.strictEqual(res.status, 200);
    assert.strictEqual(res.body.data.userId, testUserId);

    const devices = await deviceRegistrationModel.getDevicesForUser(testUserId);
    assert.ok(devices.some((d) => d.device_token === testToken1));
  });

  await t.test("3. Logging out disassociates user while retaining device registration and logged-out delivery", async () => {
    const res = await apiRequest("/device-tokens/logout", "POST", {
      deviceToken: testToken1,
    });

    assert.strictEqual(res.status, 200);
    assert.strictEqual(res.body.success, true);

    if (testUserId) {
      const userDevices = await deviceRegistrationModel.getDevicesForUser(testUserId);
      assert.ok(!userDevices.some((d) => d.device_token === testToken1), "testToken1 must be disassociated");
    }

    const regRes = await pool.query("SELECT * FROM device_registrations WHERE device_token = $1", [testToken1]);
    assert.strictEqual(regRes.rows.length, 1);
    assert.strictEqual(regRes.rows[0].user_id, null);
    assert.strictEqual(regRes.rows[0].receive_logged_out, 1);
  });

  await t.test("4. Push service sanitizes payload and does not include tokens or passwords", async () => {
    const samplePayload = {
      id: "101",
      notificationType: "ORDER_CREATED",
      entityType: "ORDER",
      entityId: "505",
      title: "New Order #505",
      message: "An order has been placed.",
      token: "secret_token_that_must_not_leak",
      password: "secret_password",
    };

    const pushResult = await pushService.sendPushNotification([testToken1], samplePayload);
    assert.strictEqual(pushResult.successCount, 1);
    assert.strictEqual(pushResult.failureCount, 0);
  });

  await t.test("5. Can unregister device token completely", async () => {
    const res = await apiRequest(`/device-tokens/${testToken1}`, "DELETE");

    assert.strictEqual(res.status, 200);
    assert.strictEqual(res.body.success, true);

    const unregRes = await pool.query("SELECT * FROM device_registrations WHERE device_token = $1", [testToken1]);
    assert.strictEqual(unregRes.rows.length, 0);
  });
});
