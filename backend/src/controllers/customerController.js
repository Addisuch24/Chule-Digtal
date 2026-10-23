 const activityService = require("../services/activityService");
const customerModel = require("../models/customerModel");
const financialMovementModel = require(
  "../models/financialMovementModel"
);

async function createCustomer(req, res) {
  try {
    const { full_name, phone } = req.body;

    if (!req.user || !req.user.id) {
      return res.status(401).json({
        success: false,
        message: "Authentication required",
      });
    }

    const createdBy = Number(req.user.id);

    if (!full_name || !full_name.trim()) {
      return res.status(400).json({
        success: false,
        message: "Customer name is required",
      });
    }

    if (!Number.isInteger(createdBy) || createdBy <= 0) {
      return res.status(400).json({
        success: false,
        message: "Invalid family member ID",
      });
    }

    const familyMemberExists =
      await customerModel.familyMemberExists(createdBy);

    if (!familyMemberExists) {
      return res.status(400).json({
        success: false,
        message: "Family member not found or inactive",
      });
    }

    const customer = await customerModel.createCustomer({
      fullName: full_name.trim(),
      phone: phone || null,
      createdBy,
    });

    await activityService.recordActivity({
      actorId: createdBy,
      activityType: "CUSTOMER_CREATED",
      entityType: "CUSTOMER",
      entityId: customer.id,
      description: `Customer ${customer.full_name} was created`,
      metadata: {
        customerId: customer.id,
      },
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

async function deleteCustomer(req, res) {
  try {
    const { customerId } = req.params;

    if (!customerId || isNaN(customerId)) {
      return res.status(400).json({
        success: false,
        message: "Valid customer ID is required",
      });
    }

    const customer = await customerModel.getCustomerById(Number(customerId));
    if (!customer) {
      return res.status(404).json({
        success: false,
        message: "Customer not found",
      });
    }

    const result = await customerModel.deleteCustomer(Number(customerId));

    if (result.blocked) {
      return res.status(400).json({
        success: false,
        message: result.reason,
      });
    }

    if (!result.success) {
      return res.status(400).json({
        success: false,
        message: "Failed to delete customer",
      });
    }

    await activityService.recordActivity({
      actorId: req.user.id,
      activityType: "CUSTOMER_DELETED",
      entityType: "CUSTOMER",
      entityId: Number(customerId),
      description: `Customer ${customer.full_name} was deleted`,
      metadata: {
        customerId: Number(customerId),
        customerName: customer.full_name,
      },
    });

    const socketService = require("../services/socketService");
    socketService.emitToAll("customer:deleted", { customerId: Number(customerId) });
    socketService.emitToAll("dashboard:refresh", {
      activityType: "CUSTOMER_DELETED",
      entityType: "CUSTOMER",
      entityId: Number(customerId),
    });

    return res.status(200).json({
      success: true,
      message: "Customer deleted successfully",
    });
  } catch (error) {
    console.error("Delete customer error:", error);
    return res.status(500).json({
      success: false,
      message: "Failed to delete customer",
    });
  }
}

async function deleteCustomers(req, res) {
  try {
    const { customerIds } = req.body;

    if (!Array.isArray(customerIds) || customerIds.length === 0) {
      return res.status(400).json({
        success: false,
        message: "customerIds must be a non-empty array",
      });
    }

    const validIds = customerIds
      .map((id) => Number(id))
      .filter((id) => Number.isInteger(id) && id > 0);

    if (validIds.length === 0) {
      return res.status(400).json({
        success: false,
        message: "No valid customer IDs provided",
      });
    }

    const result = await customerModel.deleteCustomers(validIds);

    if (result.deletedCount > 0) {
      await activityService.recordActivity({
        actorId: req.user.id,
        activityType: "CUSTOMER_BULK_DELETED",
        entityType: "CUSTOMER",
        entityId: result.deletedIds[0],
        description: `Bulk deleted ${result.deletedCount} customer(s)`,
        metadata: {
          deletedCount: result.deletedCount,
          deletedIds: result.deletedIds,
          skipped: result.skipped,
        },
      });

      const socketService = require("../services/socketService");
      socketService.emitToAll("customer:deleted", { customerIds: result.deletedIds });
      socketService.emitToAll("dashboard:refresh", {
        activityType: "CUSTOMER_BULK_DELETED",
        entityType: "CUSTOMER",
        deletedIds: result.deletedIds,
      });
    }

    if (result.deletedCount === 0) {
      const reason =
        result.skipped && result.skipped.length > 0
          ? result.skipped.length === 1
            ? result.skipped[0].reason
            : `${result.skipped.length} customer(s) could not be deleted because they have associated financial transactions in the ledger.`
          : "No customers were deleted.";

      return res.status(400).json({
        success: false,
        message: reason,
        data: result,
      });
    }

    const message =
      result.skipped && result.skipped.length > 0
        ? `Successfully deleted ${result.deletedCount} customer(s). ${result.skipped.length} customer(s) were preserved due to associated financial transactions.`
        : `Successfully deleted ${result.deletedCount} customer(s)`;

    return res.status(200).json({
      success: true,
      message,
      data: result,
    });
  } catch (error) {
    console.error("Bulk delete customers error:", error);
    return res.status(500).json({
      success: false,
      message: "Failed to delete customers",
    });
  }
}

async function updateCustomer(req, res) {
  try {
    const { customerId } = req.params;
    const { full_name, phone } = req.body;

    if (!Number.isInteger(Number(customerId)) || Number(customerId) <= 0) {
      return res.status(400).json({
        success: false,
        message: "Invalid customer ID",
      });
    }

    const existing = await customerModel.getCustomerById(Number(customerId));
    if (!existing) {
      return res.status(404).json({
        success: false,
        message: "Customer not found",
      });
    }

    const updated = await customerModel.updateCustomer({
      customerId: Number(customerId),
      fullName: full_name,
      phone,
    });

    await activityService.recordActivity({
      actorId: Number(req.user.id),
      activityType: "CUSTOMER_UPDATED",
      entityType: "CUSTOMER",
      entityId: Number(customerId),
      description: `Customer ${updated.full_name} details were updated`,
      metadata: {
        customerId: Number(customerId),
      },
    });

    return res.status(200).json({
      success: true,
      message: "Customer updated successfully",
      data: updated,
    });
  } catch (error) {
    console.error("Update customer error:", error);
    return res.status(500).json({
      success: false,
      message: "Failed to update customer",
    });
  }
}

module.exports = {
  createCustomer,
  updateCustomer,
  getCustomerById,
  getAllCustomers,
  getCustomerBalance,
  deleteCustomer,
  deleteCustomers,
};