 import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import api from "../services/api";

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

  const loadOrder = async () => {
    try {
      setLoading(true);
      setError("");

      if (!orderId || orderId === "undefined") {
        setError("Order ID is missing from the URL.");
        return;
      }

      console.log(
        "Loading order from:",
        `/orders/${orderId}`
      );

      const response = await api.get(
        `/orders/${orderId}`
      );

      console.log(
        "Complete order response:",
        response.data
      );

      const body = response.data;

      let orderData = body?.data || body;

      if (orderData?.order) {
        orderData = orderData.order;
      }

      if (Array.isArray(orderData)) {
        orderData = orderData[0];
      }

      if (!orderData || !orderData.id) {
        setError(
          "The server response does not contain valid order data."
        );

        console.error(
          "Invalid order response:",
          response.data
        );

        return;
      }

      setOrder(orderData);

      setSelectedStatus(
        orderData.order_status || "PENDING"
      );
    } catch (error) {
      console.error("Load order error:", {
        status: error.response?.status,
        response: error.response?.data,
        message: error.message,
      });

      setError(
        error.response?.data?.message ||
          error.response?.data?.error ||
          error.message ||
          "Failed to load order."
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadOrder();
  }, [orderId]);

  const handleVerify = async () => {
    try {
      setSubmitting(true);
      setMessage("");

      await api.patch(`/orders/${orderId}/verify`, {
        verificationNote:
          verificationNote.trim() || null,
      });

      setMessage("Order verified successfully.");
      setVerificationNote("");

      await loadOrder();
    } catch (error) {
      console.error("Verify order error:", error);

      setMessage(
        error.response?.data?.message ||
          error.response?.data?.error ||
          error.message ||
          "Failed to verify order."
      );
    } finally {
      setSubmitting(false);
    }
  };

  const handleReject = async () => {
    try {
      setSubmitting(true);
      setMessage("");

      await api.patch(`/orders/${orderId}/reject`, {
        verificationNote:
          verificationNote.trim() || null,
      });

      setMessage("Order rejected successfully.");
      setVerificationNote("");

      await loadOrder();
    } catch (error) {
      console.error("Reject order error:", error);

      setMessage(
        error.response?.data?.message ||
          error.response?.data?.error ||
          error.message ||
          "Failed to reject order."
      );
    } finally {
      setSubmitting(false);
    }
  };

  const handleStatusUpdate = async () => {
    try {
      setSubmitting(true);
      setMessage("");

      await api.patch(`/orders/${orderId}/status`, {
        status: selectedStatus,
      });

      setMessage(
        "Order status updated successfully."
      );

      await loadOrder();
    } catch (error) {
      console.error(
        "Update order status error:",
        error
      );

      setMessage(
        error.response?.data?.message ||
          error.response?.data?.error ||
          error.message ||
          "Failed to update order status."
      );
    } finally {
      setSubmitting(false);
    }
  };

  const formatDate = (dateValue) => {
    if (!dateValue) {
      return "Not available";
    }

    const date = new Date(dateValue);

    if (Number.isNaN(date.getTime())) {
      return "Invalid date";
    }

    return date.toLocaleString("en-US", {
      weekday: "long",
      day: "numeric",
      month: "numeric",
      year: "numeric",
      hour: "numeric",
      minute: "2-digit",
      hour12: true,
    });
  };

  if (loading) {
    return (
      <div>
        <h1>Order Details</h1>
        <p>Loading order...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div>
        <h1>Order Details</h1>

        <p style={{ color: "red" }}>
          {error}
        </p>

        <button
          type="button"
          onClick={() => navigate("/orders")}
        >
          ← Back to Orders
        </button>
      </div>
    );
  }

  if (!order) {
    return (
      <div>
        <h1>Order Details</h1>

        <p>Order not found.</p>

        <button
          type="button"
          onClick={() => navigate("/orders")}
        >
          ← Back to Orders
        </button>
      </div>
    );
  }

  const canVerify =
    order.verification_status === "PENDING";

  const isCompleted =
    order.order_status === "COMPLETED";

  const isRejected =
    order.verification_status === "REJECTED";

  return (
    <div>
      <button
        type="button"
        onClick={() => navigate("/orders")}
      >
        ← Back to Orders
      </button>

      <h1>Order Details</h1>

      <hr />

      <section>
        <h2>Order #{order.id}</h2>

        <p>
          <strong>Order Number:</strong>{" "}
          {order.order_number ||
            order.orderNumber ||
            "Not available"}
        </p>

        <p>
          <strong>Customer:</strong>{" "}
          {order.customer_name ||
            order.full_name ||
            `Customer #${order.customer_id}`}
        </p>

        <p>
          <strong>Customer ID:</strong>{" "}
          {order.customer_id || "Not available"}
        </p>

        <p>
          <strong>Description:</strong>{" "}
          {order.description || "No description"}
        </p>

        <p>
          <strong>Order Status:</strong>{" "}
          {order.order_status || "Not available"}
        </p>

        <p>
          <strong>Verification Status:</strong>{" "}
          {order.verification_status ||
            "Not available"}
        </p>

        <p>
          <strong>Created:</strong>{" "}
          {formatDate(order.created_at)}
        </p>

        {order.verified_at && (
          <p>
            <strong>Verified At:</strong>{" "}
            {formatDate(order.verified_at)}
          </p>
        )}

        {order.verification_note && (
          <p>
            <strong>Verification Note:</strong>{" "}
            {order.verification_note}
          </p>
        )}
      </section>

      <hr />

      {canVerify && !isCompleted && (
        <section>
          <h2>Verification</h2>

          <label htmlFor="verificationNote">
            Verification Note
          </label>

          <br />

          <textarea
            id="verificationNote"
            value={verificationNote}
            onChange={(event) =>
              setVerificationNote(event.target.value)
            }
            placeholder="Optional verification note"
            rows="4"
          />

          <br />
          <br />

          <button
            type="button"
            onClick={handleVerify}
            disabled={submitting}
          >
            {submitting
              ? "Processing..."
              : "Verify Order"}
          </button>

          {" "}

          <button
            type="button"
            onClick={handleReject}
            disabled={submitting}
          >
            {submitting
              ? "Processing..."
              : "Reject Order"}
          </button>
        </section>
      )}

      {isRejected && (
        <p style={{ color: "red" }}>
          This order was rejected.
        </p>
      )}

      {!canVerify && !isRejected && (
        <p>
          This order has already been processed for
          verification.
        </p>
      )}

      <hr />

      {!isCompleted && !isRejected && (
        <section>
          <h2>Update Order Status</h2>

          <select
            value={selectedStatus}
            onChange={(event) =>
              setSelectedStatus(event.target.value)
            }
          >
            <option value="PENDING">
              PENDING
            </option>

            <option value="CONFIRMED">
              CONFIRMED
            </option>

            <option value="PROCESSING">
              PROCESSING
            </option>

            <option value="READY">
              READY
            </option>

            <option value="COMPLETED">
              COMPLETED
            </option>

            <option value="CANCELLED">
              CANCELLED
            </option>
          </select>

          {" "}

          <button
            type="button"
            onClick={handleStatusUpdate}
            disabled={submitting}
          >
            {submitting
              ? "Updating..."
              : "Update Status"}
          </button>
        </section>
      )}

      {isCompleted && (
        <p>
          This order is completed and cannot be changed.
        </p>
      )}

      {message && <p>{message}</p>}
    </div>
  );
}

export default OrderDetails;