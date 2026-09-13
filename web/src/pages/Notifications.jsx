import { useEffect, useState } from "react";
import api from "../services/api";

function formatDate(dateValue) {
  if (!dateValue) return "-";

  return new Date(dateValue).toLocaleString("en-US", {
    dateStyle: "medium",
    timeStyle: "short",
  });
}

function Notifications() {
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  const user = JSON.parse(localStorage.getItem("user") || "null");

  const userId = user?.id;

  async function loadNotifications() {
    try {
      setLoading(true);
      setError("");

      if (!userId) {
        setError("User information not found. Please login again.");
        return;
      }

      const response = await api.get(
        `/notifications/recipient/${userId}`
      );

      const notificationData =
        response.data?.data ||
        response.data?.notifications ||
        [];

      setNotifications(
        Array.isArray(notificationData)
          ? notificationData
          : []
      );
    } catch (requestError) {
      console.error("Notifications loading error:", requestError);

      setError(
        requestError.response?.data?.message ||
          "Failed to load notifications."
      );
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadNotifications();
  }, []);

  async function markAsRead(notificationId) {
    try {
      setMessage("");
      setError("");

      await api.patch(
        `/notifications/${notificationId}/read`
      );

      setMessage("Notification marked as read.");

      await loadNotifications();
    } catch (requestError) {
      console.error("Mark notification read error:", requestError);

      setError(
        requestError.response?.data?.message ||
          "Failed to mark notification as read."
      );
    }
  }

  function isRead(notification) {
    return (
      notification.is_read === 1 ||
      notification.is_read === true ||
      notification.read_at !== null ||
      notification.status === "READ"
    );
  }

  return (
    <div>
      <h2>Notifications</h2>

      <p>
        View updates from other family members.
      </p>

      {message && (
        <p style={{ color: "green" }}>
          {message}
        </p>
      )}

      {error && (
        <p style={{ color: "red" }}>
          {error}
        </p>
      )}

      {loading ? (
        <p>Loading notifications...</p>
      ) : notifications.length === 0 ? (
        <p>No notifications found.</p>
      ) : (
        <div>
          {notifications.map((notification) => {
            const read = isRead(notification);

            return (
              <div
                key={notification.id}
                style={{
                  border: "1px solid #ddd",
                  borderRadius: "8px",
                  padding: "15px",
                  marginBottom: "12px",
                  backgroundColor: read
                    ? "#f8f8f8"
                    : "#fff8dc",
                }}
              >
                <p>
                  <strong>
                    {notification.title ||
                      notification.notification_type ||
                      "Notification"}
                  </strong>
                </p>

                <p>
                  {notification.message ||
                    notification.description ||
                    "No notification message"}
                </p>

                <small>
                  {formatDate(
                    notification.created_at ||
                      notification.createdAt
                  )}
                </small>

                <p>
                  <strong>Status:</strong>{" "}
                  {read ? "Read" : "Unread"}
                </p>

                {!read && (
                  <button
                    onClick={() =>
                      markAsRead(notification.id)
                    }
                  >
                    Mark as Read
                  </button>
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

export default Notifications;