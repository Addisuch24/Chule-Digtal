import { useCallback, useEffect, useMemo, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
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

function getArrayData(response, key) {
  const data = response?.data;
  if (Array.isArray(data)) return data;
  if (Array.isArray(data?.data)) return data.data;
  if (Array.isArray(data?.[key])) return data[key];
  return [];
}

function Orders() {
  const [searchParams] = useSearchParams();
  const [orders, setOrders] = useState([]);
  const [customers, setCustomers] = useState([]);
  const [customerId, setCustomerId] = useState(
    searchParams.get("customerId") || ""
  );
  const [description, setDescription] = useState("");
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [formMessage, setFormMessage] = useState("");

  const loadOrders = useCallback(async (showSpinner = false) => {
    try {
      if (showSpinner) setLoading(true);
      setError("");
      const response = await api.get("/orders");
      setOrders(getArrayData(response, "orders"));
    } catch (requestError) {
      console.error("Load orders error:", requestError);
      setError(
        requestError.response?.data?.message || "We could not load orders."
      );
    } finally {
      setLoading(false);
    }
  }, []);

  const loadCustomers = useCallback(async () => {
    try {
      const response = await api.get("/customers");
      setCustomers(getArrayData(response, "customers"));
    } catch (requestError) {
      console.error("Load customers error:", requestError);
    }
  }, []);

  useEffect(() => {
    loadOrders();
    loadCustomers();
  }, [loadOrders, loadCustomers]);

  async function handleCreateOrder(event) {
    event.preventDefault();
    setFormMessage("");
    setError("");
    if (!customerId) {
      setFormMessage("Please choose a customer.");
      return;
    }
    if (!description.trim()) {
      setFormMessage("Please add a short description.");
      return;
    }
    try {
      setSubmitting(true);
      const generatedOrderNumber = `ORD-${Date.now().toString().slice(-6)}`;
      await api.post(`/orders/customers/${customerId}/orders`, {
        orderNumber: generatedOrderNumber,
        customerId: Number(customerId),
        description: description.trim(),
      });
      setFormMessage("Order created successfully.");
      setCustomerId("");
      setDescription("");
      await loadOrders();
    } catch (requestError) {
      console.error("Create order error:", requestError);
      setFormMessage(
        requestError.response?.data?.message || "We could not create that order."
      );
    } finally {
      setSubmitting(false);
    }
  }

  const filteredOrders = useMemo(() => {
    const query = search.trim().toLowerCase();
    return orders.filter((order) => {
      if (!query) return true;
      return [
        order.order_number,
        order.customer_name,
        order.full_name,
        order.description,
        order.order_status,
        order.verification_status,
      ].some((value) =>
        String(value || "")
          .toLowerCase()
          .includes(query)
      );
    });
  }, [orders, search]);

  return (
    <div className="page-stack">
      <PageHeader
        eyebrow="Work tracking"
        title="Orders"
        description="Create, review, and follow customer work from one shared queue."
        action={<span className="count-label">{orders.length} total</span>}
      />

      <section className="panel">
        <div className="section-heading">
          <div>
            <p className="eyebrow">New work</p>
            <h2>Create an order</h2>
          </div>
          <span className="muted">The order starts as pending verification</span>
        </div>

        <form className="form-stack" onSubmit={handleCreateOrder}>
          <div className="form-grid">
            <label>
              Customer
              <select
                value={customerId}
                onChange={(event) => setCustomerId(event.target.value)}
                required
              >
                <option value="">Select a customer</option>
                {customers.map((customer) => (
                  <option key={customer.id} value={customer.id}>
                    {customer.full_name || customer.name || `Customer #${customer.id}`}
                  </option>
                ))}
              </select>
            </label>
            <div className="form-hint">
              <strong>What is this for?</strong>
              <span>
                Keep the description specific enough for another family member to pick it up.
              </span>
            </div>
          </div>

          <label>
            Order description
            <textarea
              value={description}
              onChange={(event) => setDescription(event.target.value)}
              placeholder="e.g. 24 notebooks and delivery to Bole"
              rows="3"
              required
            />
          </label>

          <div>
            <button type="submit" disabled={submitting}>
              {submitting ? "Creating..." : "Create order"}
            </button>
          </div>
        </form>

        {formMessage && (
          <p
            className={`alert ${
              formMessage.includes("successfully")
                ? "alert-success"
                : "alert-error"
            }`}
          >
            {formMessage}
          </p>
        )}
      </section>

      <section className="panel">
        <div className="section-heading">
          <div>
            <p className="eyebrow">Shared queue</p>
            <h2>All orders</h2>
          </div>
          <input
            className="search-input"
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="Search orders by number, customer, status..."
            aria-label="Search orders"
          />
        </div>

        {loading ? (
          <FeedbackState type="loading" title="Loading orders..." />
        ) : error ? (
          <FeedbackState
            type="error"
            title="Orders unavailable"
            message={error}
            action={
              <button type="button" onClick={loadOrders}>
                Try again
              </button>
            }
          />
        ) : filteredOrders.length === 0 ? (
          <FeedbackState
            title={search ? "No matching orders" : "No orders yet"}
            message={
              search
                ? "Try a different search term."
                : "Create an order above to start the queue."
            }
          />
        ) : (
          <div className="data-table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Order</th>
                  <th>Customer</th>
                  <th>Description</th>
                  <th>Order status</th>
                  <th>Verification</th>
                  <th>Taken by</th>
                  <th>Date</th>
                </tr>
              </thead>
              <tbody>
                {filteredOrders.map((order) => (
                  <tr key={order.id}>
                    <td>
                      <Link
                        className="table-primary"
                        to={`/orders/${order.id}`}
                      >
                        {order.order_number ||
                          order.orderNumber ||
                          `Order #${order.id}`}
                      </Link>
                      <small>#{order.id}</small>
                    </td>
                    <td>
                      {order.customer_name ||
                        order.full_name ||
                        `Customer #${order.customer_id}`}
                    </td>
                    <td className="table-description">
                      {order.description || "No description"}
                    </td>
                    <td>
                      <StatusBadge value={order.order_status || "PENDING"} />
                    </td>
                    <td>
                      <StatusBadge
                        value={order.verification_status || "PENDING"}
                      />
                    </td>
                    <td>
                      {order.created_by_name ||
                        order.creator_name ||
                        order.taken_by_name ||
                        "Family member"}
                    </td>
                    <td>{formatDate(order.created_at)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}

export default Orders;

