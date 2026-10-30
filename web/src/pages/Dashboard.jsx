import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import api from "../services/api";
import FeedbackState from "../components/FeedbackState";
import PageHeader from "../components/PageHeader";
import DashboardCharts from "../components/DashboardCharts";
import socket, { connectSocket } from "../services/socket";

const formatAmount = (value) =>
  `${Number(value || 0).toLocaleString("en-US")} ብር`;
const formatDate = (value) =>
  value
    ? new Date(value).toLocaleString("en-US", {
        dateStyle: "medium",
      })
    : "Date unavailable";

function getNoteTitle(note) {
  return String(note.note_text || "").split(/\r?\n/, 1)[0] || "Untitled note";
}

function Dashboard() {
  const [dashboard, setDashboard] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const loadDashboard = useCallback(async (showSpinner = false) => {
    try {
      if (showSpinner) setLoading(true);
      setError("");
      const response = await api.get("/dashboard");
      setDashboard(response.data?.data || {});
    } catch (requestError) {
      console.error("Load dashboard error:", requestError);
      setError(
        requestError.response?.data?.message ||
          "We could not load the dashboard."
      );
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    connectSocket();
    loadDashboard(false);

    function handleDashboardRefresh() {
      loadDashboard(false);
    }

    function handleVisibilityChange() {
      if (document.visibilityState === "visible") {
        loadDashboard(false);
      }
    }

    socket.on("dashboard:refresh", handleDashboardRefresh);
    socket.on("customer:deleted", handleDashboardRefresh);
    socket.on("customer:created", handleDashboardRefresh);
    socket.on("customer:updated", handleDashboardRefresh);
    socket.on("notification:new", handleDashboardRefresh);

    window.addEventListener("focus", handleDashboardRefresh);
    document.addEventListener("visibilitychange", handleVisibilityChange);

    return () => {
      socket.off("dashboard:refresh", handleDashboardRefresh);
      socket.off("customer:deleted", handleDashboardRefresh);
      socket.off("customer:created", handleDashboardRefresh);
      socket.off("customer:updated", handleDashboardRefresh);
      socket.off("notification:new", handleDashboardRefresh);

      window.removeEventListener("focus", handleDashboardRefresh);
      document.removeEventListener("visibilitychange", handleVisibilityChange);
    };
  }, [loadDashboard]);

  if (loading)
    return (
      <FeedbackState
        type="loading"
        title="Loading your overview"
        message="Gathering the latest shared records..."
      />
    );
  if (error)
    return (
      <FeedbackState
        type="error"
        title="Dashboard unavailable"
        message={error}
        action={
          <button type="button" onClick={loadDashboard}>
            Try again
          </button>
        }
      />
    );

  const customers = dashboard?.customers || {};
  const orders = dashboard?.orders || {};
  const notes = dashboard?.notes || {};
  const financial = dashboard?.financialMovements || {};
  const recentOrders = Array.isArray(dashboard?.recentOrders)
    ? dashboard.recentOrders
    : (Array.isArray(dashboard?.activities?.recentOrders) ? dashboard.activities.recentOrders : []);
  const recentNotes = Array.isArray(dashboard?.recentNotes)
    ? dashboard.recentNotes
    : (Array.isArray(dashboard?.activities?.recentNotes) ? dashboard.activities.recentNotes : []);
  const currentBalance = Number(financial.balance || 0);

  return (
    <div className="page-stack">
      <PageHeader
        eyebrow="Executive Overview"
        title="Dashboard"
        description="Your shared notes, orders, and customer balances in one place."
        theme="dashboard"
      />
      <div className="metric-grid dashboard-metrics">
        <Link className="metric-card dashboard-metric-link" to="/notes">
          <span>Shared notes</span>
          <strong>{notes.total || 0}</strong>
          <small>Open your notes</small>
        </Link>
        <Link className="metric-card dashboard-metric-link" to="/orders">
          <span>Orders</span>
          <strong>{orders.total || 0}</strong>
          <small>View the order list</small>
        </Link>
        <Link className="metric-card dashboard-metric-link" to="/finance">
          <span>Customers</span>
          <strong>{customers.total || 0}</strong>
          <small>See customer balances</small>
        </Link>
        <Link
          className="metric-card dashboard-metric-link"
          to="/finance"
        >
          <span>Current balance</span>
          <strong
            className={
              currentBalance < 0 ? "balance-negative" : "balance-positive"
            }
          >
            {currentBalance > 0 ? "+" : currentBalance < 0 ? "−" : ""}
            {formatAmount(Math.abs(currentBalance))}
          </strong>
          <small>Across all customers</small>
        </Link>
      </div>

      <DashboardCharts
        notes={notes}
        orders={orders}
        customers={customers}
        financial={financial}
      />

      <div className="dashboard-columns">
        <section className="panel">
          <div className="section-heading">
            <div>
              <p className="eyebrow">Work list</p>
              <h2>Recent orders</h2>
            </div>
            <Link to="/orders">View all</Link>
          </div>
          {recentOrders.length === 0 ? (
            <FeedbackState
              title="No orders yet"
              message="Your orders will appear here."
              action={
                <Link className="button button-secondary" to="/orders">
                  Open orders
                </Link>
              }
            />
          ) : (
            recentOrders.map((order) => (
              <Link
                className="list-row dashboard-record-row"
                to={`/orders/${order.id}`}
                key={order.id}
              >
                <div>
                  <strong>{order.title || `Order #${order.id}`}</strong>
                  <small>{formatDate(order.created_at)}</small>
                </div>
                <span aria-hidden="true">-&gt;</span>
              </Link>
            ))
          )}
        </section>

        <section className="panel">
          <div className="section-heading">
            <div>
              <p className="eyebrow">Shared notebook</p>
              <h2>Recent notes</h2>
            </div>
            <Link to="/notes">View all</Link>
          </div>
          {recentNotes.length === 0 ? (
            <FeedbackState
              title="No notes yet"
              message="Your shared notes will appear here."
              action={
                <Link className="button button-secondary" to="/notes">
                  Open notes
                </Link>
              }
            />
          ) : (
            recentNotes.map((note) => (
              <Link
                className="list-row dashboard-record-row"
                to="/notes"
                key={note.id}
              >
                <div>
                  <strong>{getNoteTitle(note)}</strong>
                  <small>{formatDate(note.updated_at || note.created_at)}</small>
                </div>
                <span aria-hidden="true">-&gt;</span>
              </Link>
            ))
          )}
        </section>
      </div>
    </div>
  );
}

export default Dashboard;
