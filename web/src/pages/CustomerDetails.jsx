import { useEffect, useState } from "react";
import { useParams, Link } from "react-router-dom";
import api from "../services/api";
function formatNoteDate(dateValue) {
  const date = new Date(dateValue);

  const dayName = date.toLocaleDateString("en-US", {
    weekday: "long",
  });

  const day = date.getDate();
  const month = date.getMonth() + 1;
  const year = date.getFullYear();

  const time = date.toLocaleTimeString("en-US", {
    hour: "numeric",
    minute: "2-digit",
    hour12: true,
  });

  return {
    date: `${dayName}, ${day}/${month}/${year}`,
    time,
  };
}
function CustomerDetails() {
    
  const { customerId } = useParams();

  const [customer, setCustomer] = useState(null);
  const [notes, setNotes] = useState([]);
  const [error, setError] = useState("");
const [noteText, setNoteText] = useState("");
const [message, setMessage] = useState("");
async function handleAddNote(event) {
  event.preventDefault();

  setMessage("");
  setError("");

  try {
    await api.post(
  `/customers/${customerId}/notes`,
  {
    note_text: noteText,
  }
);

    setMessage("Note added successfully!");
    setNoteText("");

    const notesResponse = await api.get(
      `/customers/${customerId}/notes`
    );

    setNotes(notesResponse.data.data);
  } catch (error) {
    setError(
      error.response?.data?.message ||
        "Failed to add note"
    );
  }
}
  useEffect(() => {
    async function loadCustomer() {
      try {
        const customerResponse = await api.get(
          `/customers/${customerId}`
        );

        setCustomer(customerResponse.data.data);

        const notesResponse = await api.get(
          `/customers/${customerId}/notes`
        );

        setNotes(notesResponse.data.data);
      } catch (error) {
        setError(
          error.response?.data?.message ||
            "Failed to load customer"
        );
      }
    }

    loadCustomer();
  }, [customerId]);

  if (error) {
    return <p>{error}</p>;
  }

  if (!customer) {
    return <p>Loading customer...</p>;
  }

  return (
    <div>
      <Link to="/customers">← Back to Customers</Link>

      <h2>{customer.full_name}</h2>

      <p>
        <strong>Phone:</strong>{" "}
        {customer.phone || "N/A"}
      </p>

      <p>
        <strong>Balance:</strong>{" "}
        {customer.balance}
      </p>

      <hr />
<h3>Add Note</h3>

<form onSubmit={handleAddNote}>
  <textarea
    value={noteText}
    onChange={(event) =>
      setNoteText(event.target.value)
    }
    placeholder="Write customer note..."
    required
  />

  <br />

  <button type="submit">
    Save Note
  </button>
</form>

{message && <p>{message}</p>}
      <h3>Customer Notes</h3>

      {notes.length === 0 ? (
        <p>No notes found.</p>
      ) : (
        notes.map((note) => (
          <div key={note.id}>
<p>
  {note.note ||
    note.description ||
    note.note_text ||
    "Note text unavailable"}
</p>            <small>
              Created: {formatNoteDate(note.created_at).date} at {formatNoteDate(note.created_at).time}
            </small>
            <hr />
          </div>
        ))
      )}
    </div>
  );
}

export default CustomerDetails;