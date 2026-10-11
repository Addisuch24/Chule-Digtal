import { useCallback, useEffect, useMemo, useRef, useState } from "react";
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

function noteHeading(noteText) {
  return noteText.trim().split(/\r?\n/, 1)[0] || "Untitled note";
}

function Customers() {
  const [notes, setNotes] = useState([]);
  const [noteText, setNoteText] = useState("");
  const [search, setSearch] = useState("");
  const [selectedNoteId, setSelectedNoteId] = useState(null);
  const [editingNoteId, setEditingNoteId] = useState(null);
  const [openMenuNoteId, setOpenMenuNoteId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const noteList = useRef(null);

  const loadNotes = useCallback(async (showSpinner = false) => {
    try {
      if (showSpinner) setLoading(true);
      setError("");
      const response = await api.get("/notes");
      setNotes(Array.isArray(response.data?.data) ? response.data.data : []);
    } catch (requestError) {
      console.error("Load notes error:", requestError);
      setError(
        requestError.response?.data?.message || "We could not load notes."
      );
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadNotes();
  }, [loadNotes]);

  useEffect(() => {
    if (openMenuNoteId === null) return undefined;

    function handleMenuDismiss(event) {
      if (event.type === "keydown" && event.key === "Escape") {
        setOpenMenuNoteId(null);
      } else if (
        event.type === "pointerdown" &&
        !noteList.current?.contains(event.target)
      ) {
        setOpenMenuNoteId(null);
      }
    }

    document.addEventListener("keydown", handleMenuDismiss);
    document.addEventListener("pointerdown", handleMenuDismiss);
    return () => {
      document.removeEventListener("keydown", handleMenuDismiss);
      document.removeEventListener("pointerdown", handleMenuDismiss);
    };
  }, [openMenuNoteId]);

  async function handleSaveNote(event) {
    event.preventDefault();
    setMessage("");
    setError("");
    if (!noteText.trim()) {
      setError("Please write something in your note.");
      return;
    }

    try {
      setSubmitting(true);
      if (editingNoteId !== null) {
        await api.patch(`/notes/${editingNoteId}`, {
          note_text: noteText.trim(),
        });
        setMessage("Note updated successfully.");
        setSelectedNoteId(editingNoteId);
      } else {
        const response = await api.post("/notes", {
          note_text: noteText.trim(),
        });
        const createdNote = response.data?.data;
        if (!createdNote?.id) {
          throw new Error("The server did not return a valid note.");
        }
        setMessage("Note saved successfully.");
        setSelectedNoteId(createdNote.id);
      }
      setNoteText("");
      setEditingNoteId(null);
      await loadNotes();
    } catch (requestError) {
      console.error("Save note error:", requestError);
      setError(
        requestError.response?.data?.message ||
          requestError.message ||
          "We could not save this note."
      );
    } finally {
      setSubmitting(false);
    }
  }

  function startNewNote() {
    setNoteText("");
    setEditingNoteId(null);
    setSelectedNoteId(null);
    setMessage("");
    setError("");
  }

  function startEditingNote(note) {
    setNoteText(note.note_text || "");
    setEditingNoteId(note.id);
    setSelectedNoteId(null);
    setOpenMenuNoteId(null);
    setMessage("");
    setError("");
  }

  async function handleDeleteNote(note) {
    if (
      !window.confirm(
        `Delete "${noteHeading(note.note_text)}"? This cannot be undone.`
      )
    ) {
      return;
    }

    try {
      await api.delete(`/notes/${note.id}`);
      setNotes((currentNotes) =>
        currentNotes.filter((currentNote) => currentNote.id !== note.id)
      );
      if (selectedNoteId === note.id) setSelectedNoteId(null);
      if (editingNoteId === note.id) {
        setEditingNoteId(null);
        setNoteText("");
      }
      setOpenMenuNoteId(null);
      setMessage("Note deleted successfully.");
      setError("");
    } catch (requestError) {
      console.error("Delete note error:", requestError);
      setError(
        requestError.response?.data?.message || "We could not delete this note."
      );
    }
  }

  const filteredNotes = useMemo(() => {
    const query = search.trim().toLowerCase();
    if (!query) return notes;
    return notes.filter((note) =>
      `${note.note_text || ""} ${note.created_by_name || ""}`
        .toLowerCase()
        .includes(query)
    );
  }, [notes, search]);

  const selectedNote = notes.find((note) => note.id === selectedNoteId);

  return (
    <div className="page-stack">
      <PageHeader
        eyebrow="የግል ማስታወሻ"
        title="ማስታወሻዎች"
        description="ሀሳቦችንና አስፈላጊ መረጃዎችን እዚህ ይመዝግቡ።"
        action={<span className="count-label">{notes.length} notes</span>}
      />

      <section className="panel">
        <div className="section-heading">
          <div>
            <p className="eyebrow">{editingNoteId !== null ? "Edit note" : "New note"}</p>
            <h2>{editingNoteId !== null ? "Update your note" : "Write a note"}</h2>
          </div>
          {editingNoteId !== null && (
            <button
              className="button-secondary"
              type="button"
              onClick={startNewNote}
            >
              Cancel
            </button>
          )}
        </div>
        <form className="form-stack" onSubmit={handleSaveNote}>
          <label>
            Note
            <textarea
              value={noteText}
              onChange={(event) => setNoteText(event.target.value)}
              placeholder="Write down an idea or important information..."
              rows="5"
              maxLength={10000}
              required
            />
          </label>
          <div className="action-row">
            <button type="submit" disabled={submitting}>
              {submitting
                ? "Saving..."
                : editingNoteId !== null
                  ? "Save changes"
                  : "Save note"}
            </button>
          </div>
        </form>
        {message && <p className="alert alert-success">{message}</p>}
        {error && <p className="alert alert-error">{error}</p>}
      </section>

      <section className="panel" ref={noteList}>
        <div className="section-heading">
          <div>
            <p className="eyebrow">Your notes</p>
            <h2>All notes</h2>
          </div>
          <input
            className="search-input"
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="Search notes"
            aria-label="Search notes"
          />
        </div>

        {loading ? (
          <FeedbackState type="loading" title="Loading notes..." />
        ) : error && notes.length === 0 ? (
          <FeedbackState
            type="error"
            title="Notes unavailable"
            message={error}
            action={
              <button type="button" onClick={() => loadNotes(true)}>
                Try again
              </button>
            }
          />
        ) : filteredNotes.length === 0 ? (
          <FeedbackState
            title={search ? "No matching notes" : "No notes yet"}
            message={search ? "Try another search." : "Write your first note above."}
          />
        ) : (
          <div className="business-note-list">
            {filteredNotes.map((note) => {
              const [heading, ...remainingLines] = String(note.note_text || "")
                .split(/\r?\n/);
              const preview = remainingLines.join(" ").trim() || heading;
              return (
                <article className="business-note-card" key={note.id}>
                  <button
                    className="business-note-open"
                    type="button"
                    onClick={() => {
                      setSelectedNoteId(note.id);
                      setEditingNoteId(null);
                      setMessage("");
                      setError("");
                    }}
                  >
                    <strong>{noteHeading(note.note_text)}</strong>
                    <span>{preview}</span>
                    <small>
                      {note.created_by_name || "Family member"} ·{" "}
                      {formatDate(note.updated_at || note.created_at)}
                    </small>
                  </button>
                  <div className="order-note-menu">
                    <button
                      className="order-menu-trigger"
                      type="button"
                      aria-label={`More actions for ${noteHeading(note.note_text)}`}
                      aria-haspopup="menu"
                      aria-expanded={openMenuNoteId === note.id}
                      onClick={() =>
                        setOpenMenuNoteId(
                          openMenuNoteId === note.id ? null : note.id
                        )
                      }
                    >
                      &hellip;
                    </button>
                    {openMenuNoteId === note.id && (
                      <div className="order-menu" role="menu">
                        <button
                          type="button"
                          role="menuitem"
                          onClick={() => startEditingNote(note)}
                        >
                          Edit
                        </button>
                        <button
                          className="order-menu-delete"
                          type="button"
                          role="menuitem"
                          onClick={() => handleDeleteNote(note)}
                        >
                          Delete
                        </button>
                      </div>
                    )}
                  </div>
                </article>
              );
            })}
          </div>
        )}
      </section>

      {selectedNote && (
        <section
          className="business-note-reader-section"
          aria-label="Selected note"
        >
          <div className="section-heading">
            <h2>{noteHeading(selectedNote.note_text)}</h2>
            <button
              className="button-secondary"
              type="button"
              onClick={() => setSelectedNoteId(null)}
            >
              Close
            </button>
          </div>
          <article className="order-reader">
            <h3 className="order-reader-title">
              {noteHeading(selectedNote.note_text)}
            </h3>
            <p className="order-reader-body">{selectedNote.note_text}</p>
            <footer className="order-reader-meta">
              <span>
                Written by <strong>{selectedNote.created_by_name || "Family member"}</strong>
              </span>
              <time dateTime={selectedNote.updated_at || selectedNote.created_at}>
                {formatDate(selectedNote.updated_at || selectedNote.created_at)}
              </time>
            </footer>
          </article>
        </section>
      )}
    </div>
  );
}

export default Customers;
