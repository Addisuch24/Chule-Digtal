const deviceRegistrationModel = require("../models/deviceRegistrationModel");

async function registerDevice(req, res) {
  try {
    const { deviceToken, platform, receiveLoggedOut } = req.body;
    // Link to authenticated user if available, or allow explicit userId in payload, or null if logged out
    const userId = req.user ? req.user.id : (req.body.userId || null);

    if (!deviceToken || typeof deviceToken !== "string" || !deviceToken.trim()) {
      return res.status(400).json({
        success: false,
        message: "A valid deviceToken is required",
      });
    }

    const registration = await deviceRegistrationModel.registerDevice({
      deviceToken: deviceToken.trim(),
      userId,
      platform: platform || "android",
      receiveLoggedOut: receiveLoggedOut !== false,
    });

    return res.status(200).json({
      success: true,
      message: "Device registered successfully for push notifications",
      data: registration,
    });
  } catch (error) {
    console.error("Device registration error:", error.message);
    return res.status(500).json({
      success: false,
      message: "Failed to register device: " + error.message,
    });
  }
}

async function logoutDevice(req, res) {
  try {
    const { deviceToken } = req.body;

    if (!deviceToken || typeof deviceToken !== "string") {
      return res.status(400).json({
        success: false,
        message: "deviceToken is required for logout",
      });
    }

    // Disassociate the authenticated user account without disabling logged-out push
    await deviceRegistrationModel.disassociateUser({ deviceToken: deviceToken.trim() });

    return res.status(200).json({
      success: true,
      message: "User session disassociated from device; logged-out delivery retained",
    });
  } catch (error) {
    console.error("Device logout error:", error.message);
    return res.status(500).json({
      success: false,
      message: "Failed to disassociate device: " + error.message,
    });
  }
}

async function unregisterDevice(req, res) {
  try {
    const deviceToken = req.params.deviceToken || req.body.deviceToken;

    if (!deviceToken) {
      return res.status(400).json({
        success: false,
        message: "deviceToken is required",
      });
    }

    await deviceRegistrationModel.unregisterDevice({ deviceToken: deviceToken.trim() });

    return res.status(200).json({
      success: true,
      message: "Device unregistered successfully",
    });
  } catch (error) {
    console.error("Device unregister error:", error.message);
    return res.status(500).json({
      success: false,
      message: "Failed to unregister device: " + error.message,
    });
  }
}

module.exports = {
  registerDevice,
  logoutDevice,
  unregisterDevice,
};
