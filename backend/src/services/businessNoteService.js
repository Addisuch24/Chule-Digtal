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

async function updateBusinessNote({ noteId, noteText, updatedBy }) {
  const note = await businessNoteModel.updateBusinessNote({
    noteId,
    noteText,
  });

  if (!note) {
    return null;
  }

  await activityService.recordActivity({
    actorId: updatedBy,
    activityType: "BUSINESS_NOTE_UPDATED",
    entityType: "BUSINESS_NOTE",
    entityId: note.id,
    description: "A business note was updated",
    metadata: {
      noteId: note.id,
    },
  });

  return note;
}

async function deleteBusinessNote({ noteId, deletedBy }) {
  const deleted = await businessNoteModel.deleteBusinessNote(noteId);
  if (!deleted) {
    return false;
  }

  await activityService.recordActivity({
    actorId: deletedBy,
    activityType: "BUSINESS_NOTE_DELETED",
    entityType: "BUSINESS_NOTE",
    entityId: noteId,
    description: "A business note was deleted",
    metadata: {
      noteId,
    },
  });

  return true;
}

async function deleteBusinessNotes({ noteIds, deletedBy }) {
  const result = await businessNoteModel.deleteBusinessNotes(noteIds);
  if (result.deletedCount > 0) {
    await activityService.recordActivity({
      actorId: deletedBy,
      activityType: "BUSINESS_NOTE_BULK_DELETED",
      entityType: "BUSINESS_NOTE",
      entityId: noteIds[0],
      description: `${result.deletedCount} business notes were deleted`,
      metadata: {
        deletedCount: result.deletedCount,
        noteIds,
      },
    });
  }
  return result;
}

module.exports = {
  createBusinessNote,
  getAllBusinessNotes,
  updateBusinessNote,
  deleteBusinessNote,
  deleteBusinessNotes,
};
