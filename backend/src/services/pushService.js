/**
 * Push notification delivery service for Android (FCM / background push).
 * Ensures payloads never expose sensitive passwords, access tokens, or financial records.
 */
let firebaseAdmin = null;

try {
  // If firebase-admin is installed and configured via service account or GOOGLE_APPLICATION_CREDENTIALS
  if (process.env.FIREBASE_CONFIG || process.env.GOOGLE_APPLICATION_CREDENTIALS) {
    const admin = require("firebase-admin");
    if (!admin.apps.length) {
      admin.initializeApp();
    }
    firebaseAdmin = admin;
  }
} catch (e) {
  // Graceful fallback if firebase-admin package is not present in local dev
  firebaseAdmin = null;
}

/**
 * Dispatches a push notification to target device tokens.
 *
 * @param {Array<string>} tokens - Array of device registration tokens.
 * @param {Object} payload - Notification data containing validated routing metadata.
 */
async function sendPushNotification(tokens, payload) {
  if (!tokens || tokens.length === 0) return { successCount: 0, failureCount: 0 };

  // Sanitize payload: routing metadata only. Never sensitive tokens or passwords.
  const pushData = {
    notification_id: String(payload.id || ""),
    notification_type: String(payload.notificationType || payload.notification_type || "SYSTEM"),
    entity_type: String(payload.entityType || payload.entity_type || ""),
    entity_id: String(payload.entityId || payload.entity_id || ""),
    activity_id: String(payload.activityId || payload.activity_id || ""),
    title: String(payload.title || "Business Update"),
    message: String(payload.message || ""),
    created_at: String(payload.createdAt || payload.created_at || new Date().toISOString()),
  };

  if (firebaseAdmin) {
    try {
      const message = {
        tokens,
        data: pushData,
        notification: {
          title: pushData.title,
          body: pushData.message,
        },
        android: {
          priority: "high",
          notification: {
            channelId: "chule_digital_updates_channel",
            clickAction: "FLUTTER_NOTIFICATION_CLICK", // Or default activity intent
          },
        },
      };

      const response = await firebaseAdmin.messaging().sendEachForMulticast(message);
      return {
        successCount: response.successCount,
        failureCount: response.failureCount,
      };
    } catch (error) {
      console.warn("[PUSH SERVICE] FCM delivery error:", error.message);
      return { successCount: 0, failureCount: tokens.length, error: error.message };
    }
  } else {
    // In local development or environments without FCM credentials, log payload delivery
    if (process.env.NODE_ENV !== "test") {
      console.log(`[PUSH SERVICE (Local/Simulated)] Dispatched push payload to ${tokens.length} device(s):`, {
        notification_id: pushData.notification_id,
        notification_type: pushData.notification_type,
        entity_type: pushData.entity_type,
        entity_id: pushData.entity_id,
      });
    }
    return { successCount: tokens.length, failureCount: 0 };
  }
}

module.exports = {
  sendPushNotification,
};
