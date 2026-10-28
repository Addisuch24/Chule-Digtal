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
    VALUES ($1, $2, $3, $4, CURRENT_TIMESTAMP)
    ON CONFLICT (device_token) DO UPDATE SET
      user_id = EXCLUDED.user_id,
      platform = EXCLUDED.platform,
      receive_logged_out = EXCLUDED.receive_logged_out,
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
    WHERE device_token = $1
  `;

  const result = await pool.query(query, [deviceToken.trim()]);
  return result.rowCount > 0;
}

async function unregisterDevice({ deviceToken }) {
  if (!deviceToken) return false;

  const query = `
    DELETE FROM device_registrations
    WHERE device_token = $1
  `;

  const result = await pool.query(query, [deviceToken.trim()]);
  return result.rowCount > 0;
}

async function getDevicesForUser(userId) {
  if (!userId) return [];

  const query = `
    SELECT device_token, platform, receive_logged_out
    FROM device_registrations
    WHERE user_id = $1
  `;

  const result = await pool.query(query, [Number(userId)]);
  return result.rows;
}

async function getLoggedOutDevices() {
  const query = `
    SELECT device_token, platform
    FROM device_registrations
    WHERE user_id IS NULL AND receive_logged_out = 1
  `;

  const result = await pool.query(query);
  return result.rows;
}

module.exports = {
  registerDevice,
  disassociateUser,
  unregisterDevice,
  getDevicesForUser,
  getLoggedOutDevices,
};
