const pool = require("../config/db");

async function createOrder({
  customerId,
  orderNumber,
  description,
  takenBy,
}) {
  const [result] = await pool.execute(
    `
      INSERT INTO orders (
        customer_id,
        order_number,
        description,
        taken_by
      )
      VALUES (?, ?, ?, ?)
    `,
    [
      customerId,
      orderNumber,
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
        o.description,
        o.order_status,
        o.taken_by,
        fm.full_name AS taken_by_name,
        o.verification_status,
        o.verified_by,
        o.verified_at,
        o.verification_note,
        o.created_at,
        o.updated_at
      FROM orders o
      JOIN customers c
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
        o.description,
        o.order_status,
        o.taken_by,
        fm.full_name AS taken_by_name,
        o.verification_status,
        o.verified_by,
        o.verified_at,
        o.verification_note,
        o.created_at,
        o.updated_at
      FROM orders o
      JOIN customers c
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
        o.description,
        o.order_status,
        o.taken_by,
        fm.full_name AS taken_by_name,
        o.verification_status,
        o.verified_by,
        o.verified_at,
        o.verification_note,
        o.created_at,
        o.updated_at
      FROM orders o
      JOIN customers c
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
      JOIN customers c
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
      JOIN customers c
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


async function updateOrderStatus({
  orderId,
  orderStatus,
}) {
  // 1. Get current order status and verification status
  const [orderRows] = await pool.execute(
    `
      SELECT
        order_status,
        verification_status
      FROM orders
      WHERE id = ?
    `,
    [orderId]
  );

  // 2. Order does not exist
  if (orderRows.length === 0) {
    return {
      type: "NOT_FOUND",
    };
  }

  const order = orderRows[0];
  const previousStatus = order.order_status;

  // 3. Order must be verified before confirmation
  if (
    orderStatus === "CONFIRMED" &&
    order.verification_status !== "VERIFIED"
  ) {
    return {
      type: "NOT_VERIFIED",
    };
  }

  // 4. Completed orders cannot be changed
  if (order.order_status === "COMPLETED") {
    return {
      type: "ALREADY_COMPLETED",
    };
  }

  // 5. Update the order status
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

  // 6. Get the updated order
  const [rows] = await pool.execute(
    `
      SELECT
        o.id,
        o.customer_id,
        c.full_name AS customer_name,
        o.order_number,
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
      JOIN customers c
        ON c.id = o.customer_id
      JOIN family_members taker
        ON taker.id = o.taken_by
      LEFT JOIN family_members verifier
        ON verifier.id = o.verified_by
      WHERE o.id = ?
    `,
    [orderId]
  );

  // 7. Return successful result
  return {
    type: "SUCCESS",
    order: rows[0],
    previousStatus: previousStatus,
  };
}


module.exports = {
  createOrder,
  getOrderById,
  getAllOrders,
  verifyOrder,
  rejectOrder,
  updateOrderStatus,
};
