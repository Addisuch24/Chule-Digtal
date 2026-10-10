const businessNoteModel = require("../models/businessNoteModel");
const activityService = require("./activityService");

async function createBusinessNote({ noteText, createdBy }) {
  const note = await businessNoteModel.createBusinessNote({
    noteText,
    createdBy,
  });

  await activityService.recordActivity({
    actorId: createdBy,
    activityType: "BUSINESS_NOTE_CREATED",
    entityType: "BUSINESS_NOTE",
    entityId: note.id,
    description: "A new business note was added",
    metadata: {
      noteId: note.id,
    },
  });

  return note;
}

async function getAllBusinessNotes() {
  return await businessNoteModel.getAllBusinessNotes();
}

module.exports = {
  createBusinessNote,
  getAllBusinessNotes,
};
