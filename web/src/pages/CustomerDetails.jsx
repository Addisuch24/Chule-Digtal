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
    : "Date unavailable";

function CustomerDetails() {
  const { customerId } = useParams();
  const navigate = useNavigate();
  const [customer, setCustomer] = useState(null);
  const [notes, setNotes] = useState([]);
  const [noteText, setNoteText] = useState("");
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  const [selectedNoteIds, setSelectedNoteIds] = useState([]);

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

  function handleToggleSelectNote(id, event) {
    event.stopPropagation();
    setSelectedNoteIds((prev) =>
      prev.includes(id) ? prev.filter((item) => item !== id) : [...prev, id]
    );
  }

  function handleSelectAllNotes() {
    if (selectedNoteIds.length === notes.length) {
      setSelectedNoteIds([]);
    } else {
      setSelectedNoteIds(notes.map((n) => n.id));
    }
  }

  async function handleDeleteSingleNote(noteId) {
    if (!window.confirm("Delete this customer note? This cannot be undone.")) return;
    try {
      setSubmitting(true);
      setMessage("");
      setError("");
      await api.delete(`/customers/${customerId}/notes/${noteId}`);
      setMessage("Note deleted successfully.");
      setSelectedNoteIds((prev) => prev.filter((id) => id !== noteId));
      await loadCustomerData();
    } catch (requestError) {
      console.error("Delete customer note error:", requestError);
      setError(
        requestError.response?.data?.message || "Failed to delete note."
      );
    } finally {
      setSubmitting(false);
    }
  }

  async function handleBulkDeleteNotes() {
    if (selectedNoteIds.length === 0) return;
    if (!window.confirm(`Delete ${selectedNoteIds.length} selected note(s)? This cannot be undone.`)) return;

    try {
      setSubmitting(true);
      setMessage("");
      setError("");
      await api.post(`/customers/${customerId}/notes/bulk-delete`, {
        noteIds: selectedNoteIds,
      });
      setMessage(`Successfully deleted ${selectedNoteIds.length} note(s).`);
      setSelectedNoteIds([]);
      await loadCustomerData();
    } catch (requestError) {
      console.error("Bulk delete customer notes error:", requestError);
      setError(
        requestError.response?.data?.message || "Failed to delete notes."
      );
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDeleteCustomer() {
    if (!customer?.id) return;
    if (
      !window.confirm(
        `Are you sure you want to delete customer "${customer.full_name}"? This cannot be undone.`
      )
    ) {
      return;
    }
    try {
      setSubmitting(true);
      setError("");
      setMessage("");
      const response = await api.delete(`/customers/${customer.id}`);
      alert(response.data?.message || "Customer deleted successfully.");
      navigate("/finance");
    } catch (requestError) {
      console.error("Delete customer error:", requestError);
      setError(
        requestError.response?.data?.message ||
          "Could not delete customer. If they have financial records, they must be preserved."
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
        theme="customers"
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
          <small>Includes entries awaiting review</small>
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
        <button
          type="button"
          className="button button-danger"
          style={{
            backgroundColor: "#ef4444",
            color: "#fff",
            border: "none",
            borderRadius: "8px",
            padding: "0.6rem 1.2rem",
            cursor: "pointer",
            fontWeight: 600,
            marginLeft: "auto",
          }}
          onClick={handleDeleteCustomer}
          disabled={submitting}
        >
          {submitting ? "Deleting..." : "Delete Customer"}
        </button>
      </div>

      {error && <p className="alert alert-error">{error}</p>}

      <section className="panel">
        <div className="section-heading">
          <div style={{ display: "flex", alignItems: "center", gap: "0.75rem" }}>
            {notes.length > 0 && (
              <input
                type="checkbox"
                checked={selectedNoteIds.length > 0 && selectedNoteIds.length === notes.length}
                onChange={handleSelectAllNotes}
                aria-label="Select all notes"
                title="Select all notes"
                style={{ width: "18px", height: "18px", cursor: "pointer" }}
              />
            )}
            <div>
              <p className="eyebrow">Shared context</p>
              <h2>Customer notes ({notes.length})</h2>
            </div>
          </div>
          <span className="muted">Visible to the whole family team</span>
        </div>

        {selectedNoteIds.length > 0 && (
          <div
            className="bulk-action-bar"
            style={{
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
              padding: "0.6rem 1rem",
              marginBottom: "1rem",
              background: "rgba(239, 68, 68, 0.12)",
              border: "1px solid rgba(239, 68, 68, 0.3)",
              borderRadius: "8px",
            }}
          >
            <span>
              <strong>{selectedNoteIds.length}</strong> note(s) selected
            </span>
            <div style={{ display: "flex", gap: "0.5rem" }}>
              <button
                type="button"
                className="button button-danger"
                style={{
                  backgroundColor: "#ef4444",
                  color: "#fff",
                  border: "none",
                  padding: "0.4rem 0.8rem",
                  borderRadius: "6px",
                  cursor: "pointer",
                  fontWeight: 600,
                }}
                onClick={handleBulkDeleteNotes}
                disabled={submitting}
              >
                {submitting ? "Deleting..." : `Delete Selected (${selectedNoteIds.length})`}
              </button>
              <button
                type="button"
                className="button button-secondary"
                style={{ padding: "0.4rem 0.8rem", borderRadius: "6px" }}
                onClick={() => setSelectedNoteIds([])}
              >
                Clear
              </button>
            </div>
          </div>
        )}

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
            notes.map((note) => {
              const isSelected = selectedNoteIds.includes(note.id);
              return (
                <article
                  className={`note-card ${isSelected ? "note-card-selected" : ""}`}
                  key={note.id}
                  style={{
                    display: "flex",
                    alignItems: "flex-start",
                    gap: "0.75rem",
                    border: isSelected ? "1px solid rgba(239, 68, 68, 0.5)" : undefined,
                  }}
                >
                  <input
                    type="checkbox"
                    checked={isSelected}
                    onChange={(e) => handleToggleSelectNote(note.id, e)}
                    aria-label={`Select customer note ${note.id}`}
                    style={{ cursor: "pointer", width: "18px", height: "18px", marginTop: "0.25rem", flexShrink: 0 }}
                  />
                  <div style={{ flex: 1 }}>
                    <p style={{ margin: "0 0 0.5rem 0" }}>
                      {note.note_text || note.note || note.description || "Note text unavailable"}
                    </p>
                    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                      <small>
                        By {note.created_by_name || note.author_name || note.creator_name || "Family member"} ·{" "}
                        {formatDate(note.created_at)}
                      </small>
                      <button
                        type="button"
                        onClick={() => handleDeleteSingleNote(note.id)}
                        style={{
                          background: "none",
                          border: "none",
                          color: "#ef4444",
                          cursor: "pointer",
                          fontSize: "0.8rem",
                          padding: "0.2rem 0.4rem",
                        }}
                      >
                        Delete
                      </button>
                    </div>
                  </div>
                </article>
              );
            })
          )}
        </div>
      </section>
    </div>
  );
}

export default CustomerDetails;
