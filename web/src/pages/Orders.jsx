 import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import api from "../services/api";

function Orders() {
  const [orders, setOrders] = useState([]);
  const [customers, setCustomers] = useState([]);

  const [customerId, setCustomerId] = useState("");
  const [description, setDescription] = useState("");

  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  const [error, setError] = useState("");
  const [formMessage, setFormMessage] = useState("");

  const getArrayData = (response) => {
    const data = response?.data;

    if (Array.isArray(data)) {
      return data;
    }

    if (Array.isArray(data?.data)) {
      return data.data;
    }

    if (Array.isArray(data?.orders)) {
      return data.orders;
    }

    if (Array.isArray(data?.customers)) {
      return data.customers;
    }

    return [];
  };

  const loadOrders = async () => {
    try {
      setLoading(true);
      setError("");

      const response = await api.get("/orders");

      const orderList = getArrayData(response);

      setOrders(orderList);
    } catch (error) {
      console.error("Load orders error:", error);

      setError(
        error.response?.data?.message ||
          error.response?.data?.error ||
          error.message ||
          "Failed to load orders."
      );
    } finally {
      setLoading(false);
    }
  };

  const loadCustomers = async () => {
    try {
      const response = await api.get("/customers");

      const customerList = getArrayData(response);

      setCustomers(customerList);
    } catch (error) {
      console.error("Load customers error:", error);

      setError(
        error.response?.data?.message ||
          error.response?.data?.error ||
          error.message ||
          "Failed to load customers."
      );
    }
  };

  useEffect(() => {
    loadOrders();
    loadCustomers();
  }, []);

  const handleCreateOrder = async (event) => {
    event.preventDefault();

    if (!customerId) {
      setFormMessage("Please select a customer.");
      return;
    }

    if (!description.trim()) {
      setFormMessage("Please enter an order description.");
      return;
    }

    try {
      setSubmitting(true);
      setFormMessage("");

      const generatedOrderNumber = `ORD-${Date.now()}`;

      const orderPayload = {
        orderNumber: generatedOrderNumber,
        customerId: Number(customerId),
        description: description.trim(),
      };

      console.log("Creating order:", orderPayload);

      await api.post(
        `/orders/customers/${customerId}/orders`,
        orderPayload
      );

      setFormMessage("Order created successfully.");
      setCustomerId("");
      setDescription("");

      await loadOrders();
    } catch (error) {
      console.error("Create order error:", error);

      setFormMessage(
        error.response?.data?.message ||
          error.response?.data?.error ||
          error.message ||
          "Failed to create order."
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

  return (
    <div>
      <h1>Orders</h1>

      <p>Create and manage customer orders.</p>

      <hr />

      <section>
        <h2>Create Order</h2>

        <form onSubmit={handleCreateOrder}>
          <div>
            <label htmlFor="customerId">
              Customer
            </label>

            <br />

            <select
              id="customerId"
              value={customerId}
              onChange={(event) =>
                setCustomerId(event.target.value)
              }
              required
            >
              <option value="">
                Select customer
              </option>

              {customers.map((customer) => (
                <option
                  key={customer.id}
                  value={customer.id}
                >
                  {customer.full_name ||
                    customer.name ||
                    `Customer #${customer.id}`}
                </option>
              ))}
            </select>
          </div>

          <br />

          <div>
            <label htmlFor="description">
              Order Description
            </label>

            <br />

            <textarea
              id="description"
              value={description}
              onChange={(event) =>
                setDescription(event.target.value)
              }
              placeholder="Enter order description"
              rows="4"
              required
            />
          </div>

          <br />

          <button
            type="submit"
            disabled={submitting}
          >
            {submitting
              ? "Creating..."
              : "Create Order"}
          </button>
        </form>

        {formMessage && <p>{formMessage}</p>}
      </section>

      <hr />

      <section>
        <h2>All Orders</h2>

        {loading && <p>Loading orders...</p>}

        {error && (
          <p style={{ color: "red" }}>
            {error}
          </p>
        )}

        {!loading &&
          !error &&
          orders.length === 0 && (
            <p>No orders found.</p>
          )}

        {!loading &&
          orders.length > 0 &&
          orders.map((order) => {
            const orderId = order.id;

            return (
              <article
                key={orderId}
                style={{
                  border: "1px solid #ddd",
                  padding: "16px",
                  marginBottom: "12px",
                  borderRadius: "8px",
                }}
              >
                <h3>Order #{orderId}</h3>

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
                  <strong>Description:</strong>{" "}
                  {order.description ||
                    "No description"}
                </p>

                <p>
                  <strong>Order Status:</strong>{" "}
                  {order.order_status ||
                    "PENDING"}
                </p>

                <p>
                  <strong>Verification:</strong>{" "}
                  {order.verification_status ||
                    "PENDING"}
                </p>

                <p>
                  <strong>Created:</strong>{" "}
                  {formatDate(order.created_at)}
                </p>

                {orderId ? (
                  <Link to={`/orders/${orderId}`}>
                    View Order
                  </Link>
                ) : (
                  <p style={{ color: "red" }}>
                    This order has no ID.
                  </p>
                )}
              </article>
            );
          })}
      </section>
    </div>
  );
}

export default Orders;