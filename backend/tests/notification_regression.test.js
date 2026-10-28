const test = require("node:test");
const assert = require("node:assert");
const jwt = require("jsonwebtoken");
const http = require("http");
const pool = require("../src/config/db");
const notificationModel = require("../src/models/notificationModel");
const socketClient = require("socket.io-client");
const { ensureTestServerRunning, stopTestServer } = require("./test_server_helper");

const { getJwtSecret } = require("../src/config/env");
const JWT_SECRET = getJwtSecret();
const API_BASE = "http://localhost:5000/api";

function generateToken(userId, username) {
  return jwt.sign({ id: userId, username }, JWT_SECRET, { expiresIn: "1h" });
}

function apiRequest(path, method = "GET", token, body = null) {
  return new Promise((resolve, reject) => {
    const url = new URL(`${API_BASE}${path}`);
    const options = {
      hostname: url.hostname,
      port: url.port,
      path: url.pathname + url.search,
      method,
      headers: {
        "Content-Type": "application/json",
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
    };

    const req = http.request(options, (res) => {
      let data = "";
      res.on("data", (chunk) => (data += chunk));
      res.on("end", () => {
        try {
          const parsed = data ? JSON.parse(data) : {};
          resolve({ status: res.statusCode, body: parsed });
        } catch (e) {
          resolve({ status: res.statusCode, body: data });
        }
      });
    });

    req.on("error", reject);
    if (body) {
      req.write(JSON.stringify(body));
    }
    req.end();
  });
}

test("Notification Persistence & Regression Test Suite", async (t) => {
  await ensureTestServerRunning(5000);
  // Setup two test users in family_members
  const timestamp = Date.now();
  const resA = await pool.query(
    "INSERT INTO family_members (full_name, username, password_hash, is_active) VALUES ($1, $2, $3, 1) RETURNING id",
    [`Test User A ${timestamp}`, `test_a_${timestamp}`, "hash"]
  );
  const userAId = resA.rows[0].id;
  const tokenA = generateToken(userAId, `test_a_${timestamp}`);

  const resB = await pool.query(
    "INSERT INTO family_members (full_name, username, password_hash, is_active) VALUES ($1, $2, $3, 1) RETURNING id",
    [`Test User B ${timestamp}`, `test_b_${timestamp}`, "hash"]
  );
  const userBId = resB.rows[0].id;
  const tokenB = generateToken(userBId, `test_b_${timestamp}`);

  t.after(async () => {
    try {
      await pool.query("DELETE FROM notifications WHERE recipient_id IN ($1, $2)", [userAId, userBId]);
      await pool.query("DELETE FROM family_members WHERE id IN ($1, $2)", [userAId, userBId]);
    } catch (e) {
      // ignore
    }
    await stopTestServer();
  });

  await t.test("1. Delete one notification and refresh: it remains deleted", async () => {
    const n = await notificationModel.createNotification({
      recipientId: userAId,
      notificationType: "SYSTEM",
      title: "Test Single Delete",
      message: "Message to delete",
    });

    // Verify it exists in DB
    const beforeGet = await apiRequest(`/notifications/recipient/${userAId}`, "GET", tokenA);
    assert.strictEqual(beforeGet.status, 200);
    assert.ok(beforeGet.body.data.some((item) => item.id === n.id));

    // Delete single notification
    const delRes = await apiRequest(`/notifications/${n.id}`, "DELETE", tokenA);
    assert.strictEqual(delRes.status, 200);
    assert.strictEqual(delRes.body.success, true);

    // Refresh from backend (simulate page refresh)
    const afterGet = await apiRequest(`/notifications/recipient/${userAId}`, "GET", tokenA);
    assert.strictEqual(afterGet.status, 200);
    assert.ok(!afterGet.body.data.some((item) => item.id === n.id), "Deleted notification must not reappear");
  });

  await t.test("2. Delete several notifications and refresh: all remain deleted", async () => {
    const n1 = await notificationModel.createNotification({
      recipientId: userAId,
      notificationType: "SYSTEM",
      title: "Bulk Test 1",
      message: "Message 1",
    });
    const n2 = await notificationModel.createNotification({
      recipientId: userAId,
      notificationType: "SYSTEM",
      title: "Bulk Test 2",
      message: "Message 2",
    });
    const n3 = await notificationModel.createNotification({
      recipientId: userAId,
      notificationType: "SYSTEM",
      title: "Bulk Test 3 Keep",
      message: "Message 3",
    });

    // Bulk delete n1 and n2
    const delRes = await apiRequest("/notifications/bulk-delete", "POST", tokenA, {
      notificationIds: [n1.id, n2.id],
    });
    assert.strictEqual(delRes.status, 200);
    assert.strictEqual(delRes.body.data.deletedCount, 2);
    assert.deepStrictEqual(delRes.body.data.deletedIds.sort(), [n1.id, n2.id].sort());

    // Refresh and verify
    const afterGet = await apiRequest(`/notifications/recipient/${userAId}`, "GET", tokenA);
    const ids = afterGet.body.data.map((item) => item.id);
    assert.ok(!ids.includes(n1.id), "n1 must remain deleted");
    assert.ok(!ids.includes(n2.id), "n2 must remain deleted");
    assert.ok(ids.includes(n3.id), "n3 must remain present");
  });

  await t.test("3. Restart the app/browser (new session): deleted notifications do not return", async () => {
    // Generate fresh token simulating app restart / new session
    const freshTokenA = generateToken(userAId, `test_a_${timestamp}`);
    const getRes = await apiRequest(`/notifications/recipient/${userAId}`, "GET", freshTokenA);
    assert.strictEqual(getRes.status, 200);
    // Deleted notifications from previous tests must still be absent
    const titles = getRes.body.data.map((n) => n.title);
    assert.ok(!titles.includes("Test Single Delete"));
    assert.ok(!titles.includes("Bulk Test 1"));
    assert.ok(!titles.includes("Bulk Test 2"));
  });

  await t.test("4. Multi-device sync: delete on Device A emits real-time event to Device B", async () => {
    const n = await notificationModel.createNotification({
      recipientId: userAId,
      notificationType: "SYSTEM",
      title: "Realtime Sync Delete",
      message: "Sync message",
    });

    // Connect a socket client for User A (simulating Device B)
    const socketB = socketClient("http://localhost:5000", {
      auth: { token: tokenA },
      transports: ["websocket"],
      forceNew: true,
    });

    const deletedEventPromise = new Promise((resolve, reject) => {
      const timer = setTimeout(() => reject(new Error("Timeout waiting for notification:deleted")), 5000);
      socketB.on("notification:deleted", (payload) => {
        clearTimeout(timer);
        resolve(payload);
      });
    });

    await new Promise((resolve, reject) => {
      const timer = setTimeout(() => reject(new Error("Timeout waiting for socket connect")), 5000);
      socketB.on("connect", () => {
        clearTimeout(timer);
        resolve();
      });
      socketB.on("connect_error", (err) => {
        clearTimeout(timer);
        reject(err);
      });
    });

    // Device A deletes notification
    const delRes = await apiRequest(`/notifications/${n.id}`, "DELETE", tokenA);
    assert.strictEqual(delRes.status, 200);

    // Device B receives real-time delete event
    const receivedPayload = await deletedEventPromise;
    assert.strictEqual(Number(receivedPayload.notificationId), n.id);
    assert.strictEqual(Number(receivedPayload.recipientId), userAId);

    socketB.disconnect();

    // Disconnect and reconnect socket, verify notification remains absent
    const afterRefresh = await apiRequest(`/notifications/recipient/${userAId}`, "GET", tokenA);
    assert.ok(!afterRefresh.body.data.some((item) => item.id === n.id));
  });

  await t.test("5. Cross-user isolation: User B's notifications remain unaffected", async () => {
    const nB = await notificationModel.createNotification({
      recipientId: userBId,
      notificationType: "ORDER_CREATED",
      title: "User B Private Notification",
      message: "Secret order",
    });

    // User A clears all notifications
    const clearAllRes = await apiRequest("/notifications/all", "DELETE", tokenA);
    assert.strictEqual(clearAllRes.status, 200);

    // User A should have 0 notifications
    const getA = await apiRequest(`/notifications/recipient/${userAId}`, "GET", tokenA);
    assert.strictEqual(getA.body.data.length, 0);

    // User B's notification must remain completely unaffected
    const getB = await apiRequest(`/notifications/recipient/${userBId}`, "GET", tokenB);
    assert.ok(getB.body.data.some((item) => item.id === nB.id), "User B notification must remain intact");
  });

  await t.test("6. Unauthorized deletion requests are rejected", async () => {
    const nB = await notificationModel.createNotification({
      recipientId: userBId,
      notificationType: "CUSTOMER_CREATED",
      title: "User B Only",
      message: "Cannot touch",
    });

    // User A tries to delete User B's notification
    const hackRes = await apiRequest(`/notifications/${nB.id}`, "DELETE", tokenA);
    assert.strictEqual(hackRes.status, 403, "Must return 403 Forbidden");

    // User A tries bulk-delete containing User B's notification
    const bulkHackRes = await apiRequest("/notifications/bulk-delete", "POST", tokenA, {
      notificationIds: [nB.id],
    });
    assert.strictEqual(bulkHackRes.status, 404, "Must reject unauthorized/non-owned items");

    // Verify nB still exists
    const checkB = await apiRequest(`/notifications/recipient/${userBId}`, "GET", tokenB);
    assert.ok(checkB.body.data.some((item) => item.id === nB.id));

    // Ownership check: User B can retrieve their own notification
    const ownGetRes = await apiRequest(`/notifications/${nB.id}`, "GET", tokenB);
    assert.strictEqual(ownGetRes.status, 200, "Owner must be allowed to view own notification");
    assert.strictEqual(Number(ownGetRes.body.data.id), Number(nB.id), "Must return correct notification");

    // Ownership check: User A is denied access to User B's notification
    const crossGetRes = await apiRequest(`/notifications/${nB.id}`, "GET", tokenA);
    assert.strictEqual(crossGetRes.status, 403, "Non-owner must be rejected with 403 Forbidden");
    assert.strictEqual(crossGetRes.body.message, "You are not allowed to view this notification");

    // Ownership check: Nonexistent notification returns 404
    const nonExistentRes = await apiRequest("/notifications/999999999", "GET", tokenA);
    assert.strictEqual(nonExistentRes.status, 404, "Nonexistent notification must return 404 Not Found");
    assert.strictEqual(nonExistentRes.body.message, "Notification not found");
  });

  await t.test("7. Pagination works without reintroducing deleted records", async () => {
    // Create 15 notifications for User A
    const createdIds = [];
    for (let i = 1; i <= 15; i++) {
      const created = await notificationModel.createNotification({
        recipientId: userAId,
        notificationType: "SYSTEM",
        title: `Page Test ${i}`,
        message: `Msg ${i}`,
      });
      createdIds.push(created.id);
    }

    // Page 1 with limit 5
    const page1 = await apiRequest(`/notifications/recipient/${userAId}?page=1&limit=5`, "GET", tokenA);
    assert.strictEqual(page1.body.data.length, 5);
    assert.strictEqual(page1.body.pagination.total, 15);
    assert.strictEqual(page1.body.pagination.totalPages, 3);

    // Delete 2 items from page 1
    const toDelete = [createdIds[14], createdIds[13]];
    const delRes = await apiRequest("/notifications/bulk-delete", "POST", tokenA, {
      notificationIds: toDelete,
    });
    assert.strictEqual(delRes.status, 200);

    // Fetch page 1 again
    const page1After = await apiRequest(`/notifications/recipient/${userAId}?page=1&limit=5`, "GET", tokenA);
    assert.strictEqual(page1After.body.pagination.total, 13);
    const page1Ids = page1After.body.data.map((n) => n.id);
    assert.ok(!page1Ids.includes(toDelete[0]));
    assert.ok(!page1Ids.includes(toDelete[1]));

    // Fetch page 2
    const page2 = await apiRequest(`/notifications/recipient/${userAId}?page=2&limit=5`, "GET", tokenA);
    assert.strictEqual(page2.body.data.length, 5);
    const page2Ids = page2.body.data.map((n) => n.id);
    assert.ok(!page2Ids.includes(toDelete[0]));
    assert.ok(!page2Ids.includes(toDelete[1]));
  });

  await t.test("8. New notifications created after deletion still arrive normally", async () => {
    // Clear all existing
    await apiRequest("/notifications/all", "DELETE", tokenA);
    const emptyCheck = await apiRequest(`/notifications/recipient/${userAId}`, "GET", tokenA);
    assert.strictEqual(emptyCheck.body.data.length, 0);

    // Create a new notification
    const newNotif = await notificationModel.createNotification({
      recipientId: userAId,
      notificationType: "ORDER_CREATED",
      title: "Fresh Notification",
      message: "Arrived after deletion",
    });

    // Check that it arrives
    const getNew = await apiRequest(`/notifications/recipient/${userAId}`, "GET", tokenA);
    assert.strictEqual(getNew.body.data.length, 1);
    assert.strictEqual(getNew.body.data[0].id, newNotif.id);
    assert.strictEqual(getNew.body.data[0].title, "Fresh Notification");
  });
});
