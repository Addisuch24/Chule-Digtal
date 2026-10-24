import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import api from "../services/api";
import socket, { connectSocket } from "../services/socket";
import FeedbackState from "../components/FeedbackState";
import PageHeader from "../components/PageHeader";
import StatusBadge from "../components/StatusBadge";

const formatDate = (value) =>
  value
    ? new Date(value).toLocaleString("en-US", {
        dateStyle: "medium",
        timeStyle: "short",
      })
    : "Date unavailable";

function normalizeNotification(notification) {
  if (!notification) return {};
  return {
    ...notification,
    id: notification.id ?? notification.notification_id ?? notification._id,
    title: notification.title || "New notification",
    message:
      notification.message ||
      notification.description ||
      "A new activity was recorded.",
    notification_type:
      notification.notification_type ||
      notification.notificationType ||
      "GENERAL",
    is_read:
      notification.is_read === 1 ||
      notification.is_read === true ||
      notification.isRead === true ||
      Boolean(notification.read_at),
    created_at:
      notification.created_at ||
      notification.createdAt ||
      new Date().toISOString(),
    startTime: notification.startTime || notification.start_time || null,
  };
}

function isRead(notification) {
  return (
    notification?.is_read === 1 ||
    notification?.is_read === true ||
    notification?.isRead === true ||
    notification?.read_at !== null && notification?.read_at !== undefined ||
    notification?.status === "READ"
  );
}

