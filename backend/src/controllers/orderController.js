const orderModel = require("../models/orderModel");
const customerModel = require("../models/customerModel");

async function createOrder(req, res) {
  try {
    const { customerId } = req.params;
    const {
      orderNumber,
      description,
      takenBy,
    } = req.body;

    if (
      !Number.isInteger(Number(customerId)) ||
      Number(customerId) <= 0
    ) {
      return res.status(400).json({
        success: false,
        message: "customerId must be a valid customer ID",
      });
    }

    if (!orderNumber || !orderNumber.trim()) {
      return res.status(400).json({
        success: false,
        message: "orderNumber is required",
      });
    }

    if (!takenBy) {
      return res.status(400).json({
        success: false,
        message: "takenBy is required",
      });
    }

    if (
      !Number.isInteger(Number(takenBy)) ||
      Number(takenBy) <= 0
    ) {
      return res.status(400).json({
        success: false,
        message: "takenBy must be a valid family member ID",
      });
    }
    const familyMemberExists = await customerModel.familyMemberExists(
  Number(takenBy)
);

if (!familyMemberExists) {
  return res.status(400).json({
    success: false,
    message: "Family member not found or inactive",
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

    const order = await orderModel.createOrder({
      customerId: Number(customerId),
      orderNumber: orderNumber.trim(),
      description,
      takenBy: Number(takenBy),
    });

    return res.status(201).json({
      success: true,
      message: "Order created successfully",
      data: order,
    });
  } catch (error) {
    console.error("Create order error:", error);

    return res.status(500).json({
      success: false,
      message: "Failed to create order",
    });
  }
}
async function getOrderById(req, res) {
  try {
    const { orderId } = req.params;

    if (
      !Number.isInteger(Number(orderId)) ||
      Number(orderId) <= 0
    ) {
      return res.status(400).json({
        success: false,
        message: "orderId must be a valid order ID",
      });
    }

    const order = await orderModel.getOrderById(Number(orderId));

    if (!order) {
      return res.status(404).json({
        success: false,
        message: "Order not found",
      });
    }

    return res.status(200).json({
      success: true,
      message: "Order retrieved successfully",
      data: order,
    });
  } catch (error) {
    console.error("Get order error:", error);

    return res.status(500).json({
      success: false,
      message: "Failed to retrieve order",
    });
  }
}
async function getAllOrders(req, res) {
  try {
    const orders = await orderModel.getAllOrders();

    return res.status(200).json({
      success: true,
      message: "Orders retrieved successfully",
      data: orders,
    });
  } catch (error) {
    console.error("Get all orders error:", error);

    return res.status(500).json({
      success: false,
      message: "Failed to retrieve orders",
    });
  }
}
async function verifyOrder(req, res) {
  try {
    const { orderId } = req.params;
    const { verifiedBy, verificationNote } = req.body;

    if (
      !Number.isInteger(Number(orderId)) ||
      Number(orderId) <= 0
    ) {
      return res.status(400).json({
        success: false,
        message: "orderId must be a valid order ID",
      });
    }

    if (!verifiedBy) {
      return res.status(400).json({
        success: false,
        message: "verifiedBy is required",
      });
    }

    if (
      !Number.isInteger(Number(verifiedBy)) ||
      Number(verifiedBy) <= 0
    ) {
      return res.status(400).json({
        success: false,
        message: "verifiedBy must be a valid family member ID",
      });
    }

    const familyMemberExists =
      await customerModel.familyMemberExists(
        Number(verifiedBy)
      );

    if (!familyMemberExists) {
      return res.status(400).json({
        success: false,
        message: "Family member not found or inactive",
      });
    }

    const order = await orderModel.verifyOrder({
      orderId: Number(orderId),
      verifiedBy: Number(verifiedBy),
      verificationNote,
    });

    if (!order) {
      return res.status(400).json({
        success: false,
        message: "Order not found or already verified",
      });
    }

    return res.status(200).json({
      success: true,
      message: "Order verified successfully",
      data: order,
    });
  } catch (error) {
    console.error("Verify order error:", error);

    return res.status(500).json({
      success: false,
      message: "Failed to verify order",
    });
  }
}
async function rejectOrder(req, res) {
  try {
    const { orderId } = req.params;
    const { verifiedBy, verificationNote } = req.body;

    if (
      !Number.isInteger(Number(orderId)) ||
      Number(orderId) <= 0
    ) {
      return res.status(400).json({
        success: false,
        message: "orderId must be a valid order ID",
      });
    }

    if (!verifiedBy) {
      return res.status(400).json({
        success: false,
        message: "verifiedBy is required",
      });
    }

    if (
      !Number.isInteger(Number(verifiedBy)) ||
      Number(verifiedBy) <= 0
    ) {
      return res.status(400).json({
        success: false,
        message: "verifiedBy must be a valid family member ID",
      });
    }

    const familyMemberExists =
      await customerModel.familyMemberExists(
        Number(verifiedBy)
      );

    if (!familyMemberExists) {
      return res.status(400).json({
        success: false,
        message: "Family member not found or inactive",
      });
    }

    if (
      !verificationNote ||
      !verificationNote.trim()
    ) {
      return res.status(400).json({
        success: false,
        message: "verificationNote is required when rejecting an order",
      });
    }

    const order = await orderModel.rejectOrder({
      orderId: Number(orderId),
      verifiedBy: Number(verifiedBy),
      verificationNote: verificationNote.trim(),
    });

    if (!order) {
      return res.status(400).json({
        success: false,
        message: "Order not found or already processed",
      });
    }

    return res.status(200).json({
      success: true,
      message: "Order rejected successfully",
      data: order,
    });
  } catch (error) {
    console.error("Reject order error:", error);

    return res.status(500).json({
      success: false,
      message: "Failed to reject order",
    });
  }
}
  async function updateOrderStatus(req, res) {
  try {
    const { orderId } = req.params;
    const { orderStatus } = req.body;

    if (
      !Number.isInteger(Number(orderId)) ||
      Number(orderId) <= 0
    ) {
      return res.status(400).json({
        success: false,
        message: "orderId must be a valid order ID",
      });
    }

    const allowedStatuses = [
      "PENDING",
      "CONFIRMED",
      "PROCESSING",
      "READY",
      "COMPLETED",
      "CANCELLED",
    ];

    if (!orderStatus) {
      return res.status(400).json({
        success: false,
        message: "orderStatus is required",
      });
    }

    if (!allowedStatuses.includes(orderStatus)) {
      return res.status(400).json({
        success: false,
        message: "Invalid order status",
      });
    }

    const result = await orderModel.updateOrderStatus({
      orderId: Number(orderId),
      orderStatus,
    });

    if (result.type === "NOT_FOUND") {
      return res.status(404).json({
        success: false,
        message: "Order not found",
      });
    }

    if (result.type === "NOT_VERIFIED") {
      return res.status(400).json({
        success: false,
        message: "Order must be verified before it can be confirmed",
      });
    }
    if (result.type === "ALREADY_COMPLETED") {
  return res.status(400).json({
    success: false,
    message: "Completed orders cannot be changed",
  });
}

    return res.status(200).json({
      success: true,
      message: "Order status updated successfully",
      data: result.order,
    });
  } catch (error) {
    console.error("Update order status error:", error);

    return res.status(500).json({
      success: false,
      message: "Failed to update order status",
    });
  }
}
module.exports = {
  createOrder,
  getOrderById,
  getAllOrders,
  verifyOrder,
  rejectOrder,
  updateOrderStatus,
};
