
const activityService = require("../services/activityService");
const financialMovementModel = require("../models/financialMovementModel");

async function createFinancialMovement(req, res) {
  try {
    const {
      customerId,
      movementType,
      amount,
      description,
      referenceType,
      referenceId,
      transactionAt,
      idempotencyKey,
    } = req.body;

    const recordedBy = req.user.id;

    // Validate customer ID
    if (
      !Number.isInteger(Number(customerId)) ||
      Number(customerId) <= 0
    ) {
      return res.status(400).json({
        success: false,
        message: "customerId must be a valid customer ID",
      });
    }

    // Check that the customer exists
    const customers = await require("../config/db").query(
      `
        SELECT id
        FROM customers
        WHERE id = $1
      `,
      [Number(customerId)]
    );

    if (customers.rows.length === 0) {
      return res.status(400).json({
        success: false,
        message: "Customer not found",
      });
    }

    // Validate amount
    if (
      amount === undefined ||
      amount === null ||
      Number.isNaN(Number(amount)) ||
      Number(amount) <= 0
    ) {
      return res.status(400).json({
        success: false,
        message: "amount must be greater than 0",
      });
    }

    // Validate movement type
    const allowedMovementTypes = ["DEBIT", "CREDIT"];

    if (!allowedMovementTypes.includes(movementType)) {
      return res.status(400).json({
        success: false,
        message: "movementType must be DEBIT or CREDIT",
      });
    }

    // Validate authenticated family member
    if (
      !Number.isInteger(Number(recordedBy)) ||
      Number(recordedBy) <= 0
    ) {
      return res.status(401).json({
        success: false,
        message: "Authenticated user is invalid",
      });
    }

    // Check that authenticated family member exists and is active
    const familyMembers = await require("../config/db").query(
      `
        SELECT id
        FROM family_members
        WHERE id = $1
          AND is_active = 1
      `,
      [Number(recordedBy)]
    );

    if (familyMembers.rows.length === 0) {
      return res.status(401).json({
        success: false,
        message: "Authenticated family member not found or inactive",
      });
    }

    const movement =
      await financialMovementModel.createFinancialMovement({
        customerId: Number(customerId),
        amount: Number(amount),
        movementType,
        description: description || null,
        referenceType: referenceType || null,
        referenceId:
          referenceId === undefined || referenceId === null
            ? null
            : Number(referenceId),
        idempotencyKey: idempotencyKey || null,
        recordedBy: Number(recordedBy),
        transactionAt: transactionAt || null,
      });

    if (movement.duplicate) {
      return res.status(200).json({
        success: true,
        message: "Financial movement already recorded",
        data: movement,
      });
    }

    await activityService.recordActivity({
      actorId: Number(recordedBy),
      activityType: "FINANCIAL_MOVEMENT_CREATED",
      entityType: "FINANCIAL_MOVEMENT",
      entityId: movement.id,
      description: `Financial movement of ${movement.amount} birr was recorded`,
      metadata: {
        customerId: Number(customerId),
        movementType,
      },
    });

    return res.status(201).json({
      success: true,
      message: "Financial movement created successfully",
      data: movement,
    });
  } catch (error) {
    console.error("Create financial movement error:", error);

    return res.status(500).json({
      success: false,
      message: "Failed to create financial movement",
    });
  }
}

async function getFinancialMovementById(req, res) {
  try {
    const { movementId } = req.params;

    if (
      !Number.isInteger(Number(movementId)) ||
      Number(movementId) <= 0
    ) {
      return res.status(400).json({
        success: false,
        message: "movementId must be a valid movement ID",
      });
    }

    const movement =
      await financialMovementModel.getFinancialMovementById(
        Number(movementId)
      );

    if (!movement) {
      return res.status(404).json({
        success: false,
        message: "Financial movement not found",
      });
    }

    return res.status(200).json({
      success: true,
      data: movement,
    });
  } catch (error) {
    console.error("Get financial movement error:", error);

    return res.status(500).json({
      success: false,
      message: "Failed to get financial movement",
    });
  }
}

async function getAllFinancialMovements(req, res) {
  try {
    const movements =
      await financialMovementModel.getAllFinancialMovements();

    return res.status(200).json({
      success: true,
      data: movements,
    });
  } catch (error) {
    console.error("Get all financial movements error:", error);

    return res.status(500).json({
      success: false,
      message: "Failed to get financial movements",
    });
  }
}