function Notifications() {
  const [notifications, setNotifications] = useState([]);
  const [selectedNotificationIds, setSelectedNotificationIds] = useState([]);
  const [loading, setLoading] = useState(true);
  const [deleting, setDeleting] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [socketState, setSocketState] = useState(
    socket.connected ? "connected" : "connecting"
  );

  const deletedNotificationIds = useRef(new Set());
  const [currentPage, setCurrentPage] = useState(1);
  const [pagination, setPagination] = useState({
    page: 1,
    limit: 50,
    total: 0,
    totalPages: 1,
    hasMore: false,
  });

  const unreadCount = useMemo(
    () =>
      notifications.filter(
        (notification) => !isRead(notification)
      ).length,
    [notifications]
  );

  const loadNotifications = useCallback(async (showSpinner = false, page = 1) => {
    try {
      if (showSpinner) setLoading(true);
      setError("");
      const storedUser = localStorage.getItem("user");
      const user = storedUser ? JSON.parse(storedUser) : null;
      if (!user?.id) {
        setError("Your member session is missing. Please log in again.");
        setLoading(false);
        return;
      }
      const response = await api.get(`/notifications/recipient/${user.id}?page=${page}&limit=50`);
      const list = Array.isArray(response.data?.data)
        ? response.data.data
        : [];
      
      const normalizedList = list
        .map(normalizeNotification)
        .filter((item) => !deletedNotificationIds.current.has(item.id) && !deletedNotificationIds.current.has(Number(item.id)));

      setNotifications(normalizedList);
      if (response.data?.pagination) {
        setPagination(response.data.pagination);
        setCurrentPage(response.data.pagination.page);
      }
    } catch (requestError) {
      console.error("Load notifications error:", requestError);
      setError(
        requestError.response?.data?.message ||
          "We could not load notifications."
      );
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    function handleSocketConnect() {
      setSocketState("connected");
    }
    function handleSocketDisconnect() {
      setSocketState("disconnected");
    }
    function handleSocketError(socketError) {
      setSocketState("error");
      console.error("Notifications socket error:", socketError);
    }
    function handleNewNotification(notification) {
      console.log("REAL-TIME NOTIFICATION RECEIVED:", notification);
      const storedUser = localStorage.getItem("user");
      const user = storedUser ? JSON.parse(storedUser) : null;
      const recipientId = notification.recipientId ?? notification.recipient_id;
      if (user?.id && recipientId && Number(recipientId) !== Number(user.id)) {
        return;
      }

      const safeNotification = normalizeNotification(notification);
      if (
        safeNotification.id &&
        (deletedNotificationIds.current.has(safeNotification.id) ||
         deletedNotificationIds.current.has(Number(safeNotification.id)))
      ) {
        return;
      }

      setNotifications((previousNotifications) =>
        safeNotification.id &&
        previousNotifications.some((item) => item.id === safeNotification.id)
          ? previousNotifications
          : [safeNotification, ...previousNotifications]
      );
    }

    function handleNotificationDeleted({ notificationId }) {
      if (!notificationId) return;
      deletedNotificationIds.current.add(Number(notificationId));
      deletedNotificationIds.current.add(String(notificationId));
      setNotifications((prev) =>
        prev.filter((n) => n.id !== notificationId && Number(n.id) !== Number(notificationId))
      );
      setSelectedNotificationIds((prev) =>
        prev.filter((id) => id !== notificationId && Number(id) !== Number(notificationId))
      );
    }

    function handleNotificationBulkDeleted({ notificationIds }) {
      if (!Array.isArray(notificationIds)) return;
      notificationIds.forEach((id) => {
        deletedNotificationIds.current.add(Number(id));
        deletedNotificationIds.current.add(String(id));
      });
      setNotifications((prev) =>
        prev.filter(
          (n) => !notificationIds.includes(n.id) && !notificationIds.includes(Number(n.id))
        )
      );
      setSelectedNotificationIds((prev) =>
        prev.filter(
          (id) => !notificationIds.includes(id) && !notificationIds.includes(Number(id))
        )
      );
    }

    function handleNotificationAllDeleted() {
      setNotifications([]);
      setSelectedNotificationIds([]);
    }

    socket.on("connect", handleSocketConnect);
    socket.on("disconnect", handleSocketDisconnect);
    socket.on("connect_error", handleSocketError);
    socket.on("notification:new", handleNewNotification);
    socket.on("notification:deleted", handleNotificationDeleted);
    socket.on("notification:bulk_deleted", handleNotificationBulkDeleted);
    socket.on("notification:all_deleted", handleNotificationAllDeleted);

    connectSocket();
    loadNotifications(true, 1);

    return () => {
      socket.off("connect", handleSocketConnect);
      socket.off("disconnect", handleSocketDisconnect);
      socket.off("connect_error", handleSocketError);
      socket.off("notification:new", handleNewNotification);
      socket.off("notification:deleted", handleNotificationDeleted);
      socket.off("notification:bulk_deleted", handleNotificationBulkDeleted);
      socket.off("notification:all_deleted", handleNotificationAllDeleted);
    };
  }, [loadNotifications]);

  async function markAsRead(notificationId) {
    if (!notificationId) return;
    try {
      setMessage("");
      setError("");
      await api.patch(`/notifications/${notificationId}/read`);
      setNotifications((previousNotifications) =>
        previousNotifications.map((notification) =>
          notification.id === notificationId
            ? { ...notification, is_read: true }
            : notification
        )
      );
      setMessage("Notification marked as read.");
    } catch (requestError) {
      console.error("Mark notification read error:", requestError);
      setError(
        requestError.response?.data?.message ||
          "We could not mark that notification as read."
      );
    }
  }

  function handleToggleSelectNotification(id, event) {
    event.stopPropagation();
    setSelectedNotificationIds((prev) =>
      prev.includes(id) ? prev.filter((item) => item !== id) : [...prev, id]
    );
  }

  function handleSelectAllNotifications() {
    if (selectedNotificationIds.length === notifications.length) {
      setSelectedNotificationIds([]);
    } else {
      setSelectedNotificationIds(notifications.map((n) => n.id));
    }
  }

  async function handleDeleteNotification(id) {
    if (!window.confirm("Delete this notification?")) return;
    try {
      setDeleting(true);
      setMessage("");
      setError("");
      await api.delete(`/notifications/${id}`);
      deletedNotificationIds.current.add(id);
      deletedNotificationIds.current.add(Number(id));
      setNotifications((prev) =>
        prev.filter((n) => n.id !== id && Number(n.id) !== Number(id))
      );
      setSelectedNotificationIds((prev) =>
        prev.filter((item) => item !== id && Number(item) !== Number(id))
      );
      setMessage("Notification deleted.");
      await loadNotifications(false, currentPage);
    } catch (requestError) {
      console.error("Delete notification error:", requestError);
      setError(
        requestError.response?.data?.message || "Failed to delete notification."
      );
    } finally {
      setDeleting(false);
    }
  }

  async function handleBulkDeleteNotifications() {
    if (selectedNotificationIds.length === 0) return;
    if (
      !window.confirm(
        `Delete ${selectedNotificationIds.length} selected notification(s)?`
      )
    ) {
      return;
    }
    try {
      setDeleting(true);
      setMessage("");
      setError("");
      const response = await api.post("/notifications/bulk-delete", {
        notificationIds: selectedNotificationIds,
      });
      const actuallyDeletedIds = response.data?.data?.deletedIds || selectedNotificationIds;
      actuallyDeletedIds.forEach((id) => {
        deletedNotificationIds.current.add(id);
        deletedNotificationIds.current.add(Number(id));
      });
      setNotifications((prev) =>
        prev.filter(
          (n) => !actuallyDeletedIds.includes(n.id) && !actuallyDeletedIds.includes(Number(n.id))
        )
      );
      setSelectedNotificationIds((prev) =>
        prev.filter(
          (id) => !actuallyDeletedIds.includes(id) && !actuallyDeletedIds.includes(Number(id))
        )
      );
      setMessage(
        `Successfully deleted ${actuallyDeletedIds.length} notification(s).`
      );
      await loadNotifications(false, currentPage);
    } catch (requestError) {
      console.error("Bulk delete notifications error:", requestError);
      setError(
        requestError.response?.data?.message || "Failed to delete notifications."
      );
    } finally {
      setDeleting(false);
    }
  }

  async function handleClearAllNotifications() {
    if (
      !window.confirm(
        "Are you sure you want to permanently delete ALL notifications? This will delete them from the database and cannot be undone."
      )
    ) {
      return;
    }
    try {
      setDeleting(true);
      setMessage("");
      setError("");
      await api.delete("/notifications/all");
      setNotifications([]);
      setSelectedNotificationIds([]);
      setMessage("All notifications permanently deleted.");
      await loadNotifications(false, 1);
    } catch (requestError) {
      console.error("Delete all notifications error:", requestError);
      setError(
        requestError.response?.data?.message || "Failed to clear all notifications."
      );
    } finally {
      setDeleting(false);
    }
  }

  return (
    <div className="page-stack">
      <PageHeader
        eyebrow="Shared updates"
        title="Notifications"
        description="Stay close to activity from every family member."
        action={<span className="count-label">{unreadCount} unread</span>}
        theme="notifications"
      />
      <div className={`connection-strip connection-${socketState}`}>
        <span className="online-dot" />
        <span>Real-time updates are {socketState}.</span>
      </div>

      {message && <p className="alert alert-success">{message}</p>}

      {error && (
        <FeedbackState
          type="error"
          title="Notifications unavailable"
          message={error}
          action={
            <button type="button" onClick={loadNotifications}>
              Try again
            </button>
          }
        />
      )}

      {!error && loading && (
        <FeedbackState
          type="loading"
          title="Loading notifications"
          message="Checking for the latest shared updates..."
        />
      )}

      {!error && !loading && notifications.length === 0 && (
        <FeedbackState
          title="You are all caught up"
          message="New activity from the family will appear here automatically."
        />
      )}

      {!error && !loading && notifications.length > 0 && (
        <>
          <div
            style={{
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
              marginBottom: "1rem",
              flexWrap: "wrap",
              gap: "0.5rem",
            }}
          >
            <label style={{ display: "flex", alignItems: "center", gap: "0.5rem", cursor: "pointer" }}>
              <input
                type="checkbox"
                checked={
                  selectedNotificationIds.length > 0 &&
                  selectedNotificationIds.length === notifications.length
                }
                onChange={handleSelectAllNotifications}
                aria-label="Select all notifications"
                style={{ width: "18px", height: "18px", cursor: "pointer" }}
              />
              <span style={{ fontSize: "0.9rem", fontWeight: 500 }}>Select all notifications</span>
            </label>
            <button
              type="button"
              className="button button-secondary"
              style={{
                color: "#ef4444",
                borderColor: "rgba(239, 68, 68, 0.4)",
                padding: "0.35rem 0.8rem",
                borderRadius: "6px",
                cursor: "pointer",
                fontSize: "0.85rem",
                fontWeight: 600,
              }}
              onClick={handleClearAllNotifications}
              disabled={deleting}
            >
              {deleting ? "Clearing..." : "Clear All Notifications"}
            </button>
          </div>

          {selectedNotificationIds.length > 0 && (
            <div
              className="bulk-action-bar"
              style={{
                display: "flex",
                justifyContent: "space-between",
                alignItems: "center",
                padding: "0.6rem 1rem",
                marginBottom: "1rem",
                background: "rgba(239, 68, 68, 0.12)",
                border: "1px solid rgba(239, 68, 68, 0.3)",
                borderRadius: "8px",
              }}
            >
              <span>
                <strong>{selectedNotificationIds.length}</strong> notification(s) selected
              </span>
              <div style={{ display: "flex", gap: "0.5rem" }}>
                <button
                  type="button"
                  className="button button-danger"
                  style={{
                    backgroundColor: "#ef4444",
                    color: "#fff",
                    border: "none",
                    padding: "0.4rem 0.8rem",
                    borderRadius: "6px",
                    cursor: "pointer",
                    fontWeight: 600,
                  }}
                  onClick={handleBulkDeleteNotifications}
                  disabled={deleting}
                >
                  {deleting ? "Deleting..." : `Delete Selected (${selectedNotificationIds.length})`}
                </button>
                <button
                  type="button"
                  className="button button-secondary"
                  style={{ padding: "0.4rem 0.8rem", borderRadius: "6px" }}
                  onClick={() => setSelectedNotificationIds([])}
                >
                  Clear
                </button>
              </div>
            </div>
          )}

          <section className="notification-list">
            {notifications.map((notification) => {
              const read = isRead(notification);
              const isSelected = selectedNotificationIds.includes(notification.id);
              return (
                <article
                  className={`notification-card ${
                    read ? "notification-read" : "notification-unread"
                  } ${isSelected ? "notification-selected" : ""}`}
                  key={notification.id}
                  style={{
                    display: "flex",
                    alignItems: "flex-start",
                    border: isSelected ? "1px solid rgba(239, 68, 68, 0.5)" : undefined,
                  }}
                >
                  <div style={{ padding: "1.25rem 0.5rem 0 1rem" }}>
                    <input
                      type="checkbox"
                      checked={isSelected}
                      onChange={(e) => handleToggleSelectNotification(notification.id, e)}
                      aria-label={`Select notification ${notification.title}`}
                      style={{ cursor: "pointer", width: "18px", height: "18px" }}
                    />
                  </div>
                  <div className="notification-indicator" aria-hidden="true" />
                  <div className="notification-content" style={{ flex: 1 }}>
                    <div className="notification-topline">
                      <StatusBadge value={read ? "READ" : "UNREAD"} />
                      <span className="notification-type">
                        {notification.notification_type}
                      </span>
                      <time>{formatDate(notification.created_at)}</time>
                    </div>
                    <h2>{notification.title}</h2>
                    <p>{notification.message}</p>
                    <div style={{ display: "flex", gap: "0.5rem", marginTop: "0.5rem" }}>
                      {!read && (
                        <button
                          className="button-secondary"
                          type="button"
                          onClick={() => markAsRead(notification.id)}
                        >
                          Mark as read
                        </button>
                      )}
                      <button
                        className="button-secondary"
                        type="button"
                        style={{ color: "#ef4444" }}
                        onClick={() => handleDeleteNotification(notification.id)}
                        disabled={deleting}
                      >
                        Delete
                      </button>
                    </div>
                  </div>
                </article>
              );
            })}
          </section>

          {pagination.totalPages > 1 && (
            <div
              className="pagination-bar"
              style={{
                display: "flex",
                justifyContent: "space-between",
                alignItems: "center",
                padding: "1rem",
                marginTop: "1.5rem",
                background: "var(--color-surface, #ffffff)",
                borderRadius: "8px",
                border: "1px solid var(--color-border, #e2e8f0)",
              }}
            >
              <button
                type="button"
                className="button button-secondary"
                disabled={currentPage <= 1 || loading}
                onClick={() => loadNotifications(true, currentPage - 1)}
                style={{ padding: "0.4rem 0.8rem", cursor: currentPage > 1 ? "pointer" : "not-allowed" }}
              >
                Previous
              </button>
              <span style={{ fontSize: "0.9rem", color: "var(--color-text-muted, #64748b)" }}>
                Page <strong>{currentPage}</strong> of <strong>{pagination.totalPages}</strong> ({pagination.total} total)
              </span>
              <button
                type="button"
                className="button button-secondary"
                disabled={currentPage >= pagination.totalPages || loading}
                onClick={() => loadNotifications(true, currentPage + 1)}
                style={{ padding: "0.4rem 0.8rem", cursor: currentPage < pagination.totalPages ? "pointer" : "not-allowed" }}
              >
                Next
              </button>
            </div>
          )}
        </>
      )}
    </div>
  );
}

export default Notifications;

