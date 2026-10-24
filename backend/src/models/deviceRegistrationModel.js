const pool = require("../config/db");

async function registerDevice({
  deviceToken,
  userId = null,
  platform = "android",
  receiveLoggedOut = true,
}) {
  if (!deviceToken || typeof deviceToken !== "string") {
    throw new Error("deviceToken is required and must be a string");
  }

  const query = `
    INSERT INTO device_registrations (
      device_token,
      user_id,
      platform,
      receive_logged_out,
      last_active
    )
    VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)
    ON DUPLICATE KEY UPDATE
      user_id = VALUES(user_id),
      platform = VALUES(platform),
      receive_logged_out = VALUES(receive_logged_out),
      last_active = CURRENT_TIMESTAMP
  `;

  await pool.query(query, [
    deviceToken.trim(),
    userId ? Number(userId) : null,
    platform || "android",
    receiveLoggedOut ? 1 : 0,
  ]);

  return {
    deviceToken: deviceToken.trim(),
    userId: userId ? Number(userId) : null,
    platform: platform || "android",
    receiveLoggedOut: Boolean(receiveLoggedOut),
  };
}

async function disassociateUser({ deviceToken }) {
  if (!deviceToken) return false;

  const query = `
    UPDATE device_registrations
    SET user_id = NULL,
        last_active = CURRENT_TIMESTAMP
    WHERE device_token = ?
  `;

  const [result] = await pool.query(query, [deviceToken.trim()]);
  return result.affectedRows > 0;
}

async function unregisterDevice({ deviceToken }) {
  if (!deviceToken) return false;

  const query = `
    DELETE FROM device_registrations
    WHERE device_token = ?
  `;

  const [result] = await pool.query(query, [deviceToken.trim()]);
  return result.affectedRows > 0;
}

async function getDevicesForUser(userId) {
  if (!userId) return [];

  const query = `
    SELECT device_token, platform, receive_logged_out
    FROM device_registrations
    WHERE user_id = ?
  `;

  const [rows] = await pool.query(query, [Number(userId)]);
  return rows;
}

async function getLoggedOutDevices() {
  const query = `
    SELECT device_token, platform
    FROM device_registrations
    WHERE user_id IS NULL AND receive_logged_out = 1
  `;

  const [rows] = await pool.query(query);
  return rows;
}

module.exports = {
  registerDevice,
  disassociateUser,
  unregisterDevice,
  getDevicesForUser,
  getLoggedOutDevices,
};
