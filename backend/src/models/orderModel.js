const pool = require("../config/db");

async function createOrder({
  customerId,
  orderNumber,
  title,
  description,
  takenBy,
}) {
  const insertResult = await pool.query(
    `
      INSERT INTO orders (
        customer_id,
        order_number,
        title,
        description,
        order_status,
        taken_by
      )
      VALUES ($1, $2, $3, $4, 'PENDING', $5)
      RETURNING id
    `,
    [
      customerId,
      orderNumber,
      title || null,
      description || null,
      takenBy,
    ]
  );

  const newId = insertResult.rows[0].id;

  const result = await pool.query(
    `
      SELECT
        o.id,
        o.customer_id,
        c.full_name AS customer_name,
        o.order_number,
        o.title,
        o.description,
        o.order_status,
        o.taken_by,
        fm.full_name AS taken_by_name,
        o.created_at,
        o.updated_at
      FROM orders o
      LEFT JOIN customers c
        ON c.id = o.customer_id
      JOIN family_members fm
        ON fm.id = o.taken_by
      WHERE o.id = $1
    `,
    [newId]
  );

  return result.rows[0];
}

async function getOrderById(orderId) {
  const result = await pool.query(
    `
      SELECT
        o.id,
        o.customer_id,
        c.full_name AS customer_name,
        o.order_number,
        o.title,
        o.description,
        o.order_status,
        o.taken_by,
        fm.full_name AS taken_by_name,
        o.created_at,
        o.updated_at
      FROM orders o
      LEFT JOIN customers c
        ON c.id = o.customer_id
      JOIN family_members fm
        ON fm.id = o.taken_by
      WHERE o.id = $1
    `,
    [orderId]
  );

  if (result.rows.length === 0) {
    return null;
  }

  return result.rows[0];
}

async function getAllOrders() {
  const result = await pool.query(
    `
      SELECT
        o.id,
        o.customer_id,
        c.full_name AS customer_name,
        o.order_number,
        o.title,
        o.description,
        o.order_status,
        o.taken_by,
        fm.full_name AS taken_by_name,
        o.created_at,
        o.updated_at
      FROM orders o
      LEFT JOIN customers c
        ON c.id = o.customer_id
      JOIN family_members fm
        ON fm.id = o.taken_by
      ORDER BY o.created_at DESC
    `
  );

  return result.rows;
}

async function verifyOrder({
  orderId,
  verifiedBy,
  verificationNote,
}) {
  const updateResult = await pool.query(
    `
      UPDATE orders
      SET
        verification_status = 'VERIFIED',
        verified_by = $1,
        verified_at = CURRENT_TIMESTAMP,
        verification_note = $2,
        updated_at = CURRENT_TIMESTAMP
      WHERE id = $3
        AND verification_status = 'PENDING'
        AND order_status <> 'DRAFT'
    `,
    [
      verifiedBy,
      verificationNote || null,
      orderId,
    ]
  );

  if (updateResult.rowCount === 0) {
    return null;
  }

  const result = await pool.query(
    `
      SELECT
        o.id,
        o.customer_id,
        c.full_name AS customer_name,
        o.order_number,
        o.title,
        o.description,
        o.order_status,
        o.taken_by,
        taker.full_name AS taken_by_name,
        o.verification_status,
        o.verified_by,
        verifier.full_name AS verified_by_name,
        o.verified_at,
        o.verification_note,
        o.created_at,
        o.updated_at
      FROM orders o
      LEFT JOIN customers c
        ON c.id = o.customer_id
      JOIN family_members taker
        ON taker.id = o.taken_by
      LEFT JOIN family_members verifier
        ON verifier.id = o.verified_by
      WHERE o.id = $1
    `,
    [orderId]
  );

  return result.rows[0];
}

async function rejectOrder({
  orderId,
  verifiedBy,
  verificationNote,
}) {
  const updateResult = await pool.query(
    `
      UPDATE orders
      SET
        verification_status = 'REJECTED',
        verified_by = $1,
        verified_at = CURRENT_TIMESTAMP,
        verification_note = $2,
        updated_at = CURRENT_TIMESTAMP
      WHERE id = $3
        AND verification_status = 'PENDING'
        AND order_status <> 'DRAFT'
    `,
    [
      verifiedBy,
      verificationNote,
      orderId,
    ]
  );

  if (updateResult.rowCount === 0) {
    return null;
  }

  const result = await pool.query(
    `
      SELECT
        o.id,
        o.customer_id,
        c.full_name AS customer_name,
        o.order_number,
        o.title,
        o.description,
        o.order_status,
        o.taken_by,
        taker.full_name AS taken_by_name,
        o.verification_status,
        o.verified_by,
        verifier.full_name AS verified_by_name,
        o.verified_at,
        o.verification_note,
        o.created_at,
        o.updated_at
      FROM orders o
      LEFT JOIN customers c
        ON c.id = o.customer_id
      JOIN family_members taker
        ON taker.id = o.taken_by
      LEFT JOIN family_members verifier
        ON verifier.id = o.verified_by
      WHERE o.id = $1
    `,
    [orderId]
  );

  return result.rows[0];
}

