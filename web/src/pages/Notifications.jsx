import { useCallback, useEffect, useMemo, useState } from "react";
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
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [socketState, setSocketState] = useState(
    socket.connected ? "connected" : "connecting"
  );

  const unreadCount = useMemo(
    () =>
      notifications.filter(
        (notification) => !isRead(notification)
      ).length,
    [notifications]
  );

  const loadNotifications = useCallback(async (showSpinner = false) => {
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
      const response = await api.get(`/notifications/recipient/${user.id}`);
      const list = Array.isArray(response.data?.data)
        ? response.data.data
        : [];
      setNotifications(list.map(normalizeNotification));
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
      const safeNotification = normalizeNotification(notification);
      setNotifications((previousNotifications) =>
        safeNotification.id &&
        previousNotifications.some((item) => item.id === safeNotification.id)
          ? previousNotifications
          : [safeNotification, ...previousNotifications]
      );
    }

    socket.on("connect", handleSocketConnect);
    socket.on("disconnect", handleSocketDisconnect);
    socket.on("connect_error", handleSocketError);
    socket.on("notification:new", handleNewNotification);

    connectSocket();
    loadNotifications();

    return () => {
      socket.off("connect", handleSocketConnect);
      socket.off("disconnect", handleSocketDisconnect);
      socket.off("connect_error", handleSocketError);
      socket.off("notification:new", handleNewNotification);
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

  return (
    <div className="page-stack">
      <PageHeader
        eyebrow="Shared updates"
        title="Notifications"
        description="Stay close to activity from every family member."
        action={<span className="count-label">{unreadCount} unread</span>}
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
        <section className="notification-list">
          {notifications.map((notification) => {
            const read = isRead(notification);
            return (
              <article
                className={`notification-card ${
                  read ? "notification-read" : "notification-unread"
                }`}
                key={notification.id}
              >
                <div className="notification-indicator" aria-hidden="true" />
                <div className="notification-content">
                  <div className="notification-topline">
                    <StatusBadge value={read ? "READ" : "UNREAD"} />
                    <span className="notification-type">
                      {notification.notification_type}
                    </span>
                    <time>{formatDate(notification.created_at)}</time>
                  </div>
                  <h2>{notification.title}</h2>
                  <p>{notification.message}</p>
                  {!read && (
                    <button
                      className="button-secondary"
                      type="button"
                      onClick={() => markAsRead(notification.id)}
                    >
                      Mark as read
                    </button>
                  )}
                </div>
              </article>
            );
          })}
        </section>
      )}
    </div>
  );
}

export default Notifications;

