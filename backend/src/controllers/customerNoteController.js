 const activityService = require("../services/activityService");
const customerNoteModel = require("../models/customerNoteModel");
const customerModel = require("../models/customerModel");

async function createCustomerNote(req, res) {
  try {
    const { customerId } = req.params;
    const { note_text } = req.body;
    const createdBy = req.user.id;

    if (
      !Number.isInteger(Number(customerId)) ||
      Number(customerId) <= 0
    ) {
      return res.status(400).json({
        success: false,
        message: "customerId must be a valid customer ID",
      });
    }

    if (!note_text || !note_text.trim()) {
      return res.status(400).json({
        success: false,
        message: "Note text is required",
      });
    }

    if (
      !Number.isInteger(Number(createdBy)) ||
      Number(createdBy) <= 0
    ) {
      return res.status(400).json({
        success: false,
        message: "createdBy must be a valid family member ID",
      });
    }

    const familyMemberExists =
      await customerModel.familyMemberExists(Number(createdBy));

    if (!familyMemberExists) {
      return res.status(400).json({
        success: false,
        message: "Family member not found or inactive",
      });
    }

    const note = await customerNoteModel.createCustomerNote({
      customerId: Number(customerId),
      noteText: note_text.trim(),
      createdBy: Number(createdBy),
    });

    await activityService.recordActivity({
      actorId: Number(createdBy),
      activityType: "CUSTOMER_NOTE_CREATED",
      entityType: "CUSTOMER_NOTE",
      entityId: note.id,
      description: "Customer note was created",
      metadata: {
        customerId: Number(customerId),
      },
    });

    return res.status(201).json({
      success: true,
      message: "Customer note created successfully",
      data: note,
    });
  } catch (error) {
    console.error("Create customer note error:", error);

    return res.status(500).json({
      success: false,
      message: "Failed to create customer note",
    });
  }
}

async function getCustomerNotes(req, res) {
  try {
    const { customerId } = req.params;

    if (
      !Number.isInteger(Number(customerId)) ||
      Number(customerId) <= 0
    ) {
      return res.status(400).json({
        success: false,
        message: "customerId must be a valid customer ID",
      });
    }

    const notes = await customerNoteModel.getCustomerNotes(
      Number(customerId)
    );

    return res.status(200).json({
      success: true,
      message: "Customer notes retrieved successfully",
      data: notes,
    });
  } catch (error) {
    console.error("Get customer notes error:", error);

    return res.status(500).json({
      success: false,
      message: "Failed to retrieve customer notes",
    });
  }
}

module.exports = {
  createCustomerNote,
  getCustomerNotes,
};