async function updateOrder({
  orderId,
  title,
  description,
}) {
  await pool.query(
    `
      UPDATE orders
      SET
        title = $1,
        description = $2,
        updated_at = CURRENT_TIMESTAMP
      WHERE id = $3
    `,
    [title || null, description || null, orderId]
  );

  return getOrderById(orderId);
}

async function checkOrderFinancialReferences(orderId) {
  const result = await pool.query(
    "SELECT COUNT(*) AS count FROM financial_movements WHERE reference_type = 'ORDER' AND reference_id = $1",
    [orderId]
  );
  return Number(result.rows[0]?.count || 0) > 0;
}

async function deleteOrder(orderId) {
  const result = await pool.query(
    "DELETE FROM orders WHERE id = $1",
    [orderId]
  );
  return result.rowCount > 0;
}

async function deleteOrders(orderIds) {
  if (!Array.isArray(orderIds) || orderIds.length === 0) {
    return { deletedCount: 0, deletedIds: [], skipped: [] };
  }

  const validIds = orderIds.map(Number).filter((id) => Number.isInteger(id) && id > 0);
  if (validIds.length === 0) {
    return { deletedCount: 0, deletedIds: [], skipped: [] };
  }

  // Check if any order is referenced by financial_movements
  const refResult = await pool.query(
    "SELECT DISTINCT reference_id FROM financial_movements WHERE reference_type = 'ORDER' AND reference_id = ANY($1::bigint[])",
    [validIds]
  );
  const referencedSet = new Set(refResult.rows.map((r) => Number(r.reference_id)));

  const eligibleIds = [];
  const skipped = [];

  for (const id of validIds) {
    if (referencedSet.has(Number(id))) {
      skipped.push({
        id,
        reason: "Order has linked financial records and cannot be deleted for accounting audit integrity",
      });
    } else {
      eligibleIds.push(id);
    }
  }

  if (eligibleIds.length === 0) {
    return { deletedCount: 0, deletedIds: [], skipped };
  }

  const deleteResult = await pool.query(
    "DELETE FROM orders WHERE id = ANY($1::bigint[])",
    [eligibleIds]
  );

  return {
    deletedCount: deleteResult.rowCount,
    deletedIds: eligibleIds,
    skipped,
  };
}

async function updateOrderStatus({
  orderId,
  orderStatus,
}) {
  const orderRes = await pool.query(
    `
      SELECT
        order_status,
        verification_status,
        title,
        description
      FROM orders
      WHERE id = $1
    `,
    [orderId]
  );

  if (orderRes.rows.length === 0) {
    return {
      type: "NOT_FOUND",
    };
  }

  const order = orderRes.rows[0];
  const previousStatus = order.order_status;

  if (previousStatus === "COMPLETED" && orderStatus !== "COMPLETED") {
    return {
      type: "ALREADY_COMPLETED",
    };
  }

  if (
    orderStatus === "PENDING" &&
    previousStatus === "DRAFT" &&
    (!order.title?.trim() || !order.description?.trim())
  ) {
    return {
      type: "DRAFT_INCOMPLETE",
    };
  }

  if (orderStatus === "DRAFT" && previousStatus !== "DRAFT") {
    return {
      type: "INVALID_TRANSITION",
    };
  }

  if (
    orderStatus === "CONFIRMED" &&
    order.verification_status !== "VERIFIED"
  ) {
    return {
      type: "NOT_VERIFIED",
    };
  }

  if (order.order_status === orderStatus) {
    return {
      type: "SUCCESS",
      order,
      previousStatus,
    };
  }

  const updateResult = await pool.query(
    `
      UPDATE orders
      SET
        order_status = $1,
        updated_at = CURRENT_TIMESTAMP
      WHERE id = $2
    `,
    [
      orderStatus,
      orderId,
    ]
  );

  if (updateResult.rowCount === 0) {
    return {
      type: "NOT_FOUND",
    };
  }

  const result = await pool.query(
    `
      SELECT
        o.id,
        o.customer_id,
        c.full_name AS customer_name,
        o.order_number,
        o.title,
        o.description,
        o.order_status,
        o.taken_by,
        taker.full_name AS taken_by_name,
        o.verification_status,
        o.verified_by,
        verifier.full_name AS verified_by_name,
        o.verified_at,
        o.verification_note,
        o.created_at,
        o.updated_at
      FROM orders o
      LEFT JOIN customers c
        ON c.id = o.customer_id
      JOIN family_members taker
        ON taker.id = o.taken_by
      LEFT JOIN family_members verifier
        ON verifier.id = o.verified_by
      WHERE o.id = $1
    `,
    [orderId]
  );

  return {
    type: "SUCCESS",
    order: result.rows[0],
    previousStatus,
  };
}

module.exports = {
  createOrder,
  getOrderById,
  getAllOrders,
  updateOrder,
  deleteOrder,
  deleteOrders,
  checkOrderFinancialReferences,
  verifyOrder,
  rejectOrder,
  updateOrderStatus,
};
