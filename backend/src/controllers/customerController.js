const customerModel = require("../models/customerModel");
const financialMovementModel = require(
  "../models/financialMovementModel"
);

async function createCustomer(req, res) {
  try {
    const { fullName, phone, createdBy } = req.body;

    // Validate required fields
    if (!fullName || !fullName.trim()) {
      return res.status(400).json({
        success: false,
        message: "Customer name is required",
      });
    }

    if (!createdBy) {
      return res.status(400).json({
        success: false,
        message: "createdBy is required",
      });
    }
    if (!Number.isInteger(Number(createdBy)) || Number(createdBy) <= 0) {
  return res.status(400).json({
    success: false,
    message: "createdBy must be a valid family member ID",
  });
}
const familyMemberExists = await customerModel.familyMemberExists(
  Number(createdBy)
);

if (!familyMemberExists) {
  return res.status(400).json({
    success: false,
    message: "Family member not found or inactive",
  });
}

    const customer = await customerModel.createCustomer({
      fullName: fullName.trim(),
      phone,
      createdBy,
    });

    return res.status(201).json({
      success: true,
      message: "Customer created successfully",
      data: customer,
    });
  } catch (error) {
    console.error("Create customer error:", error);

    return res.status(500).json({
      success: false,
      message: "Failed to create customer",
    });
  }
}
 async function getCustomerById(req, res) {
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

    const customer = await customerModel.getCustomerById(
      Number(customerId)
    );

    if (!customer) {
      return res.status(404).json({
        success: false,
        message: "Customer not found",
      });
    }

    const balance =
      await financialMovementModel.getCustomerBalance(
        Number(customerId)
      );

    const customerWithBalance = {
      ...customer,
      totalDebit: balance.totalDebit,
      totalCredit: balance.totalCredit,
      balance: balance.balance,
    };

    return res.status(200).json({
      success: true,
      message: "Customer retrieved successfully",
      data: customerWithBalance,
    });
  } catch (error) {
    console.error("Get customer error:", error);

    return res.status(500).json({
      success: false,
      message: "Failed to retrieve customer",
    });
  }
}
 async function getAllCustomers(req, res) {
  try {
    const customers = await customerModel.getAllCustomers();

    return res.status(200).json({
      success: true,
      message: "Customers retrieved successfully",
      data: customers,
    });
  } catch (error) {
    console.error("Get all customers error:", error);

    return res.status(500).json({
      success: false,
      message: "Failed to retrieve customers",
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
  createCustomer,
  getCustomerById,
  getAllCustomers,
  getCustomerBalance,
 };