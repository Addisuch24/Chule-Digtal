const businessNoteService = require("../services/businessNoteService");

async function createBusinessNote(req, res) {
  try {
    const { note_text } = req.body;
    const createdBy = req.user.id;

    if (!note_text || !note_text.trim()) {
      return res.status(400).json({
        success: false,
        message: "Note text is required",
      });
    }

    if (note_text.length > 10000) {
      return res.status(400).json({
        success: false,
        message: "Note text is too long (maximum 10,000 characters)",
      });
    }

    if (!Number.isInteger(Number(createdBy)) || createdBy <= 0) {
      return res.status(401).json({
        success: false,
        message: "Authentication required",
      });
    }

    const note = await businessNoteService.createBusinessNote({
      noteText: note_text.trim(),
      createdBy: Number(createdBy),
    });

    return res.status(201).json({
      success: true,
      message: "Business note created successfully",
      data: note,
    });
  } catch (error) {
    console.error("Create business note error:", error);
    return res.status(500).json({
      success: false,
      message: "Failed to create business note",
    });
  }
}

async function getAllBusinessNotes(req, res) {
  try {
    const notes = await businessNoteService.getAllBusinessNotes();

    return res.status(200).json({
      success: true,
      message: "Business notes retrieved successfully",
      data: notes,
    });
  } catch (error) {
    console.error("Get business notes error:", error);
    return res.status(500).json({
      success: false,
      message: "Failed to retrieve business notes",
    });
  }
}

async function updateBusinessNote(req, res) {
  try {
    const { noteId } = req.params;
    const { note_text: noteText } = req.body;
    const updatedBy = Number(req.user.id);

    if (!/^\d+$/.test(noteId) || Number(noteId) <= 0) {
      return res.status(400).json({
        success: false,
        message: "noteId must be a valid note ID",
      });
    }

    if (!noteText || !noteText.trim()) {
      return res.status(400).json({
        success: false,
        message: "Note text is required",
      });
    }

    if (noteText.length > 10000) {
      return res.status(400).json({
        success: false,
        message: "Note text is too long (maximum 10,000 characters)",
      });
    }

    const note = await businessNoteService.updateBusinessNote({
      noteId: Number(noteId),
      noteText: noteText.trim(),
      updatedBy,
    });

    if (!note) {
      return res.status(404).json({
        success: false,
        message: "Business note not found",
      });
    }

    return res.status(200).json({
      success: true,
      message: "Business note updated successfully",
      data: note,
    });
  } catch (error) {
    console.error("Update business note error:", error);
    return res.status(500).json({
      success: false,
      message: "Failed to update business note",
    });
  }
}

async function deleteBusinessNote(req, res) {
  try {
    const { noteId } = req.params;
    const deletedBy = Number(req.user.id);

    if (!/^\d+$/.test(noteId) || Number(noteId) <= 0) {
      return res.status(400).json({
        success: false,
        message: "noteId must be a valid note ID",
      });
    }

    const deleted = await businessNoteService.deleteBusinessNote({
      noteId: Number(noteId),
      deletedBy,
    });

    if (!deleted) {
      return res.status(404).json({
        success: false,
        message: "Business note not found",
      });
    }

    return res.status(200).json({
      success: true,
      message: "Business note deleted successfully",
    });
  } catch (error) {
    console.error("Delete business note error:", error);
    return res.status(500).json({
      success: false,
      message: "Failed to delete business note",
    });
  }
}

async function deleteBusinessNotes(req, res) {
  try {
    const rawIds = req.body.noteIds || req.body.ids;
    const deletedBy = Number(req.user.id);

    if (!Array.isArray(rawIds) || rawIds.length === 0) {
      return res.status(400).json({
        success: false,
        message: "An array of note IDs ('noteIds' or 'ids') is required",
      });
    }

    if (rawIds.length > 100) {
      return res.status(400).json({
        success: false,
        message: "Cannot delete more than 100 notes at a time",
      });
    }

    const validIds = rawIds
      .map((id) => Number(id))
      .filter((id) => Number.isInteger(id) && id > 0);

    if (validIds.length === 0) {
      return res.status(400).json({
        success: false,
        message: "No valid note IDs provided",
      });
    }

    const result = await businessNoteService.deleteBusinessNotes({
      noteIds: validIds,
      deletedBy,
    });

    return res.status(200).json({
      success: true,
      message: `${result.deletedCount} notes deleted successfully`,
      data: result,
    });
  } catch (error) {
    console.error("Bulk delete business notes error:", error);
    return res.status(500).json({
      success: false,
      message: "Failed to delete business notes",
    });
  }
}

module.exports = {
  createBusinessNote,
  getAllBusinessNotes,
  updateBusinessNote,
  deleteBusinessNote,
  deleteBusinessNotes,
};
