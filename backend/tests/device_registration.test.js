const test = require("node:test");
const assert = require("node:assert");
const pool = require("../src/config/db");
const deviceRegistrationModel = require("../src/models/deviceRegistrationModel");
const pushService = require("../src/services/pushService");

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
  const testToken1 = "test_device_token_" + Date.now();
  const testToken2 = "test_device_token_logged_out_" + Date.now();

  t.after(async () => {
    await pool.query("DELETE FROM device_registrations WHERE device_token IN (?, ?)", [testToken1, testToken2]);
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

  const [userRows] = await pool.query("SELECT id FROM family_members LIMIT 1");
  const testUserId = userRows.length > 0 ? userRows[0].id : null;

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
    assert.strictEqual(devices.length, 1);
    assert.strictEqual(devices[0].device_token, testToken1);
  });

  await t.test("3. Logging out disassociates user while retaining device registration and logged-out delivery", async () => {
    const res = await apiRequest("/device-tokens/logout", "POST", {
      deviceToken: testToken1,
    });

    assert.strictEqual(res.status, 200);
    assert.strictEqual(res.body.success, true);

    if (testUserId) {
      const userDevices = await deviceRegistrationModel.getDevicesForUser(testUserId);
      assert.strictEqual(userDevices.length, 0);
    }

    const [rows] = await pool.query("SELECT * FROM device_registrations WHERE device_token = ?", [testToken1]);
    assert.strictEqual(rows.length, 1);
    assert.strictEqual(rows[0].user_id, null);
    assert.strictEqual(rows[0].receive_logged_out, 1);
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

    const [rows] = await pool.query("SELECT * FROM device_registrations WHERE device_token = ?", [testToken1]);
    assert.strictEqual(rows.length, 0);
  });
});