async function updateFinancialMovement(req, res) {
  try {
    const { movementId } = req.params;
    const {
      customerId,
      amount,
      movementType,
      description,
    } = req.body;

    if (
      !Number.isInteger(Number(movementId)) ||
      Number(movementId) <= 0
    ) {
      return res.status(400).json({
        success: false,
        message: "movementId must be a valid movement ID",
      });
    }

    if (
      customerId === undefined ||
      customerId === null ||
      !Number.isInteger(Number(customerId)) ||
      Number(customerId) <= 0
    ) {
      return res.status(400).json({
        success: false,
        message: "customerId must be a valid customer ID",
      });
    }

    if (
      amount === undefined ||
      amount === null ||
      Number.isNaN(Number(amount)) ||
      Number(amount) <= 0
    ) {
      return res.status(400).json({
        success: false,
        message: "amount must be greater than 0",
      });
    }

    const allowedMovementTypes = ["DEBIT", "CREDIT"];
    if (
      movementType === undefined ||
      movementType === null ||
      !allowedMovementTypes.includes(movementType)
    ) {
      return res.status(400).json({
        success: false,
        message: "movementType must be DEBIT or CREDIT",
      });
    }

    const result =
      await financialMovementModel.updateFinancialMovement({
        movementId: Number(movementId),
        customerId: Number(customerId),
        amount: Number(amount),
        movementType,
        description,
      });

    if (result && result.blocked) {
      return res.status(409).json({
        success: false,
        message: result.reason || "Verified financial records cannot be modified",
      });
    }

    if (!result || result.notFound || !result.data) {
      return res.status(404).json({
        success: false,
        message: "Financial movement not found",
      });
    }

    return res.status(200).json({
      success: true,
      message: "Financial movement updated successfully",
      data: result.data,
    });
  } catch (error) {
    console.error("Update financial movement error:", error);

    return res.status(500).json({
      success: false,
      message: "Failed to update financial movement",
    });
  }
}

async function deleteFinancialMovement(req, res) {
  try {
    const { movementId } = req.params;

    if (
      !Number.isInteger(Number(movementId)) ||
      Number(movementId) <= 0
    ) {
      return res.status(400).json({
        success: false,
        message: "movementId must be a valid movement ID",
      });
    }

    const result = await financialMovementModel.deleteFinancialMovement(
      Number(movementId)
    );

    if (result.blocked) {
      return res.status(400).json({
        success: false,
        message: result.reason,
      });
    }

    if (!result.success) {
      return res.status(404).json({
        success: false,
        message: "Financial movement not found",
      });
    }

    await activityService.recordActivity({
      actorId: Number(req.user.id),
      activityType: "FINANCIAL_MOVEMENT_DELETED",
      entityType: "FINANCIAL_MOVEMENT",
      entityId: Number(movementId),
      description: `Financial movement #${movementId} was deleted`,
      metadata: { movementId: Number(movementId) },
    });

    return res.status(200).json({
      success: true,
      message: "Financial movement deleted successfully",
    });
  } catch (error) {
    console.error("Delete financial movement error:", error);

    return res.status(500).json({
      success: false,
      message: "Failed to delete financial movement",
    });
  }
}

async function deleteFinancialMovements(req, res) {
  try {
    const rawIds = req.body.movementIds || req.body.ids;
    const deletedBy = Number(req.user.id);

    if (!Array.isArray(rawIds) || rawIds.length === 0) {
      return res.status(400).json({
        success: false,
        message: "An array of movement IDs ('movementIds' or 'ids') is required",
      });
    }

    if (rawIds.length > 100) {
      return res.status(400).json({
        success: false,
        message: "Cannot delete more than 100 movements at a time",
      });
    }

    const validIds = rawIds
      .map((id) => Number(id))
      .filter((id) => Number.isInteger(id) && id > 0);

    if (validIds.length === 0) {
      return res.status(400).json({
        success: false,
        message: "No valid movement IDs provided",
      });
    }

    const result = await financialMovementModel.deleteFinancialMovements(validIds);

    if (result.deletedCount > 0) {
      await activityService.recordActivity({
        actorId: deletedBy,
        activityType: "FINANCIAL_MOVEMENT_BULK_DELETED",
        entityType: "FINANCIAL_MOVEMENT",
        entityId: result.deletedIds[0],
        description: `${result.deletedCount} financial movement(s) were deleted`,
        metadata: {
          deletedCount: result.deletedCount,
          movementIds: result.deletedIds,
          skipped: result.skipped,
        },
      });
    }

    let message = `${result.deletedCount} financial movement(s) deleted successfully`;
    if (result.skipped && result.skipped.length > 0) {
      message = `${result.deletedCount} deleted, ${result.skipped.length} skipped (verified records cannot be deleted)`;
    }

    return res.status(200).json({
      success: true,
      message,
      data: result,
    });
  } catch (error) {
    console.error("Bulk delete financial movements error:", error);
    return res.status(500).json({
      success: false,
      message: "Failed to delete financial movements",
    });
  }
}

