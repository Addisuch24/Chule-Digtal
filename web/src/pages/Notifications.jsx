import { useEffect, useState } from "react";
import api from "../services/api";
import socket from "../services/socket";

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

async function loadNotifications() {
  try {
    const storedUser = localStorage.getItem("user");

    if (!storedUser) {
      setError("User information is missing. Please login again.");
      return;
    }

    const user = JSON.parse(storedUser);
    const userId = user?.id;

    if (!userId) {
      setError("Valid user ID is missing. Please login again.");
      return;
    }

    const response = await api.get(
      `/notifications/recipient/${userId}`
    );

    setNotifications(response.data?.data || []);
  } catch (error) {
    console.error("Load notifications error:", error);

    setError(
      error.response?.data?.message ||
        "Failed to load notifications"
    );
  } finally {
    setLoading(false);
  }
}

 useEffect(() => {
  function handleSocketConnect() {
    console.log("Notifications socket connected:", socket.id);
  }

  function handleSocketDisconnect(reason) {
    console.log("Notifications socket disconnected:", reason);
  }

 function handleNewNotification(notification) {
  console.log(
    "REAL-TIME NOTIFICATION RECEIVED:",
    notification
  );

  console.log(
    "REAL-TIME NOTIFICATION JSON:",
    JSON.stringify(notification, null, 2)
  );

  const safeNotification = {
    id: notification?.id ?? Date.now(),
    title: notification?.title ?? "New notification",
    message:
      notification?.message ??
      notification?.description ??
      "A new activity was recorded.",
    is_read:
      notification?.is_read ??
      notification?.isRead ??
      false,
    created_at:
      notification?.created_at ??
      notification?.createdAt ??
      new Date().toISOString(),
  };

  setNotifications((previousNotifications) => [
    safeNotification,
    ...previousNotifications,
  ]);
}

  socket.on("connect", handleSocketConnect);
  socket.on("disconnect", handleSocketDisconnect);
  socket.on("notification:new", handleNewNotification);

  if (!socket.connected) {
    socket.connect();
  }

  return () => {
    socket.off("connect", handleSocketConnect);
    socket.off("disconnect", handleSocketDisconnect);
    socket.off("notification:new", handleNewNotification);
  };
}, []);

  useEffect(() => {
 function handleNewNotification(notification) {
  console.log(
    "REAL-TIME NOTIFICATION RECEIVED:",
    JSON.stringify(notification, null, 2)
  );

  setNotifications((previousNotifications) => [
    notification,
    ...previousNotifications,
  ]);
}

    socket.on(
      "notification:new",
      handleNewNotification
    );

    return () => {
      socket.off(
        "notification:new",
        handleNewNotification
      );
    };
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
      console.error(
        "Mark notification read error:",
        requestError
      );

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
        View updates from other family members in real time.
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