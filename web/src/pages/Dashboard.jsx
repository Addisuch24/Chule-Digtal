import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import api from "../services/api";
import FeedbackState from "../components/FeedbackState";
import PageHeader from "../components/PageHeader";
import StatusBadge from "../components/StatusBadge";

const formatAmount = (value) =>
  `${Number(value || 0).toLocaleString("en-US")} ETB`;
const formatDate = (value) =>
  value
    ? new Date(value).toLocaleString("en-US", {
        dateStyle: "medium",
        timeStyle: "short",
      })
    : "Date unavailable";

function ActivityTitle({ activity }) {
  if (!activity) return "Activity";
  if (activity.customer_name) {
    return (
      <>
        <span>Customer </span>
        <strong className="activity-customer-name">
          {activity.customer_name}
        </strong>
        <span> was created</span>
      </>
    );
  }

  return activity.description || activity.activity_type || "Activity";
}

function Dashboard() {
  const [dashboard, setDashboard] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const user = JSON.parse(localStorage.getItem("user") || "null");

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
    loadDashboard(false);
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
  const pending = dashboard?.pendingVerification || {};
  const financial = dashboard?.financialMovements || {};
  const recentActivities = Array.isArray(dashboard?.recentActivities)
    ? dashboard.recentActivities
    : [];
  const recentOrders = Array.isArray(dashboard?.recentOrders)
    ? dashboard.recentOrders
    : [];

  return (
    <div className="page-stack">
      <PageHeader
        eyebrow="Shared family workspace"
        title={`Good to see you, ${
          user?.full_name || user?.username || "family member"
        }`}
        description="A calm view of what is happening across the business today."
        action={
          <Link className="button" to="/orders">
            Create an order <span aria-hidden="true">-&gt;</span>
          </Link>
        }
      />
      <div className="metric-grid">
        <div className="metric-card">
          <span>Total customers</span>
          <strong>{customers.total || 0}</strong>
          <small>{customers.withBalance || 0} with an open balance</small>
        </div>
        <div className="metric-card">
          <span>Total orders</span>
          <strong>
            {(orders.pending || 0) + (orders.completed || 0)}
          </strong>
          <small>{orders.pending || 0} waiting to be completed</small>
        </div>
        <div className="metric-card">
          <span>Pending verification</span>
          <strong>{pending.total || 0}</strong>
          <small>
            {pending.orders || 0} orders and{" "}
            {pending.financialMovements || 0} movements
          </small>
        </div>
        <div className="metric-card">
          <span>Verified balance</span>
          <strong>{formatAmount(financial.balance)}</strong>
          <small>
            {formatAmount(financial.totalDebit)} debit /{" "}
            {formatAmount(financial.totalCredit)} credit
          </small>
        </div>
        <div className="metric-card">
          <span>Unread notifications</span>
          <strong>{dashboard?.notifications?.unread || 0}</strong>
          <Link to="/notifications">
            Review updates <span aria-hidden="true">-&gt;</span>
          </Link>
        </div>
      </div>

      <div className="dashboard-columns">
        <section className="panel">
          <div className="section-heading">
            <div>
              <p className="eyebrow">Shared timeline</p>
              <h2>Recent activity</h2>
            </div>
            <Link to="/customers">View customers</Link>
          </div>
          {recentActivities.length === 0 ? (
            <FeedbackState
              title="No activity yet"
              message="New customer, order, and finance actions will appear here."
            />
          ) : (
            recentActivities.map((activity) => (
              <div className="list-row" key={activity.id}>
                <div>
                  <strong>
                    <ActivityTitle activity={activity} />
                  </strong>
                  <small>{activity.actor_name || "Family member"}</small>
                </div>
                <time>{formatDate(activity.created_at)}</time>
              </div>
            ))
          )}
        </section>

        <section className="panel">
          <div className="section-heading">
            <div>
              <p className="eyebrow">Latest work</p>
              <h2>Recent orders</h2>
            </div>
            <Link to="/orders">View all</Link>
          </div>
          {recentOrders.length === 0 ? (
            <FeedbackState
              title="No orders yet"
              message="Create an order to start tracking work here."
              action={
                <Link className="button button-secondary" to="/orders">
                  Open orders
                </Link>
              }
            />
          ) : (
            recentOrders.map((order) => (
              <Link
                className="list-row"
                to={`/orders/${order.id}`}
                key={order.id}
              >
                <div>
                  <strong>Order #{order.id}</strong>
                  <small>
                    {order.customer_name || "Customer"} ·{" "}
                    {formatDate(order.created_at)}
                  </small>
                </div>
                <StatusBadge value={order.order_status || "PENDING"} />
              </Link>
            ))
          )}
        </section>
      </div>
    </div>
  );
}

export default Dashboard;