async function verifyFinancialMovement(req, res) {
  try {
    const { movementId } = req.params;
    const { verificationNote } = req.body;

    const verifiedBy = req.user.id;

    // Validate movement ID
    if (
      !Number.isInteger(Number(movementId)) ||
      Number(movementId) <= 0
    ) {
      return res.status(400).json({
        success: false,
        message: "movementId must be a valid movement ID",
      });
    }

    // Validate authenticated verifier
    if (
      !Number.isInteger(Number(verifiedBy)) ||
      Number(verifiedBy) <= 0
    ) {
      return res.status(401).json({
        success: false,
        message: "Authenticated user is invalid",
      });
    }

    // Check that verifier exists and is active
    const familyMembers = await require("../config/db").query(
      `
        SELECT id
        FROM family_members
        WHERE id = $1
          AND is_active = 1
      `,
      [Number(verifiedBy)]
    );

    if (familyMembers.rows.length === 0) {
      return res.status(401).json({
        success: false,
        message: "Authenticated family member not found or inactive",
      });
    }

    // Verification note is required
    if (
      !verificationNote ||
      verificationNote.trim().length === 0
    ) {
      return res.status(400).json({
        success: false,
        message: "verificationNote is required",
      });
    }

    // Fetch existing movement to enforce Segregation of Duties (SoD) and verify state
    const existingMovement =
      await financialMovementModel.getFinancialMovementById(Number(movementId));

    if (!existingMovement) {
      return res.status(404).json({
        success: false,
        message: "Financial movement not found",
      });
    }

    if (existingMovement.verification_status !== "PENDING") {
      return res.status(400).json({
        success: false,
        message: `Financial movement is already ${existingMovement.verification_status.toLowerCase()}`,
      });
    }

    // Segregation of Duties: The recording user cannot verify their own transaction
    if (Number(existingMovement.recorded_by) === Number(verifiedBy)) {
      return res.status(403).json({
        success: false,
        message: "Segregation of duties violation: You cannot verify a financial movement that you recorded yourself. Another active family member must verify it.",
      });
    }

    const movement =
      await financialMovementModel.verifyFinancialMovement({
        movementId: Number(movementId),
        verifiedBy: Number(verifiedBy),
        verificationNote: verificationNote.trim(),
      });

    if (!movement) {
      return res.status(400).json({
        success: false,
        message:
          "Financial movement not found or already processed",
      });
    }

    await activityService.recordActivity({
      actorId: Number(verifiedBy),
      activityType: "FINANCIAL_MOVEMENT_VERIFIED",
      entityType: "FINANCIAL_MOVEMENT",
      entityId: movement.id,
      description: `Financial movement of ${movement.amount} birr was verified`,
      metadata: {
        customerId: movement.customer_id,
        movementType: movement.movement_type,
      },
    });

    return res.status(200).json({
      success: true,
      message: "Financial movement verified successfully",
      data: movement,
    });
  } catch (error) {
    console.error("Verify financial movement error:", error);

    return res.status(500).json({
      success: false,
      message: "Failed to verify financial movement",
    });
  }
}

async function rejectFinancialMovement(req, res) {
  try {
    const { movementId } = req.params;
    const { verificationNote } = req.body;

    const verifiedBy = req.user.id;

    // Validate movement ID
    if (
      !Number.isInteger(Number(movementId)) ||
      Number(movementId) <= 0
    ) {
      return res.status(400).json({
        success: false,
        message: "movementId must be a valid movement ID",
      });
    }

    // Validate authenticated verifier
    if (
      !Number.isInteger(Number(verifiedBy)) ||
      Number(verifiedBy) <= 0
    ) {
      return res.status(401).json({
        success: false,
        message: "Authenticated user is invalid",
      });
    }

    // Check that verifier exists and is active
    const familyMembers = await require("../config/db").query(
      `
        SELECT id
        FROM family_members
        WHERE id = $1
          AND is_active = 1
      `,
      [Number(verifiedBy)]
    );

    if (familyMembers.rows.length === 0) {
      return res.status(401).json({
        success: false,
        message: "Authenticated family member not found or inactive",
      });
    }

    // Rejection reason is required
    if (
      !verificationNote ||
      verificationNote.trim().length === 0
    ) {
      return res.status(400).json({
        success: false,
        message: "verificationNote is required",
      });
    }

    const movement =
      await financialMovementModel.rejectFinancialMovement({
        movementId: Number(movementId),
        verifiedBy: Number(verifiedBy),
        verificationNote: verificationNote.trim(),
      });

    if (!movement) {
      return res.status(400).json({
        success: false,
        message:
          "Financial movement not found or already processed",
      });
    }

    await activityService.recordActivity({
      actorId: Number(verifiedBy),
      activityType: "FINANCIAL_MOVEMENT_REJECTED",
      entityType: "FINANCIAL_MOVEMENT",
      entityId: movement.id,
      description: `Financial movement of ${movement.amount} birr was rejected`,
      metadata: {
        customerId: movement.customer_id,
        movementType: movement.movement_type,
      },
    });

    return res.status(200).json({
      success: true,
      message: "Financial movement rejected successfully",
      data: movement,
    });
  } catch (error) {
    console.error("Reject financial movement error:", error);

    return res.status(500).json({
      success: false,
      message: "Failed to reject financial movement",
    });
  }
}

