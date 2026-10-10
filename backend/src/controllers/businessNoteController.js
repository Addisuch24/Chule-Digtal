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

module.exports = {
  createBusinessNote,
  getAllBusinessNotes,
};
