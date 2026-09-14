import { useCallback, useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import api from "../services/api";
import FeedbackState from "../components/FeedbackState";
import PageHeader from "../components/PageHeader";

const formatDate = (value) =>
  value
    ? new Date(value).toLocaleString("en-US", {
        dateStyle: "medium",
        timeStyle: "short",
      })
    : "Date unavailable";

function CustomerDetails() {
  const { customerId } = useParams();
  const [customer, setCustomer] = useState(null);
  const [notes, setNotes] = useState([]);
  const [noteText, setNoteText] = useState("");
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  const loadCustomerData = useCallback(async (showSpinner = false) => {
    if (!customerId || customerId === "undefined") {
      setError("Customer ID is missing.");
      setLoading(false);
      return;
    }
    try {
      if (showSpinner) setLoading(true);
      setError("");
      const [customerResponse, notesResponse] = await Promise.all([
        api.get(`/customers/${customerId}`),
        api.get(`/customers/${customerId}/notes`).catch((err) => {
          console.warn("Could not load customer notes:", err);
          return { data: { data: [] } };
        }),
      ]);

      const customerData = customerResponse.data?.data || null;
      setCustomer(customerData);

      const notesData = Array.isArray(notesResponse.data?.data)
        ? notesResponse.data.data
        : [];
      setNotes(notesData);
    } catch (requestError) {
      console.error("Load customer details error:", requestError);
      setError(
        requestError.response?.data?.message || "We could not load this customer."
      );
    } finally {
      setLoading(false);
    }
  }, [customerId]);

  useEffect(() => {
    loadCustomerData(false);
  }, [loadCustomerData]);

  async function handleAddNote(event) {
    event.preventDefault();
    setMessage("");
    setError("");
    if (!noteText.trim()) {
      setError("Please write a note before saving.");
      return;
    }
    try {
      setSubmitting(true);
      await api.post(`/customers/${customerId}/notes`, {
        note_text: noteText.trim(),
      });
      setNoteText("");
      setMessage("Note added successfully.");
      await loadCustomerData();
    } catch (requestError) {
      console.error("Add note error:", requestError);
      setError(
        requestError.response?.data?.message || "We could not save that note."
      );
    } finally {
      setSubmitting(false);
    }
  }

  if (loading) return <FeedbackState type="loading" title="Loading customer profile..." />;
  if (error && !customer)
    return (
      <FeedbackState
        type="error"
        title="Customer unavailable"
        message={error}
        action={
          <button type="button" onClick={loadCustomerData}>
            Try again
          </button>
        }
      />
    );
  if (!customer)
    return (
      <FeedbackState
        title="Customer not found"
        message="This customer record may have been removed or the link is incorrect."
        action={
          <Link className="button button-secondary" to="/customers">
            Back to customers
          </Link>
        }
      />
    );

  return (
    <div className="page-stack">
      <PageHeader
        eyebrow="Customer profile"
        title={customer.full_name || "Customer"}
        description={customer.phone || "No phone number has been added"}
        action={
          <Link className="button button-secondary" to="/customers">
            Back to customers
          </Link>
        }
      />
      <section className="profile-summary panel">
        <div className="profile-summary-main">
          <span className="profile-large-avatar">
            {(customer.full_name || "C").charAt(0).toUpperCase()}
          </span>
          <div>
            <p className="eyebrow">Customer #{customer.id}</p>
            <h2>{customer.full_name || "Unnamed customer"}</h2>
            <p>{customer.phone || "No phone number"}</p>
          </div>
        </div>
        <div className="profile-balance">
          <span>Current balance</span>
          <strong>{Number(customer.balance || 0).toLocaleString("en-US")} ETB</strong>
          <small>Verified balance</small>
        </div>
      </section>

      <div className="action-row">
        <Link className="button" to={`/orders?customerId=${customer.id}`}>
          Create order <span aria-hidden="true">-&gt;</span>
        </Link>
        <Link
          className="button button-secondary"
          to={`/finance?customerId=${customer.id}`}
        >
          Record financial movement
        </Link>
      </div>

      {error && <p className="alert alert-error">{error}</p>}

      <section className="panel">
        <div className="section-heading">
          <div>
            <p className="eyebrow">Shared context</p>
            <h2>Customer notes ({notes.length})</h2>
          </div>
          <span className="muted">Visible to the whole family team</span>
        </div>

        <form className="form-stack" onSubmit={handleAddNote}>
          <label className="field-label" htmlFor="noteText">
            Add a note
            <textarea
              id="noteText"
              value={noteText}
              onChange={(event) => setNoteText(event.target.value)}
              placeholder="Write something useful for the next family member..."
              rows="3"
              required
            />
          </label>
          <div>
            <button type="submit" disabled={submitting}>
              {submitting ? "Saving..." : "Save note"}
            </button>
          </div>
        </form>

        {message && <p className="alert alert-success">{message}</p>}

        <div className="notes-list" style={{ marginTop: "1.5rem" }}>
          {notes.length === 0 ? (
            <FeedbackState
              title="No notes yet"
              message="Add the first note to keep useful context close to the customer record."
            />
          ) : (
            notes.map((note) => (
              <article className="note-card" key={note.id}>
                <p>{note.note_text || note.note || note.description || "Note text unavailable"}</p>
                <small>
                  By {note.created_by_name || note.author_name || note.creator_name || "Family member"} ·{" "}
                  {formatDate(note.created_at)}
                </small>
              </article>
            ))
          )}
        </div>
      </section>
    </div>
  );
}

export default CustomerDetails;

