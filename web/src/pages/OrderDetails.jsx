import { useCallback, useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import api from "../services/api";
import FeedbackState from "../components/FeedbackState";
import PageHeader from "../components/PageHeader";

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
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const loadOrder = useCallback(async () => {
    try {
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
    loadOrder();
  }, [loadOrder]);

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

  return (
    <div className="page-stack">
      <PageHeader
        eyebrow="Reading order"
        title={
          order.title ||
          order.order_number ||
          order.orderNumber ||
          `Order #${order.id}`
        }
        action={
          <Link className="button button-secondary" to="/orders">
            Back to orders
          </Link>
        }
        theme="orders"
      />
      {error && <p className="alert alert-error">{error}</p>}

      <article className="order-reader">
        <h2 className="order-reader-title">
          {order.title || order.order_number || `Order #${order.id}`}
        </h2>
        <p className="order-reader-body">
          {order.description || "No details provided."}
        </p>
        <footer className="order-reader-meta">
          <span>
            Taken by{" "}
            <strong>
              {order.created_by_name ||
                order.creator_name ||
                order.taken_by_name ||
                "Family member"}
            </strong>
          </span>
          <time dateTime={order.created_at}>
            {formatDate(order.created_at)}
          </time>
        </footer>
      </article>
    </div>
  );
}

export default OrderDetails;
