import { useCallback, useEffect, useMemo, useState } from "react";
import { useSearchParams } from "react-router-dom";
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
    : "Not available";

function getArrayData(response, key) {
  const data = response?.data;
  if (Array.isArray(data)) return data;
  if (Array.isArray(data?.data)) return data.data;
  if (Array.isArray(data?.[key])) return data[key];
  return [];
}

function Finance() {
  const [searchParams] = useSearchParams();
  const [movements, setMovements] = useState([]);
  const [customers, setCustomers] = useState([]);
  const [customerId, setCustomerId] = useState(
    searchParams.get("customerId") || ""
  );
  const [movementType, setMovementType] = useState("DEBIT");
  const [amount, setAmount] = useState("");
  const [description, setDescription] = useState("");
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [reviewId, setReviewId] = useState(null);
  const [verificationNote, setVerificationNote] = useState("");

  const loadMovements = useCallback(async (showSpinner = false) => {
    try {
      if (showSpinner) setLoading(true);
      setError("");
      const response = await api.get("/financial-movements");
      setMovements(getArrayData(response, "movements"));
    } catch (requestError) {
      console.error("Load movements error:", requestError);
      setError(
        requestError.response?.data?.message ||
          "We could not load financial movements."
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
    loadMovements();
    loadCustomers();
  }, [loadMovements, loadCustomers]);

  async function handleCreateMovement(event) {
    event.preventDefault();
    setMessage("");
    setError("");

    if (!customerId) {
      setError("Please choose a customer.");
      return;
    }
    const numAmount = Number(amount);
    if (!amount || Number.isNaN(numAmount) || numAmount <= 0) {
      setError("Amount must be a positive number greater than zero.");
      return;
    }
    if (!description.trim()) {
      setError("Please add a description.");
      return;
    }

    try {
      setSubmitting(true);
      const payload = {
        customerId: Number(customerId),
        movementType,
        amount: numAmount,
        description: description.trim(),
      };

      await api.post(
        movementType === "CREDIT"
          ? "/financial-movements/payments"
          : "/financial-movements",
        payload
      );

      setMessage(
        `${movementType === "CREDIT" ? "Payment" : "Debit"} recorded successfully.`
      );
      setAmount("");
      setDescription("");
      setMovementType("DEBIT");
      await loadMovements();
    } catch (requestError) {
      console.error("Create financial movement error:", requestError);
      setError(
        requestError.response?.data?.message ||
          "We could not record that movement."
      );
    } finally {
      setSubmitting(false);
    }
  }

  async function handleReview(action, movementId) {
    if (!verificationNote.trim()) {
      setError("Please add a verification note before continuing.");
      return;
    }
    try {
      setSubmitting(true);
      setError("");
      await api.patch(`/financial-movements/${movementId}/${action}`, {
        verificationNote: verificationNote.trim(),
      });
      setMessage(
        `Movement ${action === "verify" ? "verified" : "rejected"} successfully.`
      );
      setReviewId(null);
      setVerificationNote("");
      await loadMovements();
    } catch (requestError) {
      console.error("Review financial movement error:", requestError);
      setError(
        requestError.response?.data?.message ||
          "We could not update that movement."
      );
    } finally {
      setSubmitting(false);
    }
  }

  const filteredMovements = useMemo(() => {
    const query = search.trim().toLowerCase();
    return movements.filter((movement) => {
      if (!query) return true;
      return [
        movement.customer_name,
        movement.full_name,
        movement.description,
        movement.movement_type,
        movement.verification_status,
      ].some((value) =>
        String(value || "")
          .toLowerCase()
          .includes(query)
      );
    });
  }, [movements, search]);

  return (
    <div className="page-stack">
      <PageHeader
        eyebrow="Money in motion"
        title="Finance"
        description="Record customer debits and payments, then keep verification visible to everyone."
        action={<span className="count-label">{movements.length} movements</span>}
      />

      <section className="panel">
        <div className="section-heading">
          <div>
            <p className="eyebrow">Record movement</p>
            <h2>
              {movementType === "CREDIT" ? "Record a payment" : "Record a debit"}
            </h2>
          </div>
          <div
            className="segmented-control"
            role="group"
            aria-label="Movement type"
          >
            <button
              className={
                movementType === "DEBIT" ? "segment-active" : "button-secondary"
              }
              type="button"
              onClick={() => setMovementType("DEBIT")}
            >
              Debit
            </button>
            <button
              className={
                movementType === "CREDIT"
                  ? "segment-active"
                  : "button-secondary"
              }
              type="button"
              onClick={() => setMovementType("CREDIT")}
            >
              Payment / credit
            </button>
          </div>
        </div>

        <form className="form-stack" onSubmit={handleCreateMovement}>
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

            <label>
              Amount (ETB)
              <input
                type="number"
                min="0.01"
                step="0.01"
                value={amount}
                onChange={(event) => setAmount(event.target.value)}
                placeholder="0.00"
                required
              />
            </label>

            <div className="form-hint">
              <strong>{movementType === "CREDIT" ? "Payment" : "Debit"}</strong>
              <span>
                Saved as pending until another family member verifies it.
              </span>
            </div>
          </div>

          <label>
            Description
            <textarea
              value={description}
              onChange={(event) => setDescription(event.target.value)}
              placeholder="What was this movement for?"
              rows="3"
              required
            />
          </label>

          <div>
            <button type="submit" disabled={submitting}>
              {submitting
                ? "Saving..."
                : `Record ${movementType === "CREDIT" ? "payment" : "debit"}`}
            </button>
          </div>
        </form>

        {message && <p className="alert alert-success">{message}</p>}
        {error && <p className="alert alert-error">{error}</p>}
      </section>

      <section className="panel">
        <div className="section-heading">
          <div>
            <p className="eyebrow">Shared ledger</p>
            <h2>Financial movements</h2>
          </div>
          <input
            className="search-input"
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="Search movements by customer, description, type..."
            aria-label="Search financial movements"
          />
        </div>

        {loading ? (
          <FeedbackState type="loading" title="Loading financial movements..." />
        ) : filteredMovements.length === 0 ? (
          <FeedbackState
            title={search ? "No matching movements" : "No movements yet"}
            message={
              search
                ? "Try a different search term."
                : "Record a debit or payment above."
            }
          />
        ) : (
          <div className="movement-list">
            {filteredMovements.map((movement) => (
              <article className="movement-card" key={movement.id}>
                <div className="movement-main">
                  <div>
                    <div className="movement-title">
                      <StatusBadge
                        value={movement.movement_type || movement.movementType}
                      />
                      <strong>
                        {movement.customer_name ||
                          movement.full_name ||
                          `Customer #${movement.customer_id}`}
                      </strong>
                    </div>
                    <p>{movement.description || "No description"}</p>
                    <small>
                      {movement.recorded_by_name ||
                        movement.creator_name ||
                        "Family member"}{" "}
                      · {formatDate(movement.created_at)}
                    </small>
                  </div>
                  <div className="movement-amount">
                    <strong>{formatAmount(movement.amount)}</strong>
                    <StatusBadge
                      value={movement.verification_status || "PENDING"}
                    />
                  </div>
                </div>

                {movement.verification_status === "PENDING" &&
                  (reviewId === movement.id ? (
                    <div className="review-box" style={{ marginTop: "1rem" }}>
                      <label htmlFor={`review-${movement.id}`}>
                        Verification note <span className="muted">(required)</span>
                        <textarea
                          id={`review-${movement.id}`}
                          value={verificationNote}
                          onChange={(event) =>
                            setVerificationNote(event.target.value)
                          }
                          placeholder="Why are you verifying or rejecting this?"
                          rows="2"
                          required
                        />
                      </label>
                      <div className="action-row" style={{ marginTop: "0.5rem" }}>
                        <button
                          type="button"
                          disabled={submitting}
                          onClick={() => handleReview("verify", movement.id)}
                        >
                          Verify
                        </button>
                        <button
                          className="button-danger"
                          type="button"
                          disabled={submitting}
                          onClick={() => handleReview("reject", movement.id)}
                        >
                          Reject
                        </button>
                        <button
                          className="button-secondary"
                          type="button"
                          onClick={() => {
                            setReviewId(null);
                            setVerificationNote("");
                          }}
                        >
                          Cancel
                        </button>
                      </div>
                    </div>
                  ) : (
                    <button
                      className="button-secondary"
                      type="button"
                      style={{ marginTop: "0.75rem" }}
                      onClick={() => {
                        setReviewId(movement.id);
                        setVerificationNote("");
                        setError("");
                      }}
                    >
                      Review movement
                    </button>
                  ))}
              </article>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}

export default Finance;