async function getCustomerBalance(req, res) {
  try {
    const { customerId } = req.params;

    if (!customerId || !/^\d+$/.test(customerId)) {
      return res.status(400).json({
        success: false,
        message: "Invalid customer ID",
      });
    }

    const balance =
      await financialMovementModel.getCustomerBalance(
        Number(customerId)
      );

    return res.status(200).json({
      success: true,
      data: balance,
    });
  } catch (error) {
    console.error("Get customer balance error:", error);

    return res.status(500).json({
      success: false,
      message: "Failed to get customer balance",
    });
  }
}

async function createPayment(req, res) {
  try {
    const {
      customerId,
      amount,
      description,
      referenceType,
      referenceId,
      idempotencyKey,
      transactionAt,
    } = req.body;

    const recordedBy = req.user.id;

    // Validate customer ID
    if (
      !Number.isInteger(Number(customerId)) ||
      Number(customerId) <= 0
    ) {
      return res.status(400).json({
        success: false,
        message: "customerId must be a valid customer ID",
      });
    }

    // Validate amount
    if (
      amount === undefined ||
      amount === null ||
      Number.isNaN(Number(amount)) ||
      Number(amount) <= 0
    ) {
      return res.status(400).json({
        success: false,
        message: "amount must be greater than 0",
      });
    }

    // Validate authenticated recorder
    if (
      !Number.isInteger(Number(recordedBy)) ||
      Number(recordedBy) <= 0
    ) {
      return res.status(401).json({
        success: false,
        message: "Authenticated user is invalid",
      });
    }

    const db = require("../config/db");

    // Check customer exists
    const customers = await db.query(
      `
        SELECT id
        FROM customers
        WHERE id = $1
      `,
      [Number(customerId)]
    );

    if (customers.rows.length === 0) {
      return res.status(400).json({
        success: false,
        message: "Customer not found",
      });
    }

    // Check authenticated family member exists and is active
    const familyMembers = await db.query(
      `
        SELECT id
        FROM family_members
        WHERE id = $1
          AND is_active = 1
      `,
      [Number(recordedBy)]
    );

    if (familyMembers.rows.length === 0) {
      return res.status(401).json({
        success: false,
        message: "Authenticated family member not found or inactive",
      });
    }

    const payment =
      await financialMovementModel.createFinancialMovement({
        customerId: Number(customerId),
        amount: Number(amount),
        movementType: "CREDIT",
        description: description || null,
        referenceType: referenceType || null,
        referenceId:
          referenceId === undefined || referenceId === null
            ? null
            : Number(referenceId),
        idempotencyKey: idempotencyKey || null,
        recordedBy: Number(recordedBy),
        transactionAt: transactionAt || null,
      });

    if (payment.duplicate) {
      return res.status(200).json({
        success: true,
        message: "Payment already recorded",
        data: payment,
      });
    }

    await activityService.recordActivity({
      actorId: Number(recordedBy),
      activityType: "PAYMENT_RECORDED",
      entityType: "FINANCIAL_MOVEMENT",
      entityId: payment.id,
      description: `Payment of ${payment.amount} birr was recorded`,
      metadata: {
        customerId: Number(customerId),
        movementType: "CREDIT",
      },
    });

    return res.status(201).json({
      success: true,
      message: "Payment recorded successfully",
      data: payment,
    });
  } catch (error) {
    console.error("Create payment error:", error);

    return res.status(500).json({
      success: false,
      message: "Failed to record payment",
    });
  }
}

async function getCustomerPayments(req, res) {
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

    const payments =
      await financialMovementModel.getCustomerPayments(
        Number(customerId)
      );

    return res.status(200).json({
      success: true,
      message: "Customer payment history retrieved successfully",
      data: payments,
    });
  } catch (error) {
    console.error("Get customer payments error:", error);

    return res.status(500).json({
      success: false,
      message: "Failed to retrieve customer payment history",
    });
  }
}

module.exports = {
  createFinancialMovement,
  getFinancialMovementById,
  getAllFinancialMovements,
  updateFinancialMovement,
  deleteFinancialMovement,
  deleteFinancialMovements,
  verifyFinancialMovement,
  rejectFinancialMovement,
  getCustomerBalance,
  createPayment,
  getCustomerPayments,
};

