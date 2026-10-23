 const pool = require("../config/db");

async function getDashboardSummary(userId) {
  const [[customerCount]] = await pool.execute(`
    SELECT COUNT(DISTINCT LOWER(TRIM(full_name))) AS total
    FROM customers
  `);

  const [[totalOrders]] = await pool.execute(`
    SELECT COUNT(*) AS total
    FROM orders
  `);

  const [[totalNotes]] = await pool.execute(`
    SELECT COUNT(*) AS total
    FROM business_notes
  `);

  const [[pendingFinancialMovements]] = await pool.execute(`
    SELECT COUNT(*) AS total
    FROM financial_movements
    WHERE verification_status = 'PENDING'
  `);
  const [[financialSummary]] = await pool.execute(`
  SELECT
    COALESCE(
      SUM(
        CASE
          WHEN movement_type = 'DEBIT'
            AND verification_status <> 'REJECTED'
          THEN amount
          ELSE 0
        END
      ),
      0
    ) AS total_debit,

    COALESCE(
      SUM(
        CASE
          WHEN movement_type = 'CREDIT'
            AND verification_status <> 'REJECTED'
          THEN amount
          ELSE 0
        END
      ),
      0
    ) AS total_credit
  FROM financial_movements
`);
const [[customerBalanceSummary]] = await pool.execute(`
  SELECT
    SUM(
      CASE
        WHEN balance < 0 THEN 1
        ELSE 0
      END
    ) AS customers_with_debt,

    SUM(
      CASE
        WHEN balance = 0 THEN 1
        ELSE 0
      END
    ) AS settled_customers,

    SUM(
      CASE
        WHEN balance > 0 THEN 1
        ELSE 0
      END
    ) AS customers_with_credit
  FROM (
    SELECT
      LOWER(TRIM(c.full_name)) AS normalized_name,
      COALESCE(
        SUM(
          CASE
            WHEN fm.movement_type = 'CREDIT'
              AND fm.verification_status <> 'REJECTED'
            THEN fm.amount

            WHEN fm.movement_type = 'DEBIT'
              AND fm.verification_status <> 'REJECTED'
            THEN -fm.amount

            ELSE 0
          END
        ),
        0
      ) AS balance
    FROM customers c
    LEFT JOIN financial_movements fm
      ON fm.customer_id = c.id
    GROUP BY LOWER(TRIM(c.full_name))
  ) AS customer_balances
`);

  const [orderStatusRows] = await pool.execute(`
    SELECT order_status, COUNT(*) AS total
    FROM orders
    GROUP BY order_status
  `);

  const orderStatusBreakdown = {
    PENDING: 0,
    CONFIRMED: 0,
    PROCESSING: 0,
    READY: 0,
    COMPLETED: 0,
    CANCELLED: 0,
  };
  orderStatusRows.forEach((row) => {
    if (row.order_status) {
      orderStatusBreakdown[row.order_status] = Number(row.total);
    }
  });

  const [[recentOrders]] = await pool.execute(`
    SELECT COUNT(*) AS total
    FROM orders
    WHERE created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)
  `);

  const [[recentPayments]] = await pool.execute(`
    SELECT COUNT(*) AS total
    FROM financial_movements
    WHERE movement_type = 'CREDIT'
      AND created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)
  `);

  const [[unreadNotifications]] = await pool.execute(
    `
      SELECT COUNT(*) AS total
      FROM notifications
      WHERE recipient_id = ?
        AND is_read = FALSE
    `,
    [userId]
  );
const [recentActivities] = await pool.execute(`
  SELECT
    a.id,
    a.actor_id,
    fm.full_name AS actor_name,
    a.activity_type,
    a.entity_type,
    a.entity_id,
    a.description,
    a.created_at,
    customer.full_name AS customer_name
  FROM activities a
  JOIN family_members fm
    ON fm.id = a.actor_id
  LEFT JOIN customers customer
    ON a.entity_type = 'CUSTOMER'
    AND a.entity_id = customer.id
  ORDER BY a.created_at DESC, a.id DESC
  LIMIT 10
`);
const [recentOrdersList] = await pool.execute(`
  SELECT
    o.id,
    o.customer_id,
    c.full_name AS customer_name,
    o.title,
    o.order_status,
    o.created_at
  FROM orders o
  LEFT JOIN customers c
    ON c.id = o.customer_id
  ORDER BY o.created_at DESC, o.id DESC
  LIMIT 10
`);
const [recentNotesList] = await pool.execute(`
  SELECT
    bn.id,
    bn.note_text,
    bn.created_by,
    fm.full_name AS created_by_name,
    bn.created_at,
    bn.updated_at
  FROM business_notes bn
  JOIN family_members fm
    ON fm.id = bn.created_by
  ORDER BY bn.updated_at DESC, bn.id DESC
  LIMIT 8
`);
const [[myActivityCount]] = await pool.execute(`
  SELECT COUNT(*) AS total
  FROM activities
  WHERE actor_id = ?
`, [userId]);

  const totalDebit = Number(financialSummary.total_debit || 0);
  const totalCredit = Number(financialSummary.total_credit || 0);
  const netBalance = totalCredit - totalDebit;

  return {
    myActivity: {
      total: Number(myActivityCount.total),
    },
    customers: {
      total: Number(customerCount.total),
      withDebt: Number(customerBalanceSummary.customers_with_debt || 0),
      withBalance: Number(customerBalanceSummary.customers_with_debt || 0) + Number(customerBalanceSummary.customers_with_credit || 0),
      settled: Number(customerBalanceSummary.settled_customers || 0),
      withCredit: Number(customerBalanceSummary.customers_with_credit || 0),
    },
    orders: {
      total: Number(totalOrders.total),
      recent: Number(recentOrders.total),
      statusBreakdown: orderStatusBreakdown,
    },
    notes: {
      total: Number(totalNotes.total),
    },
    financialMovements: {
      pending: Number(pendingFinancialMovements.total),
      recentPayments: Number(recentPayments.total),
      totalDebit,
      totalCredit,
      balance: netBalance,
    },
    notifications: {
      unread: Number(unreadNotifications.total),
    },
    recentOrders: recentOrdersList,
    recentNotes: recentNotesList,
    activities: {
      recent: recentActivities,
      recentOrders: recentOrdersList,
      recentNotes: recentNotesList,
    },
  };
}

module.exports = {
  getDashboardSummary,
};