import { useCallback, useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import api from "../services/api";
import FeedbackState from "../components/FeedbackState";
import PageHeader from "../components/PageHeader";

const formatDate = (value) =>
  value
    ? new Date(value).toLocaleDateString("en-US", { dateStyle: "medium" })
    : "Not available";

function Customers() {
  const [customers, setCustomers] = useState([]);
  const [fullName, setFullName] = useState("");
  const [phone, setPhone] = useState("");
  const [noteText, setNoteText] = useState("");
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  const loadCustomers = useCallback(async (showSpinner = false) => {
    try {
      if (showSpinner) setLoading(true);
      setError("");
      const response = await api.get("/customers");
      setCustomers(
        Array.isArray(response.data?.data) ? response.data.data : []
      );
    } catch (requestError) {
      console.error("Load customers error:", requestError);
      setError(
        requestError.response?.data?.message || "We could not load customers."
      );
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadCustomers(false);
  }, [loadCustomers]);

  async function handleCreateCustomer(event) {
    event.preventDefault();
    setMessage("");
    setError("");
    if (!fullName.trim()) {
      setError("Please enter the customer's full name.");
      return;
    }
    try {
      setSubmitting(true);
      const customerResponse = await api.post("/customers", {
        full_name: fullName.trim(),
        phone: phone.trim() || null,
      });
      const createdCustomer = customerResponse.data?.data;

      if (noteText.trim() && createdCustomer?.id) {
        try {
          await api.post(`/customers/${createdCustomer.id}/notes`, {
            note_text: noteText.trim(),
          });
          setMessage("Customer and note added successfully.");
        } catch (noteError) {
          console.error("Create customer note error:", noteError);
          setMessage("Customer added, but the note could not be saved.");
          setError(
            noteError.response?.data?.message ||
              "Please add the note from the customer details page."
          );
        }
      } else {
        setMessage("Customer added successfully.");
      }

      setFullName("");
      setPhone("");
      setNoteText("");
      await loadCustomers();
    } catch (requestError) {
      console.error("Create customer error:", requestError);
      setError(
        requestError.response?.data?.message || "We could not add that customer."
      );
    } finally {
      setSubmitting(false);
    }
  }

  const filteredCustomers = useMemo(() => {
    const query = search.trim().toLowerCase();
    return customers.filter((customer) => {
      if (!query) return true;
      return `${customer.full_name || ""} ${customer.phone || ""}`
        .toLowerCase()
        .includes(query);
    });
  }, [customers, search]);

  return (
    <div className="page-stack">
      <PageHeader
        eyebrow="Customer records"
        title="Customers"
        description="Keep one clear, shared record for every customer relationship."
        action={<span className="count-label">{customers.length} total</span>}
      />
      <section className="panel">
        <div className="section-heading">
          <div>
            <p className="eyebrow">New record</p>
            <h2>Add a customer</h2>
          </div>
          <span className="muted">Required fields are marked by the browser</span>
        </div>
        <form className="form-stack" onSubmit={handleCreateCustomer}>
          <div className="form-grid">
            <label>
              Full name
              <input
                value={fullName}
                onChange={(event) => setFullName(event.target.value)}
                placeholder="e.g. Hana Tesfaye"
                autoComplete="name"
                required
              />
            </label>
            <label>
              Phone <span className="muted">(optional)</span>
              <input
                value={phone}
                onChange={(event) => setPhone(event.target.value)}
                placeholder="e.g. 09..."
                autoComplete="tel"
              />
            </label>
          </div>
          <label>
            Initial note <span className="muted">(optional)</span>
            <textarea
              value={noteText}
              onChange={(event) => setNoteText(event.target.value)}
              placeholder="Add useful context for the family team..."
              rows="3"
            />
          </label>
          <div>
            <button type="submit" disabled={submitting}>
              {submitting ? "Saving..." : "Add customer"}
            </button>
          </div>
        </form>
        {message && <p className="alert alert-success">{message}</p>}
        {error && <p className="alert alert-error">{error}</p>}
      </section>

      <section className="panel">
        <div className="section-heading">
          <div>
            <p className="eyebrow">Directory</p>
            <h2>All customers</h2>
          </div>
          <input
            className="search-input"
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="Search name or phone"
            aria-label="Search customers"
          />
        </div>

        {loading ? (
          <FeedbackState type="loading" title="Loading customers..." />
        ) : filteredCustomers.length === 0 ? (
          <FeedbackState
            title={search ? "No matching customers" : "No customers yet"}
            message={
              search
                ? "Try a different name or phone number."
                : "Add your first customer above to begin."
            }
          />
        ) : (
          <div className="data-table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Customer</th>
                  <th>Phone</th>
                  <th>Balance</th>
                  <th>Created by</th>
                  <th>Added</th>
                  <th>
                    <span className="sr-only">Open</span>
                  </th>
                </tr>
              </thead>
              <tbody>
                {filteredCustomers.map((customer) => (
                  <tr key={customer.id}>
                    <td>
                      <Link
                        className="table-primary"
                        to={`/customers/${customer.id}`}
                      >
                        {customer.full_name || "Unnamed customer"}
                      </Link>
                      <small>Customer #{customer.id}</small>
                    </td>
                    <td>{customer.phone || "Not provided"}</td>
                    <td>
                      {Number(customer.balance || 0).toLocaleString("en-US")}{" "}
                      ETB
                    </td>
                    <td>
                      {customer.created_by_name ||
                        customer.creator_name ||
                        "Family member"}
                    </td>
                    <td>{formatDate(customer.created_at)}</td>
                    <td>
                      <Link
                        className="table-link"
                        to={`/customers/${customer.id}`}
                      >
                        View
                      </Link>
                    </td>
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

export default Customers;

