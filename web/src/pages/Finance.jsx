import { useEffect, useState } from "react";
import api from "../services/api";

function Finance() {
  const [movements, setMovements] = useState([]);
  const [customers, setCustomers] = useState([]);

  const [customerId, setCustomerId] = useState("");
  const [movementType, setMovementType] = useState("DEBIT");
  const [amount, setAmount] = useState("");
  const [description, setDescription] = useState("");

  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  const getArrayData = (response) => {
    const body = response?.data;

    if (Array.isArray(body)) {
      return body;
    }

    if (Array.isArray(body?.data)) {
      return body.data;
    }

    if (Array.isArray(body?.movements)) {
      return body.movements;
    }

    if (Array.isArray(body?.financialMovements)) {
      return body.financialMovements;
    }

    if (Array.isArray(body?.customers)) {
      return body.customers;
    }

    return [];
  };

  const loadMovements = async () => {
    try {
      setLoading(true);
      setError("");

      const response = await api.get(
        "/financial-movements"
      );

      setMovements(getArrayData(response));
    } catch (error) {
      console.error("Load movements error:", error);

      setError(
        error.response?.data?.message ||
          error.response?.data?.error ||
          error.message ||
          "Failed to load financial movements."
      );
    } finally {
      setLoading(false);
    }
  };

  const loadCustomers = async () => {
    try {
      const response = await api.get("/customers");

      setCustomers(getArrayData(response));
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
    loadMovements();
    loadCustomers();
  }, []);

  const handleCreateMovement = async (event) => {
    event.preventDefault();

    if (!customerId) {
      setMessage("Please select a customer.");
      return;
    }

    if (!amount || Number(amount) <= 0) {
      setMessage("Please enter a valid amount.");
      return;
    }

    if (!description.trim()) {
      setMessage("Please enter a description.");
      return;
    }

    try {
      setSubmitting(true);
      setMessage("");

      const payload = {
        customerId: Number(customerId),
        movementType,
        amount: Number(amount),
        description: description.trim(),
      };

      console.log("Creating financial movement:", payload);

      await api.post("/financial-movements", payload);

      setMessage(
        "Financial movement created successfully."
      );

      setCustomerId("");
      setMovementType("DEBIT");
      setAmount("");
      setDescription("");

      await loadMovements();
    } catch (error) {
      console.error(
        "Create financial movement error:",
        error
      );

      setMessage(
        error.response?.data?.message ||
          error.response?.data?.error ||
          error.message ||
          "Failed to create financial movement."
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

  const formatAmount = (value) => {
    const number = Number(value || 0);

    return `${number.toLocaleString("en-US")} ETB`;
  };

  return (
    <div>
      <h1>Finance</h1>

      <p>
        Record customer debit, credit, and payment
        movements.
      </p>

      <hr />

      <section>
        <h2>Create Financial Movement</h2>

        <form onSubmit={handleCreateMovement}>
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
            <label htmlFor="movementType">
              Movement Type
            </label>

            <br />

            <select
              id="movementType"
              value={movementType}
              onChange={(event) =>
                setMovementType(event.target.value)
              }
            >
              <option value="DEBIT">
                DEBIT
              </option>

              <option value="CREDIT">
                CREDIT
              </option>
            </select>
          </div>

          <br />

          <div>
            <label htmlFor="amount">
              Amount
            </label>

            <br />

            <input
              id="amount"
              type="number"
              min="0.01"
              step="0.01"
              value={amount}
              onChange={(event) =>
                setAmount(event.target.value)
              }
              placeholder="Enter amount"
              required
            />
          </div>

          <br />

          <div>
            <label htmlFor="description">
              Description
            </label>

            <br />

            <textarea
              id="description"
              value={description}
              onChange={(event) =>
                setDescription(event.target.value)
              }
              placeholder="Enter financial description"
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
              ? "Saving..."
              : "Save Movement"}
          </button>
        </form>

        {message && <p>{message}</p>}
      </section>

      <hr />

      <section>
        <h2>Financial Movements</h2>

        {loading && <p>Loading movements...</p>}

        {error && (
          <p style={{ color: "red" }}>
            {error}
          </p>
        )}

        {!loading &&
          !error &&
          movements.length === 0 && (
            <p>No financial movements found.</p>
          )}

        {!loading &&
          movements.length > 0 &&
          movements.map((movement) => (
            <article
              key={movement.id}
              style={{
                border: "1px solid #ddd",
                padding: "16px",
                marginBottom: "12px",
                borderRadius: "8px",
              }}
            >
              <h3>
                Movement #{movement.id}
              </h3>

              <p>
                <strong>Customer:</strong>{" "}
                {movement.customer_name ||
                  movement.full_name ||
                  `Customer #${movement.customer_id}`}
              </p>

              <p>
                <strong>Type:</strong>{" "}
                {movement.movement_type ||
                  movement.movementType ||
                  "Not available"}
              </p>

              <p>
                <strong>Amount:</strong>{" "}
                {formatAmount(movement.amount)}
              </p>

              <p>
                <strong>Description:</strong>{" "}
                {movement.description ||
                  "No description"}
              </p>

              <p>
                <strong>Verification:</strong>{" "}
                {movement.verification_status ||
                  "PENDING"}
              </p>

              <p>
                <strong>Created:</strong>{" "}
                {formatDate(movement.created_at)}
              </p>
            </article>
          ))}
      </section>
    </div>
  );
}

export default Finance;