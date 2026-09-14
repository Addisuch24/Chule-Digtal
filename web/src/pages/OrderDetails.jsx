import { useCallback, useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import api from "../services/api";
import FeedbackState from "../components/FeedbackState";
import PageHeader from "../components/PageHeader";
import StatusBadge from "../components/StatusBadge";

const formatDate = (value) =>
  value
    ? new Date(value).toLocaleString("en-US", {
        dateStyle: "medium",
        timeStyle: "short",
      })
    : "Not available";

function OrderDetails() {
  const { orderId } = useParams();
  const navigate = useNavigate();
  const [order, setOrder] = useState(null);
  const [verificationNote, setVerificationNote] = useState("");
  const [selectedStatus, setSelectedStatus] = useState("");
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  const loadOrder = useCallback(async (showSpinner = false) => {
    try {
      if (showSpinner) setLoading(true);
      setError("");
      if (!orderId || orderId === "undefined") {
        setError("Order ID is missing from the URL.");
        return;
      }
      const response = await api.get(`/orders/${orderId}`);
      let orderData = response.data?.data || response.data;
      if (orderData?.order) orderData = orderData.order;
      if (Array.isArray(orderData)) orderData = orderData[0];
      if (!orderData?.id) {
        setError("The server response did not contain a valid order.");
        return;
      }
      setOrder(orderData);
      setSelectedStatus(orderData.order_status || "PENDING");
    } catch (requestError) {
      console.error("Load order error:", requestError);
      setError(
        requestError.response?.data?.message || "We could not load this order."
      );
    } finally {
      setLoading(false);
    }
  }, [orderId]);

  useEffect(() => {
    loadOrder(false);
  }, [loadOrder]);

  async function handleVerification(action) {
    if (action === "reject" && !verificationNote.trim()) {
      setError("Please add a reason for rejection before rejecting the order.");
      return;
    }
    try {
      setSubmitting(true);
      setMessage("");
      setError("");
      await api.patch(`/orders/${orderId}/${action}`, {
        verificationNote: verificationNote.trim() || undefined,
      });
      setMessage(
        `Order ${action === "verify" ? "verified" : "rejected"} successfully.`
      );
      setVerificationNote("");
      await loadOrder(true);
    } catch (requestError) {
      console.error(`${action} order error:`, requestError);
      setError(
        requestError.response?.data?.message ||
          `We could not ${action} this order.`
      );
    } finally {
      setSubmitting(false);
    }
  }

  async function handleStatusUpdate() {
    try {
      setSubmitting(true);
      setMessage("");
      setError("");
      await api.patch(`/orders/${orderId}/status`, {
        orderStatus: selectedStatus,
      });
      setMessage("Order status updated successfully.");
      await loadOrder();
    } catch (requestError) {
      console.error("Update order status error:", requestError);
      setError(
        requestError.response?.data?.message ||
          "We could not update the order status."
      );
    } finally {
      setSubmitting(false);
    }
  }

  if (loading) return <FeedbackState type="loading" title="Loading order details..." />;
  if (error && !order)
    return (
      <FeedbackState
        type="error"
        title="Order unavailable"
        message={error}
        action={
          <button type="button" onClick={loadOrder}>
            Try again
          </button>
        }
      />
    );
  if (!order)
    return (
      <FeedbackState
        title="Order not found"
        action={
          <button type="button" onClick={() => navigate("/orders")}>
            Back to orders
          </button>
        }
      />
    );

  const canVerify = order.verification_status === "PENDING";
  const isRejected = order.verification_status === "REJECTED";

  return (
    <div className="page-stack">
      <PageHeader
        eyebrow="Order detail"
        title={order.order_number || order.orderNumber || `Order #${order.id}`}
        description={order.description || "No description"}
        action={
          <Link className="button button-secondary" to="/orders">
            Back to orders
          </Link>
        }
      />
      {message && <p className="alert alert-success">{message}</p>}
      {error && <p className="alert alert-error">{error}</p>}

      <section className="panel">
        <div className="detail-heading">
          <div>
            <p className="eyebrow">Current state</p>
            <h2>Order #{order.id}</h2>
          </div>
          <div className="detail-badges">
            <StatusBadge value={order.order_status || "PENDING"} />
            <StatusBadge value={order.verification_status || "PENDING"} />
          </div>
        </div>
        <div className="detail-grid">
          <div className="detail-row">
            <span>Customer</span>
            <strong>
              {order.customer_name ||
                order.full_name ||
                `Customer #${order.customer_id}`}
            </strong>
          </div>
          <div className="detail-row">
            <span>Customer ID</span>
            <strong>{order.customer_id || "Not available"}</strong>
          </div>
          <div className="detail-row">
            <span>Taken by</span>
            <strong>
              {order.created_by_name ||
                order.creator_name ||
                order.taken_by_name ||
                "Family member"}
            </strong>
          </div>
          <div className="detail-row">
            <span>Created</span>
            <strong>{formatDate(order.created_at)}</strong>
          </div>
          <div className="detail-row">
            <span>Verified by</span>
            <strong>
              {order.verified_by_name || order.verifier_name || "Not verified"}
            </strong>
          </div>
          <div className="detail-row">
            <span>Verified at</span>
            <strong>{formatDate(order.verified_at)}</strong>
          </div>
        </div>
        {order.verification_note && (
          <div className="detail-note">
            <span>Verification note</span>
            <p>{order.verification_note}</p>
          </div>
        )}
      </section>

      <section className="panel">
        <div className="section-heading">
          <div>
            <p className="eyebrow">Workflow</p>
            <h2>Update order status</h2>
          </div>
          <span className="muted">Use the status that matches the work right now.</span>
        </div>
        <div className="inline-form">
          <select
            value={selectedStatus}
            onChange={(event) => setSelectedStatus(event.target.value)}
            aria-label="Order status"
          >
            <option value="PENDING">Pending</option>
            <option value="CONFIRMED">Confirmed</option>
            <option value="PROCESSING">Processing</option>
            <option value="READY">Ready</option>
            <option value="COMPLETED">Completed</option>
            <option value="CANCELLED">Cancelled</option>
          </select>
          <button
            type="button"
            disabled={submitting || selectedStatus === order.order_status}
            onClick={handleStatusUpdate}
          >
            Save status
          </button>
        </div>
      </section>

      {canVerify && (
        <section className="panel">
          <div className="section-heading">
            <div>
              <p className="eyebrow">Review needed</p>
              <h2>Verification</h2>
            </div>
            <StatusBadge value="PENDING" />
          </div>

          <div className="form-stack">
            <label className="field-label" htmlFor="verificationNote">
              Verification / Rejection note{" "}
              <span className="muted">(required for rejection, optional for verify)</span>
              <textarea
                id="verificationNote"
                value={verificationNote}
                onChange={(event) => setVerificationNote(event.target.value)}
                placeholder="Add a note or reason for rejection..."
                rows="3"
              />
            </label>
            <div className="action-row">
              <button
                type="button"
                disabled={submitting}
                onClick={() => handleVerification("verify")}
              >
                {submitting ? "Processing..." : "Verify order"}
              </button>
              <button
                className="button-danger"
                type="button"
                disabled={submitting}
                onClick={() => handleVerification("reject")}
              >
                Reject order
              </button>
            </div>
          </div>
        </section>
      )}

      {isRejected && (
        <div className="feedback-state feedback-error">
          <span className="feedback-mark">!</span>
          <div>
            <strong>This order was rejected.</strong>
            <p>Review the verification note above before taking further action.</p>
          </div>
        </div>
      )}
    </div>
  );
}

export default OrderDetails;

