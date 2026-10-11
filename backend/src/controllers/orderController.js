const activityService = require("../services/activityService");
const orderModel = require("../models/orderModel");
const customerModel = require("../models/customerModel");

async function createOrder(req, res) {
  try {
    const customerIdValue = req.params.customerId ?? req.body.customerId;
    const { orderNumber, title, description } = req.body;
    const takenBy = req.user.id;
    let customerId = null;

    if (customerIdValue !== undefined && customerIdValue !== null) {
      if (
        !Number.isInteger(Number(customerIdValue)) ||
        Number(customerIdValue) <= 0
      ) {
        return res.status(400).json({
          success: false,
          message: "customerId must be a valid customer ID",
        });
      }
      customerId = Number(customerIdValue);
    }

    if (typeof orderNumber !== "string" || !orderNumber.trim()) {
      return res.status(400).json({
        success: false,
        message: "orderNumber is required",
      });
    }

    if (typeof title !== "string" || !title.trim() || title.length > 150) {
      return res.status(400).json({
        success: false,
        message: "A title of no more than 150 characters is required",
      });
    }

    if (typeof description !== "string" || !description.trim()) {
      return res.status(400).json({
        success: false,
        message: "Order details are required",
      });
    }

    if (!takenBy) {
      return res.status(400).json({
        success: false,
        message: "takenBy is required",
      });
    }

    if (!Number.isInteger(Number(takenBy)) || Number(takenBy) <= 0) {
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

    if (customerId) {
      const customer = await customerModel.getCustomerById(customerId);

      if (!customer) {
        return res.status(404).json({
          success: false,
          message: "Customer not found",
        });
      }
    }

    const order = await orderModel.createOrder({
      customerId,
      orderNumber: orderNumber.trim(),
      title: title.trim(),
      description: description.trim(),
      takenBy: Number(takenBy),
    });

    await activityService.recordActivity({
      actorId: Number(takenBy),
      activityType: "ORDER_CREATED",
      entityType: "ORDER",
      entityId: order.id,
      description: `Order ${order.title || order.order_number} was created`,
      metadata: {
        customerId,
        orderNumber: order.order_number,
        title: order.title,
      },
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

async function updateOrder(req, res) {
  try {
    const orderId = Number(req.params.orderId);
    const { title, description } = req.body;

    if (!Number.isInteger(orderId) || orderId <= 0) {
      return res.status(400).json({
        success: false,
        message: "orderId must be a valid order ID",
      });
    }

    if (
      title !== undefined &&
      title !== null &&
      (typeof title !== "string" || title.length > 150)
    ) {
      return res.status(400).json({
        success: false,
        message: "title must be a string no longer than 150 characters",
      });
    }

    if (
      description !== undefined &&
      description !== null &&
      typeof description !== "string"
    ) {
      return res.status(400).json({
        success: false,
        message: "description must be a string",
      });
    }

    const order = await orderModel.updateOrder({
      orderId,
      title: title?.trim() || null,
      description: description?.trim() || null,
    });

    if (!order) {
      return res.status(404).json({
        success: false,
        message: "Order not found",
      });
    }

    return res.status(200).json({
      success: true,
      message: "Order updated successfully",
      data: order,
    });
  } catch (error) {
    console.error("Update order error:", error);
    return res.status(500).json({
      success: false,
      message: "Failed to update order",
    });
  }
}

async function deleteOrder(req, res) {
  try {
    const orderId = Number(req.params.orderId);

    if (!Number.isInteger(orderId) || orderId <= 0) {
      return res.status(400).json({
        success: false,
        message: "orderId must be a valid order ID",
      });
    }

    const deleted = await orderModel.deleteOrder(orderId);
    if (!deleted) {
      return res.status(404).json({
        success: false,
        message: "Order not found",
      });
    }

    return res.status(200).json({
      success: true,
      message: "Order deleted successfully",
    });
  } catch (error) {
    console.error("Delete order error:", error);
    return res.status(500).json({
      success: false,
      message: "Failed to delete order",
    });
  }
}

async function getOrderById(req, res) {
  try {
    const { orderId } = req.params;

    if (!Number.isInteger(Number(orderId)) || Number(orderId) <= 0) {
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
    const { verificationNote } = req.body;
    const verifiedBy = req.user.id;

    if (!Number.isInteger(Number(orderId)) || Number(orderId) <= 0) {
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

    if (!Number.isInteger(Number(verifiedBy)) || Number(verifiedBy) <= 0) {
      return res.status(400).json({
        success: false,
        message: "verifiedBy must be a valid family member ID",
      });
    }

    const familyMemberExists = await customerModel.familyMemberExists(
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

    await activityService.recordActivity({
      actorId: Number(verifiedBy),
      activityType: "ORDER_VERIFIED",
      entityType: "ORDER",
      entityId: order.id,
      description: `Order ${order.order_number} was verified`,
      metadata: {
        customerId: order.customer_id,
        orderNumber: order.order_number,
      },
    });

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
    const { verificationNote } = req.body;
    const verifiedBy = req.user.id;

    if (!Number.isInteger(Number(orderId)) || Number(orderId) <= 0) {
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

    if (!Number.isInteger(Number(verifiedBy)) || Number(verifiedBy) <= 0) {
      return res.status(400).json({
        success: false,
        message: "verifiedBy must be a valid family member ID",
      });
    }

    const familyMemberExists = await customerModel.familyMemberExists(
      Number(verifiedBy)
    );

    if (!familyMemberExists) {
      return res.status(400).json({
        success: false,
        message: "Family member not found or inactive",
      });
    }

    if (!verificationNote || !verificationNote.trim()) {
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

    await activityService.recordActivity({
      actorId: Number(verifiedBy),
      activityType: "ORDER_REJECTED",
      entityType: "ORDER",
      entityId: order.id,
      description: `Order ${order.order_number} was rejected`,
      metadata: {
        customerId: order.customer_id,
        orderNumber: order.order_number,
      },
    });

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
    const actorId = req.user.id;

    if (!Number.isInteger(Number(orderId)) || Number(orderId) <= 0) {
      return res.status(400).json({
        success: false,
        message: "orderId must be a valid order ID",
      });
    }

    const allowedStatuses = [
      "DRAFT",
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
    if (result.type === "DRAFT_INCOMPLETE") {
      return res.status(400).json({
        success: false,
        message: "Add a title and details before submitting this draft",
      });
    }
    if (result.type === "INVALID_TRANSITION") {
      return res.status(400).json({
        success: false,
        message: "Only a draft can remain in draft status",
      });
    }
    if (result.type === "ALREADY_COMPLETED") {
      return res.status(400).json({
        success: false,
        message: "Completed orders cannot be changed",
      });
    }

    // Record activity for the status update
    await activityService.recordActivity({
      actorId: Number(actorId),
      activityType: "ORDER_STATUS_UPDATED",
      entityType: "ORDER",
      entityId: result.order.id,
      description: `Order ${result.order.order_number} status updated from ${result.previousStatus} to ${orderStatus}`,
      metadata: {
        orderId: result.order.id,
        orderNumber: result.order.order_number,
        previousStatus: result.previousStatus,
        newStatus: orderStatus,
      },
    });

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
  updateOrder,
  deleteOrder,
  getOrderById,
  getAllOrders,
};
