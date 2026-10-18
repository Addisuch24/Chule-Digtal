const pool = require("../config/db");

async function createOrder({
  customerId,
  orderNumber,
  title,
  description,
  takenBy,
}) {
  const [result] = await pool.execute(
    `
      INSERT INTO orders (
        customer_id,
        order_number,
        title,
        description,
        taken_by
      )
      VALUES (?, ?, ?, ?, ?)
    `,
    [
      customerId,
      orderNumber,
      title || null,
      description || null,
      takenBy,
    ]
  );

  const [rows] = await pool.execute(
    `
      SELECT
        o.id,
        o.customer_id,
        c.full_name AS customer_name,
        o.order_number,
        o.title,
        o.description,
        o.taken_by,
        fm.full_name AS taken_by_name,
        o.created_at,
        o.updated_at
      FROM orders o
      LEFT JOIN customers c
        ON c.id = o.customer_id
      JOIN family_members fm
        ON fm.id = o.taken_by
      WHERE o.id = ?
    `,
    [result.insertId]
  );

  return rows[0];
}


async function getOrderById(orderId) {
  const [rows] = await pool.execute(
    `
      SELECT
        o.id,
        o.customer_id,
        c.full_name AS customer_name,
        o.order_number,
        o.title,
        o.description,
        o.taken_by,
        fm.full_name AS taken_by_name,
        o.created_at,
        o.updated_at
      FROM orders o
      LEFT JOIN customers c
        ON c.id = o.customer_id
      JOIN family_members fm
        ON fm.id = o.taken_by
      WHERE o.id = ?
    `,
    [orderId]
  );

  if (rows.length === 0) {
    return null;
  }

  return rows[0];
}


async function getAllOrders() {
  const [rows] = await pool.execute(
    `
      SELECT
        o.id,
        o.customer_id,
        c.full_name AS customer_name,
        o.order_number,
        o.title,
        o.description,
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

  return rows;
}


async function verifyOrder({
  orderId,
  verifiedBy,
  verificationNote,
}) {
  const [result] = await pool.execute(
    `
      UPDATE orders
      SET
        verification_status = 'VERIFIED',
        verified_by = ?,
        verified_at = CURRENT_TIMESTAMP,
        verification_note = ?
      WHERE id = ?
        AND verification_status = 'PENDING'
        AND order_status <> 'DRAFT'
    `,
    [
      verifiedBy,
      verificationNote || null,
      orderId,
    ]
  );

  if (result.affectedRows === 0) {
    return null;
  }

  const [rows] = await pool.execute(
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
      WHERE o.id = ?
    `,
    [orderId]
  );

  return rows[0];
}


async function rejectOrder({
  orderId,
  verifiedBy,
  verificationNote,
}) {
  const [result] = await pool.execute(
    `
      UPDATE orders
      SET
        verification_status = 'REJECTED',
        verified_by = ?,
        verified_at = CURRENT_TIMESTAMP,
        verification_note = ?
      WHERE id = ?
        AND verification_status = 'PENDING'
        AND order_status <> 'DRAFT'
    `,
    [
      verifiedBy,
      verificationNote,
      orderId,
    ]
  );

  if (result.affectedRows === 0) {
    return null;
  }

  const [rows] = await pool.execute(
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
      WHERE o.id = ?
    `,
    [orderId]
  );

  return rows[0];
}


async function updateOrder({
  orderId,
  title,
  description,
}) {
  await pool.execute(
    `
      UPDATE orders
      SET
        title = ?,
        description = ?
      WHERE id = ?
    `,
    [title || null, description || null, orderId]
  );

  return getOrderById(orderId);
}

async function deleteOrder(orderId) {
  const [result] = await pool.execute(
    "DELETE FROM orders WHERE id = ?",
    [orderId]
  );
  return result.affectedRows > 0;
}

async function updateOrderStatus({
  orderId,
  orderStatus,
}) {
  const [orderRows] = await pool.execute(
    `
      SELECT
        order_status,
        verification_status,
        title,
        description
      FROM orders
      WHERE id = ?
    `,
    [orderId]
  );

  if (orderRows.length === 0) {
    return {
      type: "NOT_FOUND",
    };
  }

  const order = orderRows[0];
  const previousStatus = order.order_status;

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

  if (order.order_status === "COMPLETED") {
    return {
      type: "ALREADY_COMPLETED",
    };
  }

  const [result] = await pool.execute(
    `
      UPDATE orders
      SET
        order_status = ?
      WHERE id = ?
    `,
    [
      orderStatus,
      orderId,
    ]
  );

  if (result.affectedRows === 0) {
    return {
      type: "NOT_FOUND",
    };
  }

  const [rows] = await pool.execute(
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
      WHERE o.id = ?
    `,
    [orderId]
  );

  return {
    type: "SUCCESS",
    order: rows[0],
    previousStatus,
  };
}


module.exports = {
  createOrder,
  getOrderById,
  getAllOrders,
  updateOrder,
  deleteOrder,
  verifyOrder,
  rejectOrder,
  updateOrderStatus,
};
