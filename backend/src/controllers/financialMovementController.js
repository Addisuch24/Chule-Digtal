const financialMovementModel = require("../models/financialMovementModel");

async function createFinancialMovement(req, res) {
  try {
    const {
      customerId,
      amount,
      movementType,
      description,
      referenceType,
      referenceId,
      recordedBy,
      transactionAt,
    } = req.body;

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
const [customers] = await require("../config/db").execute(
  `
    SELECT id
    FROM customers
    WHERE id = ?
  `,
  [Number(customerId)]
);

if (customers.length === 0) {
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

    // Validate recordedBy
    if (
      !Number.isInteger(Number(recordedBy)) ||
      Number(recordedBy) <= 0
    ) {
      return res.status(400).json({
        success: false,
        message: "recordedBy must be a valid family member ID",
      });
    }

    const movement =
      await financialMovementModel.createFinancialMovement({
        customerId: Number(customerId),
        amount: Number(amount),
        movementType,
        description,
        referenceType,
        referenceId:
          referenceId === undefined || referenceId === null
            ? null
            : Number(referenceId),
        recordedBy: Number(recordedBy),
        transactionAt,
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

    // Validate recordedBy
if (
  !Number.isInteger(Number(recordedBy)) ||
  Number(recordedBy) <= 0
) {
  return res.status(400).json({
    success: false,
    message: "recordedBy must be a valid family member ID",
  });
}

// Check that the family member exists
const [familyMembers] = await require("../config/db").execute(
  `
    SELECT id
    FROM family_members
    WHERE id = ?
      AND is_active = TRUE
  `,
  [Number(recordedBy)]
);

if (familyMembers.length === 0) {
  return res.status(400).json({
    success: false,
    message: "recordedBy family member not found or inactive",
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
async function verifyFinancialMovement(req, res) {
  try {
    const { movementId } = req.params;
    const { verifiedBy, verificationNote } = req.body;

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

    // Validate verifier
    if (
      !Number.isInteger(Number(verifiedBy)) ||
      Number(verifiedBy) <= 0
    ) {
      return res.status(400).json({
        success: false,
        message: "verifiedBy must be a valid family member ID",
      });
    }

    // Check that the family member exists and is active
    const [familyMembers] = await require("../config/db").execute(
      `
        SELECT id
        FROM family_members
        WHERE id = ?
          AND is_active = TRUE
      `,
      [Number(verifiedBy)]
    );

    if (familyMembers.length === 0) {
      return res.status(400).json({
        success: false,
        message: "verifiedBy family member not found or inactive",
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
    const { verifiedBy, verificationNote } = req.body;

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

    // Validate verifier
    if (
      !Number.isInteger(Number(verifiedBy)) ||
      Number(verifiedBy) <= 0
    ) {
      return res.status(400).json({
        success: false,
        message: "verifiedBy must be a valid family member ID",
      });
    }

    // Check that the family member exists and is active
    const [familyMembers] = await require("../config/db").execute(
      `
        SELECT id
        FROM family_members
        WHERE id = ?
          AND is_active = TRUE
      `,
      [Number(verifiedBy)]
    );

    if (familyMembers.length === 0) {
      return res.status(400).json({
        success: false,
        message: "verifiedBy family member not found or inactive",
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

    const balance = await financialMovementModel.getCustomerBalance(
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
module.exports = {
  createFinancialMovement,
  getFinancialMovementById,
  getAllFinancialMovements,
  verifyFinancialMovement,
  rejectFinancialMovement,
    getCustomerBalance,
 